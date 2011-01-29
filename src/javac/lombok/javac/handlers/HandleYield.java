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

import static com.sun.tools.javac.code.Flags.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.javac.handlers.Javac.*;
import static lombok.javac.handlers.JavacTreeBuilder.statements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import lombok.Yield;
import lombok.javac.JavacASTAdapter;
import lombok.javac.JavacASTVisitor;
import lombok.javac.JavacNode;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTags;
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
import com.sun.tools.javac.tree.JCTree.JCSynchronized;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
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
		"%s %s} return new %s();";
	private final static String ITERABLE_IMPORT = ", java.lang.Iterable<%s>";
	private final static String ITERATOR_METHOD = "public java.util.Iterator<%s> iterator() { return new %s(); }";
	
	private boolean handled;
	private String methodName;
	
	@Override public void visitCompilationUnit(JavacNode top, JCCompilationUnit unit) {
		handled = false;
	}
	
	@Override public void visitStatement(JavacNode statementNode, JCTree statement) {
		if (statement instanceof JCMethodInvocation) {
			JCMethodInvocation methodCall = (JCMethodInvocation) statement;
			methodName = methodCall.meth.toString();
			if (isMethodCallValid(statementNode, methodName, Yield.class, "yield")) {
				try {
					JavacNode methodNode = methodNodeOf(statementNode);
					if (isConstructor(methodNode)) {
						methodNode.addError(canBeUsedInBodyOfMethodsOnly("yield"));
					} else {
						handled = handle(methodNode);
					}
				} catch (IllegalArgumentException e) {
					statementNode.addError(canBeUsedInBodyOfMethodsOnly("yield"));
				}
			}
		}
	}
	
	@Override public void endVisitCompilationUnit(JavacNode top, JCCompilationUnit unit) {
		if (handled) {
			deleteMethodCallImports(top, methodName, Yield.class, "yield");
		}
	}
	
	public boolean handle(JavacNode methodNode) {
		final boolean returnsIterable = returns(methodNode, Iterable.class);
		final boolean returnsIterator = returns(methodNode, Iterator.class);
		if (!(returnsIterable || returnsIterator)) {
			methodNode.addError("Method that contain yield() can only return java.util.Iterator or java.lang.Iterable.");
			return true;
		}
		if (isSynchronized(methodNode)) {
			methodNode.addError("Method that contain yield() should not be synchronized");
			return true;
		}
		if (hasNonFinalParameter(methodNode)) {
			methodNode.addError("Parameters should be final.");
			return true;
		}
		
		YieldDataCollector collector = new YieldDataCollector();
		collector.collect(methodNode);
		
		final String yielderName = yielderName(methodNode);
		final String elementType = elementType(methodNode);
		final String variables = collector.getVariables();
		final String getNextMethod = collector.getGetNextMethod();
		final String classes = collector.getClasses();
		
		TreeMaker maker = methodNode.getTreeMaker();
		JCMethodDecl method = (JCMethodDecl)methodNode.get();
		if (returnsIterable) {
			method.body = maker.Block(0, statements(methodNode, yielderForIterable(yielderName, elementType, variables, getNextMethod, classes)));
		} else if (returnsIterator) {
			method.body = maker.Block(0, statements(methodNode, yielderForIterator(yielderName, elementType, variables, getNextMethod, classes)));
		}
		
		methodNode.rebuild();
		
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
	
	private boolean returns(JavacNode methodNode, Class<?> clazz) {
		JCMethodDecl methodDecl = (JCMethodDecl)methodNode.get();
		final String type = getTypeStringOf(methodDecl.restype);
		return type.equals(clazz.getName());
	}
	
	private boolean isSynchronized(JavacNode methodNode) {
		JCMethodDecl methodDecl = (JCMethodDecl)methodNode.get();
		return (methodDecl.mods != null) && ((methodDecl.mods.flags & SYNCHRONIZED) != 0);
	}
	
	private boolean hasNonFinalParameter(JavacNode methodNode) {
		JCMethodDecl methodDecl = (JCMethodDecl)methodNode.get();
		for(JCVariableDecl param: methodDecl.params) {
			if ((param.mods == null) || (param.mods.flags & FINAL) == 0) {
				return true;
			}
		}
		return false;
	}
	
	private String getTypeStringOf(JCExpression type) {
		if (type instanceof JCTypeApply) {
			return ((JCTypeApply)type).clazz.type.toString();
		} else {
			return type.type.toString();
		}
	}
	
	private String yielderForIterator(String yielderName, String elementType, String variables, String getNextMethod, String classes) {
		return String.format(YIELDER_TEMPLATE, yielderName, elementType, "", variables, elementType, "", elementType, getNextMethod, classes, yielderName);
	}
	
	private String yielderForIterable(String yielderName, String elementType, String variables, String getNextMethod, String classes) {
		String iterableImport = String.format(ITERABLE_IMPORT, elementType);
		String iteratorMethod = String.format(ITERATOR_METHOD, elementType, yielderName);
		return String.format(YIELDER_TEMPLATE, yielderName, elementType, iterableImport, variables, elementType, iteratorMethod, elementType, getNextMethod, classes, yielderName);
	}
	
	private static class YieldDataCollector {
		private JavacNode methodNode;
		private JCMethodDecl methodTree;
		private boolean iterable;
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
		private ListBuffer<ErrorHandler> errorHandlers = ListBuffer.lb();
		private int finallyBlocks;
		private HashMap<JCLiteral, JCCase> labelLiterals = new HashMap<JCLiteral, JCCase>();
		private HashSet<JCCase> usedLabels = new HashSet<JCCase>();
		private TreeMaker maker;
		private TreeCopier<JCTree> copier;
		private Name stateName;
		private Name nextName;
		private Name errorName;
		
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
		
		public String getGetNextMethod() {
			StringBuilder builder = new StringBuilder();
			builder.append(generateGetNext()).append(" ");
			return builder.toString();
		}

		public void collect(final JavacNode methodNode) {
			this.methodNode = methodNode;
			maker = methodNode.getTreeMaker();
			copier = new TreeCopier<JCTree>(maker);
			methodTree = (JCMethodDecl) methodNode.get();
			try {
				validate();
			} catch (Exception ignore) {
			}
			refactor();
		}

		private void validate() {
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
					if ((scope.parent.node instanceof JCCatch)
							&& allScopes.containsKey(scope.parent.parent.node)) {
						stateVariable = true;
					}
				}

				if (stateVariable) {
					JCVariableDecl variable = (JCVariableDecl) scope.node;

					allScopes.put(scope.node, scope);

					stateVariables.append(varDef(Flags.PRIVATE, variable.name, copy(variable.vartype)));
				}
			}

			root = allScopes.get(methodTree.body);

			for (JCTree tree : allScopes.keySet()) {
				if (tree instanceof JCSynchronized) {
					methodNode.addError("No synchronized statement allowed in this context.");
				}
			}

			if (!stateVariables.isEmpty()) {
				HashSet<Name> names = new HashSet<Name>();

				for (JCVariableDecl variable : stateVariables) {
					if (!names.add(variable.name)) {
						methodNode.addError("No variables with duplicate names should be used in the state machine.");
					}
				}
			}

			stateName = name("$state");
			nextName = name("$next");
			errorName = name("$exception");
		}

		private void refactor() {
			JCCase iteratorLabel = getIterationLabel(root);

			usedLabels.add(iteratorLabel);
			usedLabels.add(getBreakLabel(root));
			
			addStatement(iteratorLabel);
			root.refactor();
			endCase();

			optimizeBreaks();
			synchronizeLiteralsAndLabels();
		}

		private <T extends JCTree> T copy(T tree) {
			return copier.copy(tree);
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

		private boolean isNotReturnOrContinue(JCStatement statement) {
			return !((statement instanceof JCContinue) || (statement instanceof JCReturn));
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

		private JCCase getFinallyLabel(Scope scope) {
			JCCase label = scope.finallyLabel;
			if (label == null) {
				label = label();
				scope.finallyLabel = label;
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
			return ident(name.split("\\."));
		}

		private JCExpression ident(String... names) {
			JCExpression expression = null;
			for (String value : names) {
				Name name = name(value);
				expression = expression == null ? ident(name) : maker.Select(expression, name);
			}
			return expression;
		}

		private JCExpression ident(Name name) {
			return maker.Ident(name);
		}

		private JCExpression type(String name) {
			return ident(name);
		}

		private JCExpression type(int typeId) {
			return maker.TypeIdent(typeId);
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

		private JCStatement ret(JCExpression expression) {
			return maker.Return(expression);
		}

		private JCStatement ifThen(JCExpression condition, JCStatement then) {
			return maker.If(condition, then, null);
		}

		private JCNewClass newClass(JCExpression type, JCExpression... arguments) {
			return maker.NewClass(null, List.<JCExpression> nil(), type, list(arguments), null);
		}

		private JCMethodDecl methodDef(long flags, Name name, JCBlock body, JCExpression restype, JCVariableDecl... params) {
			return maker.MethodDef(maker.Modifiers(flags), name, restype, List.<JCTypeParameter> nil(), list(params), List.<JCExpression> nil(), body, null);
		}

		private JCVariableDecl varDef(long flags, Name name, JCExpression type) {
			return maker.VarDef(maker.Modifiers(flags), name, type, null);
		}

		private JCLiteral literal(int value) {
			return maker.Literal(value);
		}

		private JCLiteral literal(boolean value) {
			return maker.Literal(TypeTags.BOOLEAN, value ? 1 : 0);
		}

		private JCLiteral literal() {
			return maker.Literal(TypeTags.BOT, null);
		}

		private JCLiteral literal(JCCase label) {
			JCLiteral literal = label.pat != null ? copy((JCLiteral) label.pat) : literal(-1);
			labelLiterals.put(literal, label);
			return literal;
		}

		private static Scope getFinallyScope(Scope scope, Scope top) {
			JCTree previous = null;
			while (scope != null) {
				JCTree tree = scope.node;
				if (tree instanceof JCTry) {
					JCTry statement = (JCTry) tree;
					if ((statement.finalizer != null) && (statement.finalizer != previous)) {
						return scope;
					}
				}
				if (scope == top) {
					break;
				}
				previous = tree;
				scope = scope.parent;
			}
			return null;
		}

		private JCStatement setStateId(JCExpression expression) {
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

		private void optimizeBreaks() {
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
					} else if ((previous != null) && isNotReturnOrContinue(previous.stats.last())) {
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

		private JCMethodDecl generateGetNext() {
			JCSwitch switchStatement = generateSwitch();
			JCStatement errorHandler = generateErrorHandler();

			return methodDef(
					Flags.PRIVATE,
					name("getNext"),
					errorHandler != null ? block(
							varDef(0, errorName, type("java.lang.Throwable")),
							maker.WhileLoop(
									literal(true),
									block(maker.Try(
											block(switchStatement),
											List.<JCCatch> of(maker
													.Catch(varDef(
															0,
															name("e"),
															type("java.lang.Throwable")),
															block(assign(errorName,
																	ident("e"))))),
											null), errorHandler)))
							: block(maker.WhileLoop(literal(true), switchStatement)),
					type(TypeTags.BOOLEAN));
		}

		private JCSwitch generateSwitch() {
			ListBuffer<JCCase> switchCases = ListBuffer.lb();
			for (JCCase statement : cases) {
				if (statement == null) {
					continue;
				}
				switchCases.append(statement);
			}
			switchCases.append(maker.Case(null, list(ret(literal(false)))));
			return maker.Switch(ident(stateName), switchCases.toList());
		}

		private JCStatement generateErrorHandler() {
			if (errorHandlers.isEmpty()) {
				return null;
			}

			ListBuffer<JCCase> switchCases = ListBuffer.lb();
			HashSet<JCCase> labels = new HashSet<JCCase>();

			for (ErrorHandler handler : errorHandlers) {
				JCCase lastCase = null;

				for (int i = handler.begin; i < handler.end; ++i) {
					JCCase label = cases.get(i);

					if ((label != null) && labels.add(label)) {
						lastCase = maker.Case(copy(label.pat),
								List.<JCStatement> nil());
						switchCases.append(lastCase);
					}
				}

				lastCase.stats = handler.statements.toList();
			}

			Name exception = name("ce");
			JCExpression type = type("java.util.ConcurrentModificationException");

			switchCases.append(maker.Case(
					null,
					list(setStateId(literal(getBreakLabel(root))), maker.VarDef(
							maker.Modifiers(0), exception, type, newClass(type)),
							maker.Exec(invoke(ident(exception), name("initCause"),
									ident(errorName))), maker
									.Throw(ident(exception)))));

			return maker.Switch(ident(stateName), switchCases.toList());
		}

		private class YieldScanner extends TreeScanner {
			private Scope current;

			@Override
			public void visitBlock(final JCBlock tree) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						if (!tree.stats.isEmpty()) {
							for (JCStatement statement : tree.stats) {
								refactorStatement(statement);
							}
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
						if (!tree.init.isEmpty()) {
							for (JCStatement statement : tree.init) {
								refactorStatement(statement);
							}
						}

						JCCase label = label();
						JCCase breakLabel = getBreakLabel(this);

						addStatement(label);

						if ((tree.cond != null) && !isTrueLiteral(tree.cond)) {
							addStatement(ifThen(
									maker.Unary(JCTree.NOT, copy(tree.cond)),
									block(setStateId(literal(breakLabel)),
											maker.Continue(null))));
						}

						refactorStatement(tree.body);

						addStatement(getIterationLabel(this));

						for (JCStatement statement : tree.step) {
							refactorStatement(statement);
						}

						addStatement(setStateId(literal(label)));
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
						JCVariableDecl field = varDef(Flags.PRIVATE,
								iteratorVarName, type("java.util.Iterator"));
						JCExpression rawTypes = maker.Literal("rawtypes");

						field.mods.annotations = list(maker.Annotation(
								type("java.lang.SuppressWarnings"), list(rawTypes)));

						stateVariables.append(field);

						addStatement(assign(iteratorVarName,
								invoke(tree.expr, name("iterator"))));

						addStatement(getIterationLabel(this));

						addStatement(ifThen(
								maker.Unary(
										JCTree.NOT,
										invoke(ident(iteratorVarName),
												name("hasNext"))),
								block(setStateId(literal(getBreakLabel(this))),
										maker.Continue(null))));

						addStatement(assign(tree.var.name, maker.TypeCast(
								copy(tree.var.vartype),
								invoke(ident(iteratorVarName), name("next")))));

						refactorStatement(tree.body);
						addStatement(setStateId(literal(getIterationLabel(this))));
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

						addStatement(ifThen(
								copy(tree.cond),
								block(setStateId(literal(getIterationLabel(this))),
										maker.Continue(null))));

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
							addStatement(ifThen(
									maker.Unary(JCTree.NOT, copy(tree.cond)),
									block(setStateId(literal(getBreakLabel(this))),
											maker.Continue(null))));
						}

						refactorStatement(tree.body);

						addStatement(setStateId(literal(getIterationLabel(this))));
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
						JCCase label = tree.elsepart == null ? getBreakLabel(this)
								: label();

						if (!isTrueLiteral(tree.cond)) {
							addStatement(ifThen(
									maker.Unary(JCTree.NOT, copy(tree.cond)),
									block(setStateId(literal(label)),
											maker.Continue(null))));
						}

						if (tree.elsepart != null) {
							refactorStatement(tree.elsepart);
							addStatement(setStateId(literal(getBreakLabel(this))));
							addStatement(maker.Continue(null));
							addStatement(label);
						}

						refactorStatement(tree.thenpart);
						addStatement(getBreakLabel(this));
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
						JCSwitch switchStatement = maker.Switch(
								copy(tree.selector), List.<JCCase> nil());

						addStatement(switchStatement);

						if (!tree.cases.isEmpty()) {
							boolean hasDefault = false;
							ListBuffer<JCCase> cases = ListBuffer.lb();

							for (JCCase item : tree.cases) {
								if (item.pat == null) {
									hasDefault = true;
								}

								JCCase label = label();

								cases.append(maker.Case(
										item.pat,
										list(setStateId(literal(label)),
												maker.Continue(null))));

								addStatement(label);

								for (JCStatement statement : item.stats) {
									refactorStatement(statement);
								}
							}

							if (!hasDefault) {
								cases.append(maker.Case(
										null,
										list(setStateId(literal(breakLabel)),
												maker.Continue(null))));
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
			public void visitTry(final JCTry tree) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						boolean hasFinally = tree.finalizer != null;
						boolean hasCatch = !tree.catchers.isEmpty();
						ErrorHandler catchHandler = null;
						ErrorHandler finallyHandler = null;
						JCCase tryLabel = label();
						JCCase finallyLabel;
						JCCase breakLabel = getBreakLabel(this);
						Name finallyErrorName = null;

						if (hasFinally) {
							finallyHandler = new ErrorHandler();
							finallyLabel = getFinallyLabel(this);

							++finallyBlocks;
							finallyErrorName = name("$exception"
									+ finallyBlocks);
							labelName = name("$id" + finallyBlocks);

							stateVariables.append(varDef(Flags.PRIVATE,
									finallyErrorName, type("java.lang.Throwable")));

							stateVariables.append(varDef(Flags.PRIVATE, labelName,
									type(TypeTags.INT)));

							addStatement(assign(finallyErrorName, literal()));
							addStatement(assign(labelName, literal(breakLabel)));
						} else {
							finallyLabel = breakLabel;
						}

						addStatement(setStateId(literal(tryLabel)));

						if (hasCatch) {
							catchHandler = new ErrorHandler();
							catchHandler.begin = cases.size();
						} else {
							if (hasFinally) {
								finallyHandler.begin = cases.size();
							}
						}

						addStatement(tryLabel);
						refactorStatement(tree.body);
						addStatement(setStateId(literal(finallyLabel)));

						if (hasCatch) {
							addStatement(maker.Continue(null));
							catchHandler.end = cases.size();

							for (JCCatch catcher : tree.catchers) {
								JCCase label = label();

								usedLabels.add(label);
								addStatement(label);
								refactorStatement(catcher.body);
								addStatement(setStateId(literal(finallyLabel)));
								addStatement(maker.Continue(null));

								catchHandler.statements
										.append(ifThen(
												maker.TypeTest(ident(errorName),
														copy(catcher.param.vartype)),
												block(assign(
														catcher.param.name,
														maker.TypeCast(
																copy(catcher.param.vartype),
																ident(errorName))),
														setStateId(literal(label)),
														maker.Continue(null))));
							}

							errorHandlers.append(catchHandler);

							if (hasFinally) {
								finallyHandler.begin = catchHandler.end;
							}
						}

						if (hasFinally) {
							finallyHandler.end = cases.size();

							addStatement(finallyLabel);
							refactorStatement(tree.finalizer);

							addStatement(ifThen(
									maker.Binary(JCTree.NE,
											ident(finallyErrorName), literal()),
									block(assign(errorName, ident(finallyErrorName)),
											maker.Break(null))));

							Scope next = getFinallyScope(parent, null);

							if (next != null) {
								JCCase label = getFinallyLabel(next);

								addStatement(maker.If(
										maker.Binary(JCTree.GT, ident(labelName),
												literal(label)),
										block(assign(next.labelName,
												ident(labelName)),
												setStateId(literal(label))),
										setStateId(ident(labelName))));
							} else {
								addStatement(setStateId(ident(labelName)));
							}

							addStatement(maker.Continue(null));

							finallyHandler.statements.append(assign(
									finallyErrorName, ident(errorName)));
							finallyHandler.statements
									.append(setStateId(literal(finallyLabel)));
							finallyHandler.statements.append(maker.Continue(null));

							usedLabels.add(finallyLabel);
							errorHandlers.append(finallyHandler);
						}

						addStatement(breakLabel);
					}
				};

				super.visitTry(tree);
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
				Name label = tree.label;
				Scope target = null;

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
						if ((labelScope.node instanceof JCForLoop)
								|| (labelScope.node instanceof JCEnhancedForLoop)
								|| (labelScope.node instanceof JCWhileLoop)
								|| (labelScope.node instanceof JCDoWhileLoop)
								|| (labelScope.node instanceof JCSwitch)) {
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
						Scope next = getFinallyScope(parent, target);
						JCCase label = getBreakLabel(target);

						if (next == null) {
							addStatement(setStateId(literal(label)));
							addStatement(maker.Continue(null));
						} else {
							addStatement(assign(next.labelName, literal(label)));
							addStatement(setStateId(literal(getFinallyLabel(next))));
							addStatement(maker.Continue(null));
						}
					}
				};

				current.target = target;
				breaks.append(current);
				current = current.parent;
			}

			@Override
			public void visitContinue(final JCContinue tree) {
				Name label = tree.label;
				Scope target = null;

				if (label != null) {
					Scope labelScope = current;

					while (labelScope != null) {
						if (labelScope.node instanceof JCLabeledStatement) {
							JCLabeledStatement labeledStatement = (JCLabeledStatement) labelScope.node;

							if (label == labeledStatement.label) {
								if (target != null) {
									methodNode.addError("Invalid label.");
								}

								if ((labelScope.node instanceof JCForLoop)
										|| (labelScope.node instanceof JCEnhancedForLoop)
										|| (labelScope.node instanceof JCWhileLoop)
										|| (labelScope.node instanceof JCDoWhileLoop)) {
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
						if ((labelScope.node instanceof JCForLoop)
								|| (labelScope.node instanceof JCEnhancedForLoop)
								|| (labelScope.node instanceof JCWhileLoop)
								|| (labelScope.node instanceof JCDoWhileLoop)) {
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
						Scope next = getFinallyScope(parent, target);
						JCCase label = getIterationLabel(target);

						if (next == null) {
							addStatement(setStateId(literal(label)));
							addStatement(maker.Continue(null));
						} else {
							addStatement(assign(next.labelName, literal(label)));
							addStatement(setStateId(literal(getFinallyLabel(next))));
							addStatement(maker.Continue(null));
						}
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

					if ((iterable && (name == name("iterator")))
							|| (name == name("hasNext")) || (name == name("next"))
							|| (name == name("remove"))) {
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
							addStatement(setStateId(literal(label)));
							addStatement(ret(literal(true)));
							addStatement(label);

							Scope next = getFinallyScope(parent, null);

							if (next != null) {
								maker.Case(literal(label),
														list(assign(
																next.labelName,
																literal(getBreakLabel(root))),
																setStateId(literal(getFinallyLabel(next))),
																maker.Continue(null)));
							}
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
		public Name labelName;
		public JCCase iterationLabel;
		public JCCase breakLabel;
		public JCCase finallyLabel;
		
		public Scope(Scope parent, JCTree node) {
			this.parent = parent;
			this.node = node;
		}

		public abstract void refactor();
	}
	
	public static class ErrorHandler {
		public int begin;
		public int end;
		public ListBuffer<JCStatement> statements = new ListBuffer<JCStatement>();
	}
}
