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
package lombok.eclipse.handlers;

import static lombok.eclipse.handlers.Eclipse.*;
import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.core.util.Types.*;
import static lombok.core.util.Lists.list;

import java.util.*;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.BreakStatement;
import org.eclipse.jdt.internal.compiler.ast.CaseStatement;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ContinueStatement;
import org.eclipse.jdt.internal.compiler.ast.DoStatement;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.SuperReference;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.mangosdk.spi.ProviderFor;

import lombok.*;
import lombok.eclipse.EclipseASTAdapter;
import lombok.eclipse.EclipseASTVisitor;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseMethod;

@ProviderFor(EclipseASTVisitor.class)
public class HandleYield extends EclipseASTAdapter {
	private final Set<String> methodNames = new HashSet<String>();

	@Override public void visitCompilationUnit(final EclipseNode top, final CompilationUnitDeclaration unit) {
		methodNames.clear();
	}

	@Override public void visitStatement(final EclipseNode statementNode, final Statement statement) {
		if (statement instanceof MessageSend) {
			String methodName = getMethodName((MessageSend) statement);
			if (isMethodCallValid(statementNode, methodName, Yield.class, "yield")) {
				final EclipseMethod method = EclipseMethod.methodOf(statementNode, statement);
				if ((method == null) || method.isConstructor()) {
					statementNode.addError(canBeUsedInBodyOfMethodsOnly("yield"));
				} else if (handle(method)) {
					methodNames.add(methodName);
				}
			}
		}
	}

	@Override public void endVisitCompilationUnit(final EclipseNode top, final CompilationUnitDeclaration unit) {
		for (String methodName : methodNames) {
			deleteMethodCallImports(top, methodName, Yield.class, "yield");
		}
	}

