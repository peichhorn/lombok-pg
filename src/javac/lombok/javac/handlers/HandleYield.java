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
 * 
 * Credit where credit is due:
 * ===========================
 * 
 *   Yield is based on the idea, algorithms and sources of
 *   Arthur and Vladimir Nesterovsky (http://www.nesterovsky-bros.com/weblog),
 *   who generously allowed me to use them.
 */
package lombok.javac.handlers;

import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Lists.notNull;
import static lombok.core.util.Names.*;
import static lombok.core.util.Types.*;
import static lombok.javac.handlers.Javac.*;

import java.util.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.util.Lists;
import lombok.core.util.Names;
import lombok.javac.JavacASTAdapter;
import lombok.javac.JavacASTVisitor;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacMethod;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
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
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.util.Name;

@ProviderFor(JavacASTVisitor.class)
public class HandleYield extends JavacASTAdapter {
	private final Set<String> methodNames = new HashSet<String>();

	@Override public void visitCompilationUnit(final JavacNode top, final JCCompilationUnit unit) {
		methodNames.clear();
	}

	@Override public void visitStatement(final JavacNode statementNode, final JCTree statement) {
		if (statement instanceof JCMethodInvocation) {
			JCMethodInvocation methodCall = (JCMethodInvocation) statement;
			String methodName = methodCall.meth.toString();
			if (isMethodCallValid(statementNode, methodName, Yield.class, "yield")) {
				final JavacMethod method = JavacMethod.methodOf(statementNode, statement);
				if ((method == null) || method.isConstructor()) {
					statementNode.addError(canBeUsedInBodyOfMethodsOnly("yield"));
				} else if (handle(method)) {
					methodNames.add(methodName);
				}
			}
		}
	}

	@Override public void endVisitCompilationUnit(final JavacNode top, final JCCompilationUnit unit) {
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
		if (method.hasNonFinalArgument()) {
			method.node().addError("Parameters should be final.");
			return true;
		}

		final String stateName = "$state";
		final String nextName = "$next";
		final String errorName = "$yieldException";
		YieldDataCollector collector = new YieldDataCollector();
		collector.collect(method, stateName, nextName, errorName);

		if (!collector.hasYields()) {
			return true;
		}

		final String yielderName = yielderName(method);
		final String elementType = elementType(method);
		final List<FieldDecl> variables = collector.getStateVariables();
		final Switch stateSwitch = collector.getStateSwitch();
		final Statement errorHandler = collector.getErrorHandler();

		ClassDecl yielder = ClassDecl(yielderName).makeLocal().implementing(Type("java.util.Iterator").withTypeArgument(Type(elementType))) //
			.withFields(variables) //
			.withField(FieldDecl(Type("int"), stateName).makePrivate()) //
			.withField(FieldDecl(Type("boolean"), "$hasNext").makePrivate()) //
			.withField(FieldDecl(Type("boolean"), "$nextDefined").makePrivate()) //
			.withField(FieldDecl(Type(elementType), nextName).makePrivate()); //
		if (returnsIterable) {
			yielder.implementing(Type("java.lang.Iterable").withTypeArgument(Type(elementType))) //
				.withMethod(MethodDecl(Type("java.util.Iterator").withTypeArgument(Type(elementType)), "iterator").makePublic().withStatement(Return(New(Type(yielderName)))));
		}
		yielder.withMethod(MethodDecl(Type("boolean"), "hasNext").makePublic() //
				.withStatement(If(Not(Name("$nextDefined"))).Then(Block() //
					.withStatement(Assign(Name("$hasNext"), Call("getNext"))) //
					.withStatement(Assign(Name("$nextDefined"), True())))) //
				.withStatement(Return(Name("$hasNext")))) //
			.withMethod(MethodDecl(Type(elementType), "next").makePublic() //
				.withStatement(If(Not(Call("hasNext"))).Then(Block().withStatement(Throw(New(Type("java.util.NoSuchElementException")))))) //
				.withStatement(Assign(Name("$nextDefined"), False())) //
				.withStatement(Return(Name(nextName)))) //
			.withMethod(MethodDecl(Type("void"), "remove").makePublic().withStatement(Throw(New(Type("java.lang.UnsupportedOperationException")))));
		if (errorHandler != null) {
			String caughtErrorName = errorName + "Caught";
			yielder.withMethod(MethodDecl(Type("boolean"), "getNext").makePrivate() //
				.withStatement(LocalDecl(Type(Throwable.class), errorName)) //
				.withStatement(While(True()).Do(Block().withStatement(Try(Block().withStatement(stateSwitch)) //
					.Catch(Arg(Type(Throwable.class), caughtErrorName), Block().withStatement(Assign(Name(errorName), Name(caughtErrorName))).withStatement(errorHandler))))));
		} else {
			yielder.withMethod(MethodDecl(Type("boolean"), "getNext").makePrivate().withStatement(While(True()).Do(stateSwitch)));
		}
		method.replaceBody(yielder, Return(New(Type(yielderName))));
		method.rebuild();

		return true;
	}

	private String yielderName(final JavacMethod method) {
		String[] parts = method.name().split("_");
		String[] newParts = new String[parts.length + 1];
		newParts[0] = "yielder";
		System.arraycopy(parts, 0, newParts, 1, parts.length);
		return camelCase("$", newParts);
	}

	private String elementType(final JavacMethod method) {
		JCExpression type = method.get().restype;
		if (type instanceof JCTypeApply) {
			return ((JCTypeApply) type).arguments.head.type.toString();
		} else {
			return Object.class.getName();
		}
	}

	private static class YieldDataCollector {
		private JavacMethod method;
		private Set<String> names = new HashSet<String>();
		private List<Scope> yields = new ArrayList<Scope>();
		private List<Scope> breaks = new ArrayList<Scope>();
		private List<Scope> variableDecls = new ArrayList<Scope>();
		@Getter
		private List<FieldDecl> stateVariables = new ArrayList<FieldDecl>();
		private Scope root;
		private Map<JCTree, Scope> allScopes = new HashMap<JCTree, Scope>();
		private List<Case> cases = new ArrayList<Case>();
		private List<Statement> statements = new ArrayList<Statement>();
		private List<ErrorHandler> errorHandlers = new ArrayList<ErrorHandler>();
		private int finallyBlocks;
		private Map<NumberLiteral, Case> labelLiterals = new HashMap<NumberLiteral, Case>();
		private Set<Case> usedLabels = new HashSet<Case>();
		private String stateName;
		private String nextName;
		private String errorName;

		public Switch getStateSwitch() {
			final List<Case> switchCases = new ArrayList<Case>();
			for (Case label : cases) if (label != null) switchCases.add(label);
			return Switch(Name(stateName)).withCases(switchCases).withCase(Case().withStatement(Return(False())));
		}

		public Statement getErrorHandler() {
			if (errorHandlers.isEmpty()) {
				return null;
			} else {
				final List<Case> switchCases = new ArrayList<Case>();
				final Set<Case> labels = new HashSet<Case>();
				for (ErrorHandler handler: errorHandlers) {
					Case lastCase = null;
					for(int i = handler.begin; i < handler.end; i++) {
						Case label = cases.get(i);
						if ((label != null) && labels.add(label)) {
							lastCase = Case(label.getPattern());
							switchCases.add(lastCase);
						}
					}
					if (lastCase != null) {
						lastCase.withStatements(handler.statements);
					}
				}
				
				final String unhandledErrorName = errorName + "Unhandled";
				switchCases.add(Case() //
					.withStatement(setState(literal(getBreakLabel(root)))) //
					.withStatement(LocalDecl(Type(ConcurrentModificationException.class), unhandledErrorName).withInitialization(New(Type(ConcurrentModificationException.class)))) //
					.withStatement(Call(Name(unhandledErrorName), "initCause").withArgument(Name(errorName))) //
					.withStatement(Throw(Name(unhandledErrorName))));
				return Switch(Name(stateName)).withCases(switchCases);
			}
		}

		public boolean hasYields() {
			return !yields.isEmpty();
		}

		public void collect(final JavacMethod method, final String state, final String next, final String errorName) {
			this.method = method;
			this.stateName = state;
			this.nextName = next;
			this.errorName = errorName;
			try {
				if (scan()) {
					refactor();
				}
			} catch (final Exception ignore) {
			}
		}

		private boolean scan() {
			try {
				new YieldQuickScanner().scan(method.get().body);
				return false;
			} catch (final IllegalStateException ignore) {
				// this means there are unhandled yields left
			}

			YieldScanner scanner = new YieldScanner();
			scanner.scan(method.get().body);

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
					FieldDecl field = FieldDecl(Type(variable.vartype), string(variable.name)).makePrivate();
					if (scope.parent.node instanceof JCTry) {
						field.withAnnotation(Annotation(Type(SuppressWarnings.class)).withValue(String("unused")));
					}
					stateVariables.add(field);
				}
			}
			return true;
		}

		private void refactor() {
			root = allScopes.get(method.get().body);
			Case iteratorLabel = getIterationLabel(root);

			usedLabels.add(iteratorLabel);
			usedLabels.add(getBreakLabel(root));

			addLabel(iteratorLabel);
			root.refactor();
			endCase();

			optimizeStates();
			synchronizeLiteralsAndLabels();
		}

		private JCExpression getYieldExpression(final JCExpression expr) {
			if (expr instanceof JCMethodInvocation) {
				JCMethodInvocation methodCall = (JCMethodInvocation) expr;
				if (methodCall.meth.toString().endsWith("yield") && (methodCall.args.length() == 1)) {
					return methodCall.args.head;
				}
			}
			return null;
		}

		private boolean isTrueLiteral(final JCExpression expression) {
			if (expression instanceof JCLiteral) {
				return "true".equals(expression.toString());
			} else if (expression instanceof JCParens) {
				return isTrueLiteral(((JCParens) expression).expr);
			}
			return false;
		}

		private void endCase() {
			if (!cases.isEmpty()) {
				Case lastCase = cases.get(cases.size() - 1);
				if (lastCase.getStatements().isEmpty() && !statements.isEmpty()) {
					lastCase.withStatements(statements);
					statements.clear();
				}
			}
		}

		private void addLabel(final Case label) {
			endCase();
			label.withPattern(Number(cases.size()));
			cases.add(label);
		}

		private void addStatement(final Statement statement) {
			statements.add(statement);
		}

		private Case getBreakLabel(final Scope scope) {
			Case label = scope.breakLabel;
			if (label == null) {
				label = Case();
				scope.breakLabel = label;
			}
			return label;
		}

		private Case getIterationLabel(final Scope scope) {
			Case label = scope.iterationLabel;
			if (label == null) {
				label = Case();
				scope.iterationLabel = label;
			}
			return label;
		}

		private Case getFinallyLabel(Scope scope) {
			Case label = scope.finallyLabel;
			if (label == null) {
				label = Case();
				scope.finallyLabel = label;
			}
			return label;
		}

		private Scope getFinallyScope(Scope scope, Scope top) {
			JCTree previous = null;
			while(scope != null) {
				JCTree tree = scope.node;
				if (tree instanceof JCTry) {
					JCTry statement = (JCTry) tree;
					if ((statement.finalizer != null) && (statement.finalizer != previous)) {
						return scope;
					}
				}
				if (scope == top) break;
				previous = tree;
				scope = scope.parent;
			}
			return null;
		}

		private Expression literal(final Case label) {
			NumberLiteral pattern = (NumberLiteral) label.getPattern();
			NumberLiteral literal = Number(pattern == null ? -1 : pattern.getNumber());
			labelLiterals.put(literal, label);
			return literal;
		}

		private Statement setState(final Expression expression) {
			return Assign(Name(stateName), expression);
		}

		private void refactorStatement(final JCStatement statement) {
			if (statement == null) {
				return;
			}
			Scope scope = allScopes.get(statement);
			if (scope != null) {
				scope.refactor();
			} else {
				addStatement(Stat(statement));
			}
		}

		private void optimizeStates() {
			int diff = 0;
			Case previous = null;
			for (int i = 1; i < cases.size(); i++) {
				Case label = cases.get(i);
				NumberLiteral literal = (NumberLiteral) label.getPattern();
				literal.setNumber((Integer) literal.getNumber() - diff);
				if (!usedLabels.contains(label)) {
					if (label.getStatements().isEmpty()) {
						cases.set(i, null);
						diff++;
					} else if ((previous != null) && isNoneOf(previous.getStatements().get(previous.getStatements().size() - 1), Continue.class, Return.class)) {
						previous.withStatements(label.getStatements());
						cases.set(i, null);
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
			for (Map.Entry<NumberLiteral, Case> entry : labelLiterals.entrySet()) {
				Case label = entry.getValue();
				if (label != null) {
					NumberLiteral literal = (NumberLiteral) label.getPattern();
					entry.getKey().setNumber(literal.getNumber());
				}
			}
		}

		private class YieldQuickScanner extends TreeScanner {
			@Override
			public void visitExec(final JCExpressionStatement tree) {
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
						for (JCStatement statement : notNull(tree.stats)) {
							refactorStatement(statement);
						}
						addLabel(getBreakLabel(this));
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
						for (JCStatement statement : notNull(tree.init)) {
							refactorStatement(statement);
						}
						Case label = Case();
						Case breakLabel = getBreakLabel(this);
						addLabel(label);
						if ((tree.cond != null) && !isTrueLiteral(tree.cond)) {
							addStatement(If(Not(Expr(tree.cond))).Then(Block().withStatement(setState(literal(breakLabel))).withStatement(Continue())));
						}
						refactorStatement(tree.body);
						addLabel(getIterationLabel(this));
						for (JCStatement statement : notNull(tree.step)) {
							refactorStatement(statement);
						}
						addStatement(setState(literal(label)));
						addStatement(Continue());
						addLabel(breakLabel);
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
						String iteratorVar = "$" + string(tree.var.name) + "Iter";
						stateVariables.add(FieldDecl(Type("java.util.Iterator"), iteratorVar).makePrivate().withAnnotation(Annotation(Type(SuppressWarnings.class)).withValue(String("all"))));

						addStatement(Assign(Name(iteratorVar), Call(Expr(tree.expr), "iterator")));
						addLabel(getIterationLabel(this));
						addStatement(If(Not(Call(Name(iteratorVar), "hasNext"))).Then(Block().withStatement(setState(literal(getBreakLabel(this)))).withStatement(Continue())));
						addStatement(Assign(Name(string(tree.var.name)), Cast(Type(tree.var.vartype), Call(Name(iteratorVar), "next"))));

						refactorStatement(tree.body);
						addStatement(setState(literal(getIterationLabel(this))));
						addStatement(Continue());
						addLabel(getBreakLabel(this));
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
						addLabel(getIterationLabel(this));
						refactorStatement(tree.body);
						addStatement(If(Expr(tree.cond)).Then(Block().withStatement(setState(literal(getIterationLabel(this)))).withStatement(Continue())));
						addLabel(getBreakLabel(this));
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
						addLabel(getIterationLabel(this));
						if (!isTrueLiteral(tree.cond)) {
							addStatement(If(Not(Expr(tree.cond))).Then(Block().withStatement(setState(literal(getBreakLabel(this)))).withStatement(Continue())));
						}
						refactorStatement(tree.body);
						addStatement(setState(literal(getIterationLabel(this))));
						addStatement(Continue());
						addLabel(getBreakLabel(this));
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
						Case label = tree.elsepart == null ? getBreakLabel(this) : Case();
						addStatement(If(Not(Expr(tree.cond))).Then(Block().withStatement(setState(literal(label))).withStatement(Continue())));
						if (tree.elsepart != null) {
							refactorStatement(tree.thenpart);
							addStatement(setState(literal(getBreakLabel(this))));
							addStatement(Continue());
							addLabel(label);
							refactorStatement(tree.elsepart);
							addLabel(getBreakLabel(this));
						} else {
							refactorStatement(tree.thenpart);
							addLabel(getBreakLabel(this));
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
						Case breakLabel = getBreakLabel(this);
						Switch switchStatement = Switch(Expr(tree.selector));
						addStatement(switchStatement);
						if (!Lists.isEmpty(tree.cases)) {
							boolean hasDefault = false;
							for (JCCase item : tree.cases) {
								if (item.pat == null) {
									hasDefault = true;
								}
								Case label = Case();
								switchStatement.withCase(Case(Expr(item.pat)).withStatement(setState(literal(label))).withStatement(Continue()));
								addLabel(label);
								for (JCStatement statement : item.stats) {
									refactorStatement(statement);
								}
							}
							if (!hasDefault) {
								switchStatement.withCase(Case().withStatement(setState(literal(breakLabel))).withStatement(Continue()));
							}
						}
						addLabel(breakLabel);
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
						boolean hasCatch = !Lists.isEmpty(tree.catchers);
						ErrorHandler catchHandler = null;
						ErrorHandler finallyHandler = null;
						Case tryLabel = Case();
						Case finallyLabel;
						Case breakLabel = getBreakLabel(this);
						String finallyErrorName = null;
						
						if (hasFinally) {
							finallyHandler = new ErrorHandler();
							finallyLabel = getFinallyLabel(this);
							finallyBlocks++;
							finallyErrorName = errorName + finallyBlocks;
							labelName = "$id" + finallyBlocks;
							
							stateVariables.add(FieldDecl(Type(Throwable.class), finallyErrorName).makePrivate());
							stateVariables.add(FieldDecl(Type("int"), labelName).makePrivate());
							
							addStatement(Assign(Name(finallyErrorName), Null()));
							addStatement(Assign(Name(labelName), literal(breakLabel)));
						} else {
							finallyLabel = breakLabel;
						}
						
						addStatement(setState(literal(tryLabel)));
						
						if (hasCatch) {
							catchHandler = new ErrorHandler();
							catchHandler.begin = cases.size();
						} else if (hasFinally) {
							finallyHandler.begin = cases.size();
						}
						
						addLabel(tryLabel);
						refactorStatement(tree.body);
						addStatement(setState(literal(finallyLabel)));
						
						if (hasCatch) {
							addStatement(Continue());
							catchHandler.end = cases.size();
							for(JCCatch catcher: tree.catchers) {
								Case label = Case();
								usedLabels.add(label);
								addLabel(label);
								refactorStatement(catcher.body);
								addStatement(setState(literal(finallyLabel)));
								addStatement(Continue());
								
								catchHandler.statements.add(If(InstanceOf(Name(errorName), Type(catcher.param.vartype))).Then(Block() //
									.withStatement(Assign(Name(string(catcher.param.name)), Cast(Type(catcher.param.vartype), Name(errorName)))) //
									.withStatement(setState(literal(label))).withStatement(Continue())));
							}
							
							errorHandlers.add(catchHandler);
							
							if (hasFinally) {
								finallyHandler.begin = catchHandler.end;
							}
						}
						
						if (hasFinally) {
							finallyHandler.end = cases.size();
							addLabel(finallyLabel);
							refactorStatement(tree.finalizer);
							
							addStatement(If(NotEqual(Name(finallyErrorName), Null())).Then(Block().withStatement(Assign(Name(errorName), Name(finallyErrorName))).withStatement(Break())));
							
							Scope next = getFinallyScope(parent, null);
							if (next != null) {
								Case label = getFinallyLabel(next);
								addStatement(If(Binary(Name(labelName), ">", literal(label))).Then(Block() //
									.withStatement(Assign(Name(next.labelName), Name(labelName))) //
									.withStatement(setState(literal(label))).withStatement(setState(Name(labelName)))));
							} else {
								addStatement(setState(Name(labelName)));
							}
							
							addStatement(Continue());
							
							finallyHandler.statements.add(Assign(Name(finallyErrorName), Name(errorName)));
							finallyHandler.statements.add(setState(literal(finallyLabel)));
							finallyHandler.statements.add(Continue());
							
							usedLabels.add(finallyLabel);
							errorHandlers.add(finallyHandler);
						}
						addLabel(breakLabel);
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
							addStatement(Assign(Name(string(tree.name)), Expr(tree.init)));
						}
					}
				};

				variableDecls.add(current);
				super.visitVarDef(tree);
				current = current.parent;
			}

			@Override
			public void visitReturn(final JCReturn tree) {
				method.node().addError("The 'return' expression is permitted.");
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
									method.node().addError("Invalid label.");
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
					method.node().addError("Invalid break.");
				}

				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						Scope next = getFinallyScope(parent, target);
						Case label = getBreakLabel(target);
						
						if (next == null) {
							addStatement(setState(literal(label)));
							addStatement(Continue());
						} else {
							addStatement(Assign(Name(next.labelName), literal(label)));
							addStatement(setState(literal(getFinallyLabel(next))));
							addStatement(Continue());
						}
					}
				};

				current.target = target;
				breaks.add(current);
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
									method.node().addError("Invalid label.");
								}
								if (isOneOf(labelScope.node, JCForLoop.class, JCEnhancedForLoop.class, JCWhileLoop.class, JCDoWhileLoop.class)) {
									target = labelScope;
								} else {
									method.node().addError("Invalid continue.");
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
					method.node().addError("Invalid continue.");
				}

				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						Scope next = getFinallyScope(parent, target);
						Case label = getIterationLabel(target);
						
						if (next == null) {
							addStatement(setState(literal(label)));
							addStatement(Continue());
						} else {
							addStatement(Assign(Name(next.labelName), literal(label)));
							addStatement(setState(literal(getFinallyLabel(next))));
							addStatement(Continue());
						}
					}
				};

				current.target = target;
				breaks.add(current);
				current = current.parent;
			}

			@Override
			public void visitApply(final JCMethodInvocation tree) {
				if (tree.meth instanceof JCIdent) {
					String name = string(tree.meth);
					if (Names.isOneOf(name, "hasNext", "next", "remove")) {
						method.node().addError("Cannot call method " + name + "(), as it is hidden.");
					}
				}

				super.visitApply(tree);
			}

			@Override
			public void visitExec(final JCExpressionStatement tree) {
				final JCExpression expression = getYieldExpression(tree.expr);
				if (expression != null) {
					current = new Scope(current, tree) {
						@Override
						public void refactor() {
							Case label = getBreakLabel(this);
							addStatement(Assign(Name(nextName), Expr(expression)));
							addStatement(setState(literal(label)));
							addStatement(Return(True()));
							addLabel(label);
						}
					};
					yields.add(current);
					scan(expression);
					current = current.parent;
				} else {
					super.visitExec(tree);
				}
			}

			@Override
			public void visitIdent(final JCIdent tree) {
				if ("this".equals(tree.name.toString())) {
					method.node().addError("No unqualified 'this' expression is permitted.");
				}
				if ("super".equals(tree.name.toString())) {
					method.node().addError("No unqualified 'super' expression is permitted.");
				}
				names.add(tree.name.toString());
				super.visitIdent(tree);
			}

			@Override
			public void visitNewClass(final JCNewClass tree) {
				scan(tree.encl);
				scan(tree.clazz);
				scan(tree.args);
			}
		}
	}

	private static class ErrorHandler {
		public int begin;
		public int end;
		public List<Statement> statements = new ArrayList<Statement>();
	}

	@RequiredArgsConstructor
	private abstract static class Scope {
		public final Scope parent;
		public final JCTree node;
		public Scope target;
		public String labelName;
		public Case iterationLabel;
		public Case breakLabel;
		public Case finallyLabel;

		public abstract void refactor();
	}
}
