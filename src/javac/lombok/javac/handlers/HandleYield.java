/*
 * Copyright © 2011 Philipp Eichhorn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.javac.handlers;

import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.core.util.Types.*;
import static lombok.javac.handlers.Javac.*;
import static lombok.javac.handlers.JavacTreeBuilder.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.TypeTags.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import lombok.Yield;
import lombok.javac.JavacASTAdapter;
import lombok.javac.JavacASTVisitor;
import lombok.javac.JavacNode;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

@ProviderFor(JavacASTVisitor.class)
public class HandleYield extends JavacASTAdapter {
	private final static String YIELDER_TEMPLATE = //
		"class %s implements java.util.Iterator<%s> %s { %s private int $state; private boolean $hasNext; private boolean $nextDefined; private %s $next; %s " + //
		"public boolean hasNext() { if (!$nextDefined) { $hasNext = getNext(); $nextDefined = true; } return $hasNext; } " + //
		"public %s next() { if (!hasNext()) { throw new java.util.NoSuchElementException(); } $nextDefined = false; return $next; }" + //
		"public void remove() { throw new java.lang.UnsupportedOperationException(); }" + //
		"private boolean getNext() { while(true) %s } %s} return new %s();";
	private final static String ITERABLE_IMPORT = ", java.lang.Iterable<%s>";
	private final static String ITERATOR_METHOD = "public java.util.Iterator<%s> iterator() { return new %s(); }";

	private final Set<String> methodNames = new HashSet<String>();

	@Override public void visitCompilationUnit(JavacNode top, JCCompilationUnit unit) {
		methodNames.clear();
	}

	@Override public void visitStatement(JavacNode statementNode, JCTree statement) {
		if (statement instanceof JCMethodInvocation) {
			JCMethodInvocation methodCall = (JCMethodInvocation) statement;
			String methodName = methodCall.meth.toString();
			if (isMethodCallValid(statementNode, methodName, Yield.class, "yield")) {
				final JavacMethod method = JavacMethod.methodOf(statementNode);
				if ((method == null) || method.isConstructor()) {
					method.node().addError(canBeUsedInBodyOfMethodsOnly("yield"));
				} else if (handle(method)) {
					methodNames.add(methodName);
				}
			}
		}
	}

	@Override public void endVisitCompilationUnit(JavacNode top, JCCompilationUnit unit) {
		for (String methodName : methodNames) {
			deleteMethodCallImports(top, methodName, Yield.class, "yield");
		}
	}

	public boolean handle(final JavacMethod method) {
		final boolean returnsIterable = method.returns(Iterable.class);
		final boolean returnsIterator = method.returns(Iterator.class);
		if (!(returnsIterable || returnsIterator)) {
			method.node().addError("Method that contain yield() can only return java.util.Iterator or java.lang.Iterable.");
			return true;
		}
		if (method.hasNonFinalParameter()) {
			method.node().addError("Parameters should be final.");
			return true;
		}

		YieldDataCollector collector = new YieldDataCollector();
		collector.collect(method.node(), "$state", "$next");

		if (!collector.hasYields()) {
			return true;
		}

		final String yielderName = yielderName(method.node());
		final String elementType = elementType(method.node());
		final String variables = collector.getVariables();
		final String stateSwitch = collector.getStateSwitch();
		final String classes = collector.getClasses();

		if (returnsIterable) {
			method.body(statements(method.node(), yielderForIterable(yielderName, elementType, variables, stateSwitch, classes)));
		} else if (returnsIterator) {
			method.body(statements(method.node(), yielderForIterator(yielderName, elementType, variables, stateSwitch, classes)));
		}
		method.rebuild();

		return true;
	}

	private String yielderName(JavacNode methodNode) {
		String[] parts = methodNode.getName().split("_");
		String[] newParts = new String[parts.length + 1];
		newParts[0] = "yielder";
		System.arraycopy(parts, 0, newParts, 1, parts.length);
		return camelCase("$", newParts);
	}

	private String elementType(JavacNode methodNode) {
		JCMethodDecl methodDecl = (JCMethodDecl)methodNode.get();
		JCExpression type = methodDecl.restype;
		if (type instanceof JCTypeApply) {
			return ((JCTypeApply)type).arguments.head.type.toString();
		} else {
			return Object.class.getName();
		}
	}

	private String yielderForIterator(String yielderName, String elementType, String variables, String stateSwitch, String classes) {
		return String.format(YIELDER_TEMPLATE, yielderName, elementType, "", variables, elementType, "", elementType, stateSwitch, classes, yielderName);
	}

	private String yielderForIterable(String yielderName, String elementType, String variables, String stateSwitch, String classes) {
		String iterableImport = String.format(ITERABLE_IMPORT, elementType);
		String iteratorMethod = String.format(ITERATOR_METHOD, elementType, yielderName);
		return String.format(YIELDER_TEMPLATE, yielderName, elementType, iterableImport, variables, elementType, iteratorMethod, elementType, stateSwitch, classes, yielderName);
	}

	private static class YieldDataCollector {
		private JavacNode methodNode;
		private JCMethodDecl methodTree;
		private final HashSet<String> names = new HashSet<String>();
		private final ListBuffer<JCTree> classes = ListBuffer.lb();
		private ListBuffer<Scope> yields = ListBuffer.lb();
		private final ListBuffer<Scope> breaks = ListBuffer.lb();
		private final ListBuffer<Scope> variableDecls = ListBuffer.lb();
		private ListBuffer<JCVariableDecl> stateVariables = ListBuffer.lb();
		private Scope root;
		private HashMap<JCTree, Scope> allScopes = new HashMap<JCTree, Scope>();
		private ArrayList<JCCase> cases = new ArrayList<JCCase>();
		private ListBuffer<JCStatement> statements = ListBuffer.lb();
		private HashMap<JCLiteral, JCCase> labelLiterals = new HashMap<JCLiteral, JCCase>();
		private HashSet<JCCase> usedLabels = new HashSet<JCCase>();
		private TreeMaker maker;
		private Name stateName;
		private Name nextName;

		public String getVariables() {
			StringBuilder builder = new StringBuilder();
			for (JCVariableDecl variable : stateVariables) {
				builder.append(variable).append(";");
			}
			return builder.toString();
		}

		public String getClasses() {
			StringBuilder builder = new StringBuilder();
			for (JCTree variable : classes) {
				builder.append(variable).append(" ");
			}
			return builder.toString();
		}

		public String getStateSwitch() {
			ListBuffer<JCCase> switchCases = ListBuffer.lb();
			for (JCCase statement : cases) {
				if (statement == null) {
					continue;
				}
				switchCases.append(statement);
			}
			switchCases.append(maker.Case(null, list((JCStatement)maker.Return(literal(false)))));
			return maker.Switch(ident(stateName), switchCases.toList()).toString().replace("%", "%%");
		}

		public boolean hasYields() {
			return !yields.isEmpty();
		}

		public void collect(final JavacNode methodNode, final String state, final String next) {
			this.methodNode = methodNode;
			maker = methodNode.getTreeMaker();
			methodTree = (JCMethodDecl) methodNode.get();
			stateName = name(state);
			nextName = name(next);
			try {
				if (scan()) {
					refactor();
				}
			} catch (Exception ignore) {
			}
		}

		private boolean scan() {
			try {
				new YieldQuickScanner().scan(methodTree.body);
				return false;
			} catch (IllegalStateException ignore) {
				// this means there are unhandled yields left
			}

			YieldScanner scanner = new YieldScanner();
			scanner.scan(methodTree.body);

			for (Scope scope : yields) {
				Scope yieldScope = scope;
				do {
					allScopes.put(yieldScope.node, yieldScope);
					yieldScope = yieldScope.parent;
				} while (yieldScope != null);
			}

			boolean collected = !breaks.isEmpty();

			while (collected) {
				collected = false;
				for (Scope scope : breaks) {
					Scope target = scope.target;
					if (((target == null) || allScopes.containsKey(target.node)) && !allScopes.containsKey(scope.node)) {
						collected = true;
						Scope breakScope = scope;
						do {
							allScopes.put(breakScope.node, breakScope);
							breakScope = breakScope.parent;
						} while (breakScope != null);
					}
				}
			}

			for (Scope scope : variableDecls) {
				boolean stateVariable = false;
				if (allScopes.containsKey(scope.parent.node)) {
					stateVariable = true;
				} else {
					if ((scope.parent.node instanceof JCCatch) && allScopes.containsKey(scope.parent.parent.node)) {
						stateVariable = true;
					}
				}

				if (stateVariable) {
					JCVariableDecl variable = (JCVariableDecl) scope.node;
					allScopes.put(scope.node, scope);
					stateVariables.append(varDef(PRIVATE, variable.name, copy(variable.vartype)));
				}
			}
			return true;
		}

		private void refactor() {
			root = allScopes.get(methodTree.body);
			JCCase iteratorLabel = getIterationLabel(root);

			usedLabels.add(iteratorLabel);
			usedLabels.add(getBreakLabel(root));

			addStatement(iteratorLabel);
			root.refactor();
			endCase();

			optimizeStates();
			synchronizeLiteralsAndLabels();
		}

		private <T extends JCTree> T copy(T tree) {
			return new TreeCopier<JCTree>(maker).copy(tree);
		}

		private JCExpression getYieldExpression(JCExpression expr) {
			if (expr instanceof JCMethodInvocation) {
				JCMethodInvocation methodCall = (JCMethodInvocation) expr;
				if (methodCall.meth.toString().endsWith("yield") && (methodCall.args.length() == 1)) {
					return methodCall.args.head;
				}
			}
			return null;
		}

		private boolean isTrueLiteral(JCExpression expression) {
			if (expression instanceof JCLiteral) {
				return "true".equals(expression.toString());
			} else if(expression instanceof JCParens) {
				return isTrueLiteral(((JCParens)expression).expr);
			}
			return false;
		}

		private JCCase label() {
			return maker.Case(null, List.<JCStatement> nil());
		}

		private void endCase() {
			if (!cases.isEmpty()) {
				JCCase lastCase = cases.get(cases.size() - 1);
				if (lastCase.stats.isEmpty() && !statements.isEmpty()) {
					lastCase.stats = statements.toList();
					statements.clear();
				}
			}
		}

		private void addStatement(JCCase label) {
			endCase();
			label.pat = literal(cases.size());
			cases.add(label);
		}

		private void addStatement(JCStatement statement) {
			statements.append(statement);
		}

		private JCCase getBreakLabel(Scope scope) {
			JCCase label = scope.breakLabel;
			if (label == null) {
				label = label();
				scope.breakLabel = label;
			}
			return label;
		}

		private JCCase getIterationLabel(Scope scope) {
			JCCase label = scope.iterationLabel;
			if (label == null) {
				label = label();
				scope.iterationLabel = label;
			}
			return label;
		}

		private Name name(String value) {
			return methodNode.toName(value);
		}

		private JCExpression ident(String name) {
			return chainDotsString(maker, methodNode, name);
		}

		private JCExpression ident(Name name) {
			return maker.Ident(name);
		}

		private JCExpression type(String name) {
			return ident(name);
		}

		private JCStatement assign(Name variable, JCExpression expression) {
			return maker.Exec(maker.Assign(ident(variable), expression));
		}

		private static <T> List<T> list(T... items) {
			return List.from(items);
		}

		private JCBlock block(JCStatement... statements) {
			return maker.Block(0, list(statements));
		}

		private JCStatement ifThen(JCExpression condition, JCStatement then) {
			return maker.If(condition, then, null);
		}

		private JCVariableDecl varDef(long flags, Name name, JCExpression type) {
			return maker.VarDef(maker.Modifiers(flags), name, type, null);
		}

		private JCLiteral literal(int value) {
			return maker.Literal(value);
		}

		private JCLiteral literal(boolean value) {
			return maker.Literal(BOOLEAN, value ? 1 : 0);
		}

		private JCLiteral literal(JCCase label) {
			JCLiteral literal = label.pat != null ? copy((JCLiteral) label.pat) : literal(-1);
			labelLiterals.put(literal, label);
			return literal;
		}

		private JCStatement setState(JCExpression expression) {
			return assign(stateName, expression);
		}

		private JCExpression invoke(JCExpression instance, Name name, JCExpression... arguments) {
			return maker.Apply(List.<JCExpression> nil(), instance == null ? ident(name) : maker.Select(instance, name), list(arguments));
		}

		private void refactorStatement(JCStatement statement) {
			if (statement == null) {
				return;
			}
			Scope scope = allScopes.get(statement);
			if (scope != null) {
				scope.refactor();
			} else {
				addStatement(copy(statement));
			}
		}

		private void optimizeStates() {
			int diff = 0;
			JCCase previous = null;
			for (int i = 1; i < cases.size(); i++) {
				JCCase label = cases.get(i);
				JCLiteral literal = (JCLiteral) label.pat;
				literal.value = (Integer)literal.value - diff;
				if (!usedLabels.contains(label)) {
					if (label.stats.isEmpty()) {
						cases.remove(i--);
						diff++;
					} else if ((previous != null) && isNoneOf(previous.stats.last(), JCContinue.class, JCReturn.class)) {
						previous.stats = previous.stats.appendList(label.stats);
						cases.remove(i--);
						diff++;
					} else {
						previous = label;
					}
				} else {
					previous = label;
				}
			}
		}

		private void synchronizeLiteralsAndLabels() {
			for (Map.Entry<JCLiteral, JCCase> entry : labelLiterals.entrySet()) {
				JCCase label = entry.getValue();
				if (label != null) {
					JCLiteral literal = (JCLiteral) label.pat;
					entry.getKey().value = literal.value;
				}
			}
		}

		private class YieldQuickScanner extends TreeScanner {
			@Override
			public void visitExec(JCExpressionStatement tree) {
				final JCExpression expression = getYieldExpression(tree.expr);
				if (expression != null) {
					throw new IllegalStateException();
				} else {
					super.visitExec(tree);
				}
			}
		}

		private class YieldScanner extends TreeScanner {
			private Scope current;

			@Override
			public void visitBlock(final JCBlock tree) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						for (JCStatement statement : tree.stats) {
							refactorStatement(statement);
						}
						addStatement(getBreakLabel(this));
					}
				};

				super.visitBlock(tree);
				current = current.parent;
			}

			@Override
			public void visitLabelled(final JCLabeledStatement tree) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						refactorStatement(tree.body);
					}
				};

				super.visitLabelled(tree);
				current = current.parent;
			}

			@Override
			public void visitForLoop(final JCForLoop tree) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						for (JCStatement statement : tree.init) {
							refactorStatement(statement);
						}
						JCCase label = label();
						JCCase breakLabel = getBreakLabel(this);
						addStatement(label);
						if ((tree.cond != null) && !isTrueLiteral(tree.cond)) {
							addStatement(ifThen(maker.Unary(JCTree.NOT, copy(tree.cond)), block(setState(literal(breakLabel)), maker.Continue(null))));
						}
						refactorStatement(tree.body);
						addStatement(getIterationLabel(this));
						for (JCStatement statement : tree.step) {
							refactorStatement(statement);
						}
						addStatement(setState(literal(label)));
						addStatement(maker.Continue(null));
						addStatement(breakLabel);
					}
				};

				super.visitForLoop(tree);
				current = current.parent;
			}

			@Override
			public void visitForeachLoop(final JCEnhancedForLoop tree) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						Name iteratorVarName = name("$" + tree.var.name + "Iter");
						JCVariableDecl field = varDef(PRIVATE, iteratorVarName, type("java.util.Iterator"));
						stateVariables.append(field);
						addStatement(assign(iteratorVarName, invoke(tree.expr, name("iterator"))));
						addStatement(getIterationLabel(this));
						addStatement(ifThen(maker.Unary(JCTree.NOT, invoke(ident(iteratorVarName), name("hasNext"))), block(setState(literal(getBreakLabel(this))), maker.Continue(null))));
						addStatement(assign(tree.var.name, maker.TypeCast(copy(tree.var.vartype), invoke(ident(iteratorVarName), name("next")))));
						refactorStatement(tree.body);
						addStatement(setState(literal(getIterationLabel(this))));
						addStatement(maker.Continue(null));
						addStatement(getBreakLabel(this));
					}
				};

				super.visitForeachLoop(tree);
				current = current.parent;
			}

			@Override
			public void visitDoLoop(final JCDoWhileLoop tree) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						addStatement(getIterationLabel(this));
						refactorStatement(tree.body);
						addStatement(ifThen(copy(tree.cond), block(setState(literal(getIterationLabel(this))), maker.Continue(null))));
						addStatement(getBreakLabel(this));
					}
				};

				super.visitDoLoop(tree);
				current = current.parent;
			}

			@Override
			public void visitWhileLoop(final JCWhileLoop tree) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						addStatement(getIterationLabel(this));
						if (!isTrueLiteral(tree.cond)) {
							addStatement(ifThen(maker.Unary(JCTree.NOT, copy(tree.cond)), block(setState(literal(getBreakLabel(this))), maker.Continue(null))));
						}
						refactorStatement(tree.body);
						addStatement(setState(literal(getIterationLabel(this))));
						addStatement(maker.Continue(null));
						addStatement(getBreakLabel(this));
					}
				};

				super.visitWhileLoop(tree);
				current = current.parent;
			}

			@Override
			public void visitIf(final JCIf tree) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						JCCase label = tree.elsepart == null ? getBreakLabel(this) : label();
						addStatement(ifThen(maker.Unary(JCTree.NOT, copy(tree.cond)), block(setState(literal(label)), maker.Continue(null))));
						if (tree.elsepart != null) {
							refactorStatement(tree.thenpart);
							addStatement(setState(literal(getBreakLabel(this))));
							addStatement(maker.Continue(null));
							addStatement(label);
							refactorStatement(tree.elsepart);
							addStatement(getBreakLabel(this));
						} else {
							refactorStatement(tree.thenpart);
							addStatement(getBreakLabel(this));
						}
					}
				};

				super.visitIf(tree);
				current = current.parent;
			}

			@Override
			public void visitSwitch(final JCSwitch tree) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						JCCase breakLabel = getBreakLabel(this);
						JCSwitch switchStatement = maker.Switch(copy(tree.selector), List.<JCCase> nil());
						addStatement(switchStatement);
						if (!tree.cases.isEmpty()) {
							boolean hasDefault = false;
							ListBuffer<JCCase> cases = ListBuffer.lb();
							for (JCCase item : tree.cases) {
								if (item.pat == null) {
									hasDefault = true;
								}
								JCCase label = label();
								cases.append(maker.Case(item.pat, list(setState(literal(label)), maker.Continue(null))));
								addStatement(label);
								for (JCStatement statement : item.stats) {
									refactorStatement(statement);
								}
							}
							if (!hasDefault) {
								cases.append(maker.Case(null, list(setState(literal(breakLabel)), maker.Continue(null))));
							}
							switchStatement.cases = cases.toList();
						}
						addStatement(breakLabel);
					}
				};

				super.visitSwitch(tree);
				current = current.parent;
			}

			@Override
			public void visitVarDef(final JCVariableDecl tree) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						if (tree.init != null) {
							addStatement(assign(tree.name, copy(tree.init)));
						}
					}
				};

				variableDecls.append(current);
				super.visitVarDef(tree);
				current = current.parent;
			}

			@Override
			public void visitReturn(final JCReturn tree) {
				methodNode.addError("The 'return' expression is permitted.");
			}

			@Override
			public void visitBreak(final JCBreak tree) {
				Scope target = null;
				Name label = tree.label;
				if (label != null) {
					Scope labelScope = current;
					while (labelScope != null) {
						if (labelScope.node instanceof JCLabeledStatement) {
							JCLabeledStatement labeledStatement = (JCLabeledStatement) labelScope.node;
							if (label == labeledStatement.label) {
								if (target != null) {
									methodNode.addError("Invalid label.");
								}
								target = labelScope;
							}
						}
						labelScope = labelScope.parent;
					}
				} else {
					Scope labelScope = current;
					while (labelScope != null) {
						if (isOneOf(labelScope.node, JCForLoop.class, JCEnhancedForLoop.class, JCWhileLoop.class, JCDoWhileLoop.class, JCSwitch.class)) {
							target = labelScope;
							break;
						}
						labelScope = labelScope.parent;
					}
				}

				if (target == null) {
					methodNode.addError("Invalid break.");
				}

				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						addStatement(setState(literal(getBreakLabel(target))));
						addStatement(maker.Continue(null));
					}
				};

				current.target = target;
				breaks.append(current);
				current = current.parent;
			}

			@Override
			public void visitContinue(final JCContinue tree) {
				Scope target = null;
				Name label = tree.label;
				if (label != null) {
					Scope labelScope = current;
					while (labelScope != null) {
						if (labelScope.node instanceof JCLabeledStatement) {
							JCLabeledStatement labeledStatement = (JCLabeledStatement) labelScope.node;
							if (label == labeledStatement.label) {
								if (target != null) {
									methodNode.addError("Invalid label.");
								}
								if (isOneOf(labelScope.node, JCForLoop.class, JCEnhancedForLoop.class, JCWhileLoop.class, JCDoWhileLoop.class)) {
									target = labelScope;
								} else {
									methodNode.addError("Invalid continue.");
								}
							}
						}
						labelScope = labelScope.parent;
					}
				} else {
					Scope labelScope = current;
					while (labelScope != null) {
						if (isOneOf(labelScope.node, JCForLoop.class, JCEnhancedForLoop.class, JCWhileLoop.class, JCDoWhileLoop.class)) {
							target = labelScope;
							break;
						}
						labelScope = labelScope.parent;
					}
				}

				if (target == null) {
					methodNode.addError("Invalid continue.");
				}

				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						addStatement(setState(literal(getIterationLabel(target))));
						addStatement(maker.Continue(null));
					}
				};

				current.target = target;
				breaks.append(current);
				current = current.parent;
			}

			@Override
			public void visitApply(JCMethodInvocation tree) {
				if (tree.meth instanceof JCIdent) {
					Name name = TreeInfo.fullName(tree.meth);
					if ((name == name("hasNext")) || (name == name("next")) || (name == name("remove"))) {
						methodNode.addError("Cannot call method " + name + "(), as it is hidden.");
					}
				}

				super.visitApply(tree);
			}

			@Override
			public void visitExec(JCExpressionStatement tree) {
				final JCExpression expression = getYieldExpression(tree.expr);
				if (expression != null) {
					current = new Scope(current, tree) {
						@Override
						public void refactor() {
							JCCase label = getBreakLabel(this);
							addStatement(assign(nextName, copy(expression)));
							addStatement(setState(literal(label)));
							addStatement(maker.Return(literal(true)));
							addStatement(label);
						}
					};
					yields.append(current);
					scan(expression);
					current = current.parent;
				} else {
					super.visitExec(tree);
				}
			}

			@Override
			public void visitIdent(JCIdent tree) {
				if (tree.name == tree.name.table._this) {
					methodNode.addError("No unqualified 'this' expression is permitted.");
				}
				if (tree.name == tree.name.table._super) {
					methodNode.addError("No unqualified 'super' expression is permitted.");
				}
				names.add(tree.name.toString());
				super.visitIdent(tree);
			}

			@Override
			public void visitNewClass(JCNewClass tree) {
				scan(tree.encl);
				scan(tree.clazz);
				scan(tree.args);
			}

			@Override
			public void visitClassDef(JCClassDecl tree) {
				classes.add(tree);
			}
		}
	}

	public static abstract class Scope {
		public JCTree node;
		public Scope parent;
		public Scope target;
		public JCCase iterationLabel;
		public JCCase breakLabel;

		public Scope(Scope parent, JCTree node) {
			this.parent = parent;
			this.node = node;
		}

		public abstract void refactor();
	}
}