	public boolean handle(final EclipseMethod method) {
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
		final List<lombok.ast.FieldDecl> variables = collector.getStateVariables();
		final lombok.ast.Switch stateSwitch= collector.getStateSwitch();
		final lombok.ast.Statement errorHandler = collector.getErrorHandler();

		lombok.ast.ClassDecl yielder = ClassDecl(yielderName).makeLocal().implementing(Type("java.util.Iterator").withTypeArgument(Type(elementType))) //
			.withFields(variables) //
			.withField(FieldDecl(Type("int"), stateName).makePrivate()) //
			.withField(FieldDecl(Type("boolean"), "$hasNext").makePrivate()) //
			.withField(FieldDecl(Type("boolean"), "$nextDefined").makePrivate()) //
			.withField(FieldDecl(Type(elementType), nextName).makePrivate()) //
			.withMethod(ConstructorDecl(yielderName).withImplicitSuper().makePrivate()); //
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

	private String yielderName(final EclipseMethod method) {
		String[] parts = method.name().split("_");
		String[] newParts = new String[parts.length + 1];
		newParts[0] = "yielder";
		System.arraycopy(parts, 0, newParts, 1, parts.length);
		return camelCase("$", newParts);
	}

	private String elementType(final EclipseMethod method) {
		MethodDeclaration methodDecl = (MethodDeclaration)method.get();
		TypeReference type = methodDecl.returnType;
		if (type instanceof ParameterizedSingleTypeReference) {
			ParameterizedSingleTypeReference paramType = (ParameterizedSingleTypeReference)type;
			if (paramType.typeArguments != null) {
				return paramType.typeArguments[0].toString();
			}
		}
		return Object.class.getName();
	}

	private static class YieldDataCollector {
		private EclipseMethod method;
		private Set<String> names = new HashSet<String>();
		private List<Scope> yields = new ArrayList<Scope>();
		private List<Scope> breaks = new ArrayList<Scope>();
		private List<Scope> variableDecls = new ArrayList<Scope>();
		@Getter
		private List<lombok.ast.FieldDecl> stateVariables = new ArrayList<lombok.ast.FieldDecl>();
		private Scope root;
		private Map<ASTNode, Scope> allScopes = new HashMap<ASTNode, Scope>();
		private List<lombok.ast.Case> cases = new ArrayList<lombok.ast.Case>();
		private List<lombok.ast.Statement> statements = new ArrayList<lombok.ast.Statement>();
		private List<ErrorHandler> errorHandlers = new ArrayList<ErrorHandler>();
		private int finallyBlocks;
		private Map<lombok.ast.NumberLiteral, lombok.ast.Case> labelLiterals = new HashMap<lombok.ast.NumberLiteral, lombok.ast.Case>();
		private Set<lombok.ast.Case> usedLabels = new HashSet<lombok.ast.Case>();
		private String stateName;
		private String nextName;
		private String errorName;

		public lombok.ast.Switch getStateSwitch() {
			final List<lombok.ast.Case> switchCases = new ArrayList<lombok.ast.Case>();
			for (lombok.ast.Case label : cases) if (label != null) switchCases.add(label);
			return Switch(Name(stateName)).withCases(switchCases).withCase(Case().withStatement(Return(False())));
		}

		public lombok.ast.Statement getErrorHandler() {
			if (errorHandlers.isEmpty()) {
				return null;
			} else {
				final List<lombok.ast.Case> switchCases = new ArrayList<lombok.ast.Case>();
				final Set<lombok.ast.Case> labels = new HashSet<lombok.ast.Case>();
				for (ErrorHandler handler: errorHandlers) {
					lombok.ast.Case lastCase = null;
					for(int i = handler.begin; i < handler.end; i++) {
						lombok.ast.Case label = cases.get(i);
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

		public void collect(final EclipseMethod method, final String state, final String next, final String errorName) {
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
				method.get().traverse(new YieldQuickScanner(), (ClassScope)null);
				return false;
			} catch (final IllegalStateException ignore) {
				// this means there are unhandled yields left
			}

			ValidationScanner scanner = new ValidationScanner();
			method.get().traverse(scanner, (ClassScope)null);

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
					if ((scope.parent.node instanceof TryStatement) && allScopes.containsKey(scope.parent.parent.node)) {
						stateVariable = true;
					}
				}

				if (stateVariable) {
					LocalDeclaration variable = (LocalDeclaration) scope.node;
					allScopes.put(scope.node, scope);
					lombok.ast.FieldDecl field = FieldDecl(Type(variable.type), string(variable.name)).makePrivate();
					if (scope.parent.node instanceof TryStatement) {
						field.withAnnotation(Annotation(Type(SuppressWarnings.class)).withValue(String("unused")));
					}
					stateVariables.add(field);
				}
			}
			return true;
		}

		private void refactor() {
			root = allScopes.get(method.get());
			lombok.ast.Case iteratorLabel = getIterationLabel(root);

			usedLabels.add(iteratorLabel);
			usedLabels.add(getBreakLabel(root));

			addLabel(iteratorLabel);
			root.refactor();
			endCase();

			optimizeStates();
			synchronizeLiteralsAndLabels();
		}

		private Expression getYieldExpression(final MessageSend invoke) {
			if ("yield".equals(new String(invoke.selector)) && (invoke.arguments != null) && (invoke.arguments.length == 1)) {
				return invoke.arguments[0];
			}
			return null;
		}

		private boolean isTrueLiteral(final Expression expression) {
			return expression instanceof TrueLiteral;
		}

		private void endCase() {
			if (!cases.isEmpty()) {
				lombok.ast.Case lastCase = cases.get(cases.size() - 1);
				if (lastCase.getStatements().isEmpty() && !statements.isEmpty()) {
					lastCase.withStatements(statements);
					statements.clear();
				}
			}
		}

		private void addLabel(final lombok.ast.Case label) {
			endCase();
			label.withPattern(Number(cases.size()));
			cases.add(label);
		}

		private void addStatement(final lombok.ast.Statement statement) {
			statements.add(statement);
		}

		private lombok.ast.Case getBreakLabel(final Scope scope) {
			lombok.ast.Case label = scope.breakLabel;
			if (label == null) {
				label = Case();
				scope.breakLabel = label;
			}
			return label;
		}

		private lombok.ast.Case getIterationLabel(final Scope scope) {
			lombok.ast.Case label = scope.iterationLabel;
			if (label == null) {
				label = Case();
				scope.iterationLabel = label;
			}
			return label;
		}

		private lombok.ast.Case getFinallyLabel(Scope scope) {
			lombok.ast.Case label = scope.finallyLabel;
			if (label == null) {
				label = Case();
				scope.finallyLabel = label;
			}
			return label;
		}

		private Scope getFinallyScope(Scope scope, Scope top) {
			ASTNode previous = null;
			while(scope != null) {
				ASTNode tree = scope.node;
				if (tree instanceof TryStatement) {
					TryStatement statement = (TryStatement) tree;
					if ((statement.finallyBlock != null) && (statement.finallyBlock != previous)) {
						return scope;
					}
				}
				if (scope == top) break;
				previous = tree;
				scope = scope.parent;
			}
			return null;
		}

		private lombok.ast.Expression literal(final lombok.ast.Case label) {
			lombok.ast.NumberLiteral pattern = (lombok.ast.NumberLiteral) label.getPattern();
			lombok.ast.NumberLiteral literal = Number(pattern == null ? -1 : pattern.getNumber());
			labelLiterals.put(literal, label);
			return literal;
		}

		private lombok.ast.Statement setState(final lombok.ast.Expression expression) {
			return Assign(Name(stateName), expression);
		}

		private void refactorStatement(final Statement statement) {
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
			lombok.ast.Case previous = null;
			for (int i = 1; i < cases.size(); i++) {
				lombok.ast.Case label = cases.get(i);
				lombok.ast.NumberLiteral literal = (lombok.ast.NumberLiteral) label.getPattern();
				literal.setNumber((Integer) literal.getNumber() - diff);
				if (!usedLabels.contains(label)) {
					if (label.getStatements().isEmpty()) {
						cases.set(i, null);
						diff++;
					} else if ((previous != null) && isNoneOf(previous.getStatements().get(previous.getStatements().size() - 1), lombok.ast.Continue.class, lombok.ast.Return.class)) {
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
			for (Map.Entry<lombok.ast.NumberLiteral, lombok.ast.Case> entry : labelLiterals.entrySet()) {
				lombok.ast.Case label = entry.getValue();
				if (label != null) {
					lombok.ast.NumberLiteral literal = (lombok.ast.NumberLiteral) label.getPattern();
					entry.getKey().setNumber(literal.getNumber());
				}
			}
		}

		private class YieldQuickScanner extends ASTVisitor {
			@Override
			public boolean visit(final MessageSend tree, final BlockScope scope) {
				final Expression expression = getYieldExpression(tree);
				if (expression != null) {
					throw new IllegalStateException();
				} else {
					return super.visit(tree, scope);
				}
			}
		}

		private class ValidationScanner extends ASTVisitor {
			private Scope current;

			@Override
			public boolean visit(final MethodDeclaration tree, final ClassScope scope) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						for (Statement statement : list(tree.statements)) {
							refactorStatement(statement);
						}
						addLabel(getBreakLabel(this));
					}
				};

				return super.visit(tree, scope);
			}

			@Override
			public void endVisit(final MethodDeclaration tree, final ClassScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final Block tree, final BlockScope scope) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						for (Statement statement : list(tree.statements)) {
							refactorStatement(statement);
						}
						addLabel(getBreakLabel(this));
					}
				};

				return super.visit(tree, scope);
			}

			@Override
			public void endVisit(final Block tree, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final LabeledStatement tree, final BlockScope scope) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						refactorStatement(tree.statement);
					}
				};

				return super.visit(tree, scope);
			}

			@Override
			public void endVisit(final LabeledStatement tree, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final ForStatement tree, final BlockScope scope) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						for (Statement statement : list(tree.initializations)) {
							refactorStatement(statement);
						}
						lombok.ast.Case label = Case();
						lombok.ast.Case breakLabel = getBreakLabel(this);
						addLabel(label);
						if ((tree.condition != null) && !isTrueLiteral(tree.condition)) {
							addStatement(If(Not(Expr(tree.condition))).Then(Block().withStatement(setState(literal(breakLabel))).withStatement(Continue())));
						}
						refactorStatement(tree.action);
						addLabel(getIterationLabel(this));
						for (Statement statement : list(tree.increments)) {
							refactorStatement(statement);
						}
						addStatement(setState(literal(label)));
						addStatement(Continue());
						addLabel(breakLabel);
					}
				};

				return super.visit(tree, scope);
			}

			@Override
			public void endVisit(final ForStatement tree, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final ForeachStatement tree, final BlockScope scope) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						String iteratorVar = "$" + string(tree.elementVariable.name) + "Iter";
						stateVariables.add(FieldDecl(Type("java.util.Iterator"), iteratorVar).makePrivate().withAnnotation(Annotation(Type(SuppressWarnings.class)).withValue(String("all"))));

						addStatement(Assign(Name(iteratorVar), Call(Expr(tree.collection), "iterator")));
						addLabel(getIterationLabel(this));
						addStatement(If(Not(Call(Name(iteratorVar), "hasNext"))).Then(Block().withStatement(setState(literal(getBreakLabel(this)))).withStatement(Continue())));
						addStatement(Assign(Name(string(tree.elementVariable.name)), Cast(Type(tree.elementVariable.type), Call(Name(iteratorVar), "next"))));

						refactorStatement(tree.action);
						addStatement(setState(literal(getIterationLabel(this))));
						addStatement(Continue());
						addLabel(getBreakLabel(this));
					}
				};

				return super.visit(tree, scope);
			}

			@Override
			public void endVisit(final ForeachStatement tree, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final DoStatement tree, final BlockScope scope) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						addLabel(getIterationLabel(this));
						refactorStatement(tree.action);
						addStatement(If(Expr(tree.condition)).Then(Block().withStatement(setState(literal(breakLabel))).withStatement(Continue())));
						addLabel(getBreakLabel(this));
					}
				};

				return super.visit(tree, scope);
			}

			@Override
			public void endVisit(final DoStatement tree, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final WhileStatement tree, final BlockScope scope) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						addLabel(getIterationLabel(this));
						if (!isTrueLiteral(tree.condition)) {
							addStatement(If(Not(Expr(tree.condition))).Then(Block().withStatement(setState(literal(getBreakLabel(this)))).withStatement(Continue())));
						}
						refactorStatement(tree.action);
						addStatement(setState(literal(getIterationLabel(this))));
						addStatement(Continue());
						addLabel(getBreakLabel(this));
					}
				};

				return super.visit(tree, scope);
			}

			@Override
			public void endVisit(final WhileStatement tree, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final IfStatement tree, final BlockScope scope) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						lombok.ast.Case label = tree.elseStatement == null ? getBreakLabel(this) : Case();
						addStatement(If(Not(Expr(tree.condition))).Then(Block().withStatement(setState(literal(label))).withStatement(Continue())));
						if (tree.elseStatement != null) {
							refactorStatement(tree.thenStatement);
							addStatement(setState(literal(getBreakLabel(this))));
							addStatement(Continue());
							addLabel(label);
							refactorStatement(tree.elseStatement);
							addLabel(getBreakLabel(this));
						} else {
							refactorStatement(tree.thenStatement);
							addLabel(getBreakLabel(this));
						}
					}
				};

				return super.visit(tree, scope);
			}

			@Override
			public void endVisit(final IfStatement tree, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final SwitchStatement tree, final BlockScope scope) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						lombok.ast.Case breakLabel = getBreakLabel(this);
						lombok.ast.Switch switchStatement = Switch(Expr(tree.expression));
						addStatement(switchStatement);
						if (list(tree.statements).isEmpty()) {
							boolean hasDefault = false;
							for (Statement statement : tree.statements) {
								if (statement instanceof CaseStatement) {
									CaseStatement caseStatement = (CaseStatement) statement;
									if (caseStatement.constantExpression == null) {
										hasDefault = true;
									}
									lombok.ast.Case label = Case();
									switchStatement.withCase(Case(Expr(caseStatement.constantExpression)).withStatement(setState(literal(label))).withStatement(Continue()));
									addLabel(label);
								} else {
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

				return super.visit(tree, scope);
			}

			@Override
			public void endVisit(final SwitchStatement tree, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final TryStatement tree, final BlockScope scope) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
						boolean hasFinally = tree.finallyBlock != null;
						boolean hasCatch = !list(tree.catchArguments).isEmpty();
						ErrorHandler catchHandler = null;
						ErrorHandler finallyHandler = null;
						lombok.ast.Case tryLabel = Case();
						lombok.ast.Case finallyLabel;
						lombok.ast.Case breakLabel = getBreakLabel(this);
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
						refactorStatement(tree.tryBlock);
						addStatement(setState(literal(finallyLabel)));
						
						if (hasCatch) {
							addStatement(Continue());
							catchHandler.end = cases.size();
							int numberOfCatchBlocks = tree.catchArguments.length;
							for(int i = 0; i < numberOfCatchBlocks; i++) {
								Argument argument = tree.catchArguments[i];
								Block block = tree.catchBlocks[i];
								
								lombok.ast.Case label = Case();
								usedLabels.add(label);
								addLabel(label);
								refactorStatement(block);
								addStatement(setState(literal(finallyLabel)));
								addStatement(Continue());
								
								catchHandler.statements.add(If(InstanceOf(Name(errorName), Type(argument.type))).Then(Block() //
									.withStatement(Assign(Name(string(argument.name)), Cast(Type(argument.type), Name(errorName)))) //
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
							refactorStatement(tree.finallyBlock);
							
							addStatement(If(NotEqual(Name(finallyErrorName), Null())).Then(Block().withStatement(Assign(Name(errorName), Name(finallyErrorName))).withStatement(Break())));
							
							Scope next = getFinallyScope(parent, null);
							if (next != null) {
								lombok.ast.Case label = getFinallyLabel(next);
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
				return super.visit(tree, scope);
			}

			@Override
			public void endVisit(final TryStatement tree, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final LocalDeclaration tree, final BlockScope scope) {
				variableDecls.add(new Scope(current, tree) {
					@Override
					public void refactor() {
						if (tree.initialization != null) {
							if (tree.initialization instanceof ArrayInitializer) {
								ArrayInitializer initializer = (ArrayInitializer) tree.initialization;
								ArrayAllocationExpression allocation = new ArrayAllocationExpression();
								allocation.type = method.build(Type(tree.type.toString()));
								allocation.initializer = initializer;
								allocation.dimensions = new Expression[tree.type.dimensions()];
								addStatement(Assign(Name(string(tree.name)), Expr(allocation)));
							} else {
								addStatement(Assign(Name(string(tree.name)), Expr(tree.initialization)));
							}
						}
					}
				});

				return super.visit(tree, scope);
			}

			@Override
			public boolean visit(final Argument tree, final BlockScope scope) {
				current = new Scope(current, tree) {
					@Override
					public void refactor() {
					}
				};
				if (!(current.parent.node instanceof MethodDeclaration)) {
					variableDecls.add(current);
				}

				return super.visit(tree, scope);
			}

			@Override
			public void endVisit(final Argument tree, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final ReturnStatement tree, final BlockScope scope) {
				method.node().addError("The 'return' expression is permitted.");
				return false;
			}

			@Override
			public void endVisit(final ReturnStatement tree, final BlockScope scope) {
			}

			@Override
			public boolean visit(final BreakStatement tree, final BlockScope scope) {
				Scope target = null;
				char[] label = tree.label;
				if (label != null) {
					Scope labelScope = current;
					while (labelScope != null) {
						if (labelScope.node instanceof LabeledStatement) {
							LabeledStatement labeledStatement = (LabeledStatement) labelScope.node;
							if (Arrays.equals(label, labeledStatement.label)) {
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
						if (isOneOf(labelScope.node, ForStatement.class, ForeachStatement.class, WhileStatement.class, DoStatement.class, SwitchStatement.class)) {
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
						lombok.ast.Case label = getBreakLabel(target);
						
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
				return false;
			}

			@Override
			public void endVisit(final BreakStatement tree, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final ContinueStatement tree, final BlockScope scope) {
				Scope target = null;
				char[] label = tree.label;
				if (label != null) {
					Scope labelScope = current;
					while (labelScope != null) {
						if (labelScope.node instanceof LabeledStatement) {
							LabeledStatement labeledStatement = (LabeledStatement) labelScope.node;
							if (label == labeledStatement.label) {
								if (target != null) {
									method.node().addError("Invalid label.");
								}
								if (isOneOf(labelScope.node, ForStatement.class, ForeachStatement.class, WhileStatement.class, DoStatement.class)) {
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
						if (isOneOf(labelScope.node, ForStatement.class, ForeachStatement.class, WhileStatement.class, DoStatement.class)) {
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
						lombok.ast.Case label = getIterationLabel(target);
						
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
				return false;
			}

			@Override
			public void endVisit(final ContinueStatement tree, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final ThisReference tree, final BlockScope scope) {
				if (!tree.isImplicitThis()) {
					method.node().addError("No unqualified 'this' expression is permitted.");
				}
				return false;
			}

			@Override
			public boolean visit(final SuperReference tree, final BlockScope scope) {
				method.node().addError("No unqualified 'super' expression is permitted.");
				return false;
			}

			@Override
			public boolean visit(final SingleNameReference tree, final BlockScope scope) {
				names.add(new String(tree.token));
				return super.visit(tree, scope);
			}

			@Override
			public boolean visit(final MessageSend tree, final BlockScope scope) {
				final Expression expression = getYieldExpression(tree);
				if (expression != null) {
					current = new Scope(current, tree) {
						@Override
						public void refactor() {
							lombok.ast.Case label = getBreakLabel(this);
							addStatement(Assign(Name(nextName), Expr(expression)));
							addStatement(setState(literal(label)));
							addStatement(Return(True()));
							addLabel(label);
						}
					};
					yields.add(current);
					expression.traverse(this, scope);
					current = current.parent;
					return false;
				} else {
					if (tree.receiver.isImplicitThis()) {
						String name = string(tree.selector);
						if (isOneOf(name, "hasNext", "next", "remove")) {
							method.node().addError(String.format("Cannot call method %s(), as it is hidden.", name));
						}
					}
					return super.visit(tree, scope);
				}
			}
		}
	}

	private static class ErrorHandler {
		public int begin;
		public int end;
		public List<lombok.ast.Statement> statements = new ArrayList<lombok.ast.Statement>();
	}

	@RequiredArgsConstructor
	private abstract static class Scope {
		public final Scope parent;
		public final ASTNode node;
		public Scope target;
		public String labelName;
		public lombok.ast.Case iterationLabel;
		public lombok.ast.Case breakLabel;
		public lombok.ast.Case finallyLabel;

		public abstract void refactor();
	}
}
