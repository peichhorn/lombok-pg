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
package lombok.eclipse.handlers;

import static lombok.eclipse.handlers.Eclipse.*;
import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.camelCase;
import static lombok.core.util.Types.*;

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
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.SuperReference;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.mangosdk.spi.ProviderFor;

import lombok.*;
import lombok.ast.*;
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
			method.node().addError("Method that contain yield() can only return java.util.Iterator or java.lang.Iterable");
			return true;
		}
		if (method.hasNonFinalArgument()) {
			method.node().addError("Parameters should be final.");
			return true;
		}


		YieldDataCollector collector = new YieldDataCollector();
		collector.collect(method, "$state", "$next");

		if (!collector.hasYields()) {
			return true;
		}

		final String yielderName = yielderName(method.node());
		final String elementType = elementType(method.node());

		lombok.ast.Statement switchStatement = Stat(collector.getStateSwitch());
		List<FieldDecl> variables = collector.getStateVariables();

		ClassDecl yielder = ClassDecl(yielderName).makeLocal().implementing(Type("java.util.Iterator").withTypeArgument(Type(elementType))) //
			.withFields(variables) //
			.withField(FieldDecl(Type("int"), "$state").makePrivate()) //
			.withField(FieldDecl(Type("boolean"), "$hasNext").makePrivate()) //
			.withField(FieldDecl(Type("boolean"), "$nextDefined").makePrivate()) //
			.withField(FieldDecl(Type(elementType), "$next").makePrivate()) //
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
					.withStatement(Return(Name("$next")))) //
			.withMethod(MethodDecl(Type("void"), "remove").makePublic().withStatement(Throw(New(Type("java.lang.UnsupportedOperationException"))))) //
			.withMethod(MethodDecl(Type("boolean"), "getNext").makePrivate().withStatement(While(True()).Do(switchStatement)));

		method.body(yielder, Return(New(Type(yielderName))));
		method.rebuild();

		return true;
	}

	private String yielderName(final EclipseNode methodNode) {
		String[] parts = methodNode.getName().split("_");
		String[] newParts = new String[parts.length + 1];
		newParts[0] = "yielder";
		System.arraycopy(parts, 0, newParts, 1, parts.length);
		return camelCase("$", newParts);
	}

	private String elementType(final EclipseNode methodNode) {
		MethodDeclaration methodDecl = (MethodDeclaration)methodNode.get();
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
		private MethodDeclaration declaration;
		private Set<String> names = new HashSet<String>();
		private List<Scope> yields = new ArrayList<Scope>();
		private List<Scope> breaks = new ArrayList<Scope>();
		private List<Scope> variableDecls = new ArrayList<Scope>();
		@Getter
		private List<FieldDecl> stateVariables = new ArrayList<FieldDecl>();
		private Scope root;
		private Map<ASTNode, Scope> allScopes = new HashMap<ASTNode, Scope>();
		private List<Statement> statements = new ArrayList<Statement>();
		private Map<Expression, Label> labelLiterals = new HashMap<Expression, Label>();
		private Set<Label> usedLabels = new HashSet<Label>();
		private String stateName;
		private String nextName;

		public SwitchStatement getStateSwitch() {
			List<Statement> switchStatements = new ArrayList<Statement>(statements);
			switchStatements.add(new CaseStatement(null, 0, 0));
			switchStatements.add(method.build(Return(False()), Statement.class));
			SwitchStatement switchStatement = new SwitchStatement();
			switchStatement.expression = method.build(Name(stateName));
			switchStatement.statements = switchStatements.toArray(new Statement[switchStatements.size()]);
			return switchStatement;
		}

		public boolean hasYields() {
			return !yields.isEmpty();
		}

		public void collect(final EclipseMethod eclipseMethod, final String state, final String next) {
			this.method = eclipseMethod;
			this.declaration = (MethodDeclaration)eclipseMethod.get();
			stateName = state;
			nextName = next;
			try {
				if (scan()) {
					refactor();
				}
			} catch (final Exception ignore) {
			}
		}

		private boolean scan() {
			try {
				declaration.traverse(new YieldQuickScanner(), (ClassScope)null);
				return false;
			} catch (final IllegalStateException ignore) {
				// this means there are unhandled yields left
			}

			ValidationScanner scanner = new ValidationScanner();
			declaration.traverse(scanner, (ClassScope)null);

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
				}
				if (stateVariable) {
					LocalDeclaration variable = (LocalDeclaration) scope.node;
					allScopes.put(scope.node, scope);
					stateVariables.add(FieldDecl(Type(variable.type), new String(variable.name)).makePrivate());
				}
			}

			return true;
		}

		private void refactor() {
			root = allScopes.get(declaration);
			Label iteratorLabel = getIterationLabel(root);

			usedLabels.add(iteratorLabel);
			usedLabels.add(getBreakLabel(root));

			addStatement(iteratorLabel);
			root.refactor();
			optimizeStates();
			synchronizeLiteralsAndLabels();
		}

		private Expression getYieldExpression(final MessageSend invoke) {
			if ("yield".equals(new String(invoke.selector)) && (invoke.arguments != null) && (invoke.arguments.length == 1)) {
				return invoke.arguments[0];
			}
			return null;
		}

		private Label label() {
			return new Label();
		}

		private void addStatement(final Label label) {
			if (label != null) {
				label.id = statements.size();
				statements.add(label);
			}
		}

		private void addStatement(final lombok.ast.Statement statement) {
			statements.add(method.build(statement, Statement.class));
		}

		private Expression labelLiteral(final Label label) {
			Expression literal = new UnaryExpression(label.constantExpression != null ? label.constantExpression : method.build(Number(Integer.valueOf(-1)), Expression.class), OperatorIds.PLUS);
			labelLiterals.put(literal, label);
			return literal;
		}

		public Label getBreakLabel(final Scope scope) {
			Label label = scope.breakLabel;
			if (label == null) {
				label = label();
				scope.breakLabel = label;
			}
			return label;
		}

		public Label getIterationLabel(final Scope scope) {
			Label label = scope.iterationLabel;
			if (label == null) {
				label = label();
				scope.iterationLabel = label;
			}
			return label;
		}

		private lombok.ast.Statement setStateId(final Expression expression) {
			return Assign(Name(stateName), Expr(expression));
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
			int id = 0;
			int labelCounter = 0;
			Statement previous = null;
			for (int i = 0; i < statements.size(); i++) {
				Statement statement = statements.get(i);
				if (statement instanceof Label) {
					labelCounter++;
					Label label = (Label) statement;
					if ((previous != null) && (labelCounter > 2) && isNoneOf(previous, ContinueStatement.class, ReturnStatement.class)) {
						id--;
						statements.remove(i--);
					}
					label.id = id++;
					label.constantExpression = method.build(Number(Integer.valueOf(label.id)));
				}
				previous = statement;
			}
		}

		private void synchronizeLiteralsAndLabels() {
			for (Map.Entry<Expression, Label> entry : labelLiterals.entrySet()) {
				UnaryExpression expression = (UnaryExpression) entry.getKey();
				Label label = entry.getValue();
				expression.expression = method.build(Number(Integer.valueOf(label.id)));
			}
		}

		private class YieldQuickScanner extends ASTVisitor {
			@Override
			public boolean visit(final MessageSend messageSend, final BlockScope scope) {
				final Expression expression = getYieldExpression(messageSend);
				if (expression != null) {
					throw new IllegalStateException();
				} else {
					return super.visit(messageSend, scope);
				}
			}
		}

		private class ValidationScanner extends ASTVisitor {
			private Scope current;

			@Override
			public boolean visit(final MethodDeclaration methodDeclaration, final ClassScope scope) {
				current = new Scope(current, methodDeclaration) {
					@Override
					public void refactor() {
						if (methodDeclaration.statements != null) {
							for (Statement statement : methodDeclaration.statements) {
								refactorStatement(statement);
							}
						}
						addStatement(getBreakLabel(this));
					}
				};

				return super.visit(methodDeclaration, scope);
			}

			@Override
			public void endVisit(final MethodDeclaration methodDeclaration, final ClassScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final Block block, final BlockScope scope) {
				current = new Scope(current, block) {
					@Override
					public void refactor() {
						if (block.statements != null) {
							for (Statement statement : block.statements) {
								refactorStatement(statement);
							}
						}
						addStatement(getBreakLabel(this));
					}
				};

				return super.visit(block, scope);
			}

			@Override
			public void endVisit(final Block block, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final LabeledStatement labeledStatement, final BlockScope scope) {
				current = new Scope(current, labeledStatement) {
					@Override
					public void refactor() {
						refactorStatement(labeledStatement.statement);
					}
				};

				return super.visit(labeledStatement, scope);
			}

			@Override
			public void endVisit(final LabeledStatement labeledStatement, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final ForStatement forStatement, final BlockScope scope) {
				current = new Scope(current, forStatement) {
					@Override
					public void refactor() {
						if (forStatement.initializations != null) {
							for (Statement statement : forStatement.initializations) {
								refactorStatement(statement);
							}
						}
						Label label = label();
						Label breakLabel = getBreakLabel(this);
						addStatement(label);
						if ((forStatement.condition != null) && !(forStatement.condition instanceof TrueLiteral)) {
							addStatement(If(Not(Expr(forStatement.condition))).Then(Block() //
								.withStatement(setStateId(labelLiteral(breakLabel))) //
								.withStatement(Continue()) //
							));
						}
						refactorStatement(forStatement.action);
						addStatement(getIterationLabel(this));
						if (forStatement.increments != null) {
							for (Statement statement : forStatement.increments) {
								refactorStatement(statement);
							}
						}
						addStatement(setStateId(labelLiteral(label)));
						addStatement(Continue());
						addStatement(breakLabel);
					}
				};

				return super.visit(forStatement, scope);
			}

			@Override
			public void endVisit(final ForStatement forStatement, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final ForeachStatement forStatement, final BlockScope scope) {
				current = new Scope(current, forStatement) {
					@Override
					public void refactor() {
						String iteratorVarName = "$" + new String(forStatement.elementVariable.name) + "Iter";
						stateVariables.add(FieldDecl(Type("java.util.Iterator"), iteratorVarName).makePrivate());
						addStatement(Assign(Name(iteratorVarName), Call(Expr(forStatement.collection), "iterator")));
						addStatement(getIterationLabel(this));
						addStatement(If(Not(Call(Name(iteratorVarName), "hasNext"))).Then(Block() //
							.withStatement(setStateId(labelLiteral(getBreakLabel(this)))) //
							.withStatement(Continue()) //
						));
						addStatement(Assign(Name(new String(forStatement.elementVariable.name)), Cast(Type(forStatement.elementVariable.type), Call(Name(iteratorVarName), "next"))));
						refactorStatement(forStatement.action);
						addStatement(setStateId(labelLiteral(getIterationLabel(this))));
						addStatement(Continue());
						addStatement(getBreakLabel(this));
					}
				};

				return super.visit(forStatement, scope);
			}

			@Override
			public void endVisit(final ForeachStatement forStatement, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final DoStatement doStatement, final BlockScope scope) {
				current = new Scope(current, doStatement) {
					@Override
					public void refactor() {
						addStatement(getIterationLabel(this));
						refactorStatement(doStatement.action);
						addStatement(If(Expr(doStatement.condition)).Then(Block() //
							.withStatement(setStateId(labelLiteral(breakLabel))) //
							.withStatement(Continue()) //
						));
						addStatement(getBreakLabel(this));
					}
				};

				return super.visit(doStatement, scope);
			}

			@Override
			public void endVisit(final DoStatement doStatement, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final WhileStatement whileStatement, final BlockScope scope) {
				current = new Scope(current, whileStatement) {
					@Override
					public void refactor() {
						addStatement(getIterationLabel(this));
						if (!(whileStatement.condition instanceof TrueLiteral)) {
							addStatement(If(Not(Expr(whileStatement.condition))).Then(Block() //
								.withStatement(setStateId(labelLiteral(getBreakLabel(this)))) //
								.withStatement(Continue()) //
							));
						}
						refactorStatement(whileStatement.action);
						addStatement(setStateId(labelLiteral(getIterationLabel(this))));
						addStatement(Continue());
						addStatement(getBreakLabel(this));
					}
				};

				return super.visit(whileStatement, scope);
			}

			@Override
			public void endVisit(final WhileStatement whileStatement, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final IfStatement ifStatement, final BlockScope scope) {
				current = new Scope(current, ifStatement) {
					@Override
					public void refactor() {
						Label label = ifStatement.elseStatement == null ? getBreakLabel(this) : label();
						addStatement(If(Not(Expr(ifStatement.condition))).Then(Block() //
							.withStatement(setStateId(labelLiteral(label))) //
							.withStatement(Continue()) //
						));
						if (ifStatement.elseStatement != null) {
							refactorStatement(ifStatement.thenStatement);
							addStatement(setStateId(labelLiteral(getBreakLabel(this))));
							addStatement(Continue());
							addStatement(label);
							refactorStatement(ifStatement.elseStatement);
							addStatement(getBreakLabel(this));
						} else {
							refactorStatement(ifStatement.thenStatement);
							addStatement(getBreakLabel(this));
						}
					}
				};

				return super.visit(ifStatement, scope);
			}

			@Override
			public void endVisit(final IfStatement ifStatement, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final SwitchStatement switchStatement, final BlockScope scope) {
				current = new Scope(current, switchStatement) {
					@Override
					public void refactor() {
						Label breakLabel = getBreakLabel(this);
						SwitchStatement newSwitchStatement = new SwitchStatement();
						newSwitchStatement.expression = switchStatement.expression;
						addStatement(Stat(newSwitchStatement));
						if (switchStatement.statements != null) {
							boolean hasDefault = false;
							ArrayList<Statement> cases = new ArrayList<Statement>();
							for (Statement statement : switchStatement.statements) {
								if (statement instanceof CaseStatement) {
									CaseStatement caseStatement = (CaseStatement) statement;
									if (caseStatement.constantExpression == null) {
										hasDefault = true;
									}
									Label label = label();
									cases.add(caseStatement);
									cases.add(method.build(setStateId(labelLiteral(label)), Statement.class));
									cases.add(new ContinueStatement(null, 0, 0));
									addStatement(label);
								} else {
									refactorStatement(statement);
								}
							}
							if (!hasDefault) {
								cases.add(new CaseStatement(null, 0, 0));
								cases.add(method.build(setStateId(labelLiteral(breakLabel)), Statement.class));
								cases.add(new ContinueStatement(null, 0, 0));
							}
							newSwitchStatement.statements = cases.toArray(new Statement[cases.size()]);
						}
						addStatement(breakLabel);
					}
				};

				return super.visit(switchStatement, scope);
			}

			@Override
			public void endVisit(final SwitchStatement switchStatement, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final LocalDeclaration localDeclaration, final BlockScope scope) {
				variableDecls.add(new Scope(current, localDeclaration) {
					@Override
					public void refactor() {
						if (localDeclaration.initialization != null) {
							if (localDeclaration.initialization instanceof ArrayInitializer) {
								ArrayInitializer initializer = (ArrayInitializer) localDeclaration.initialization;
								ArrayAllocationExpression allocation = new ArrayAllocationExpression();
								allocation.type = method.build(Type(localDeclaration.type.toString()));
								allocation.initializer = initializer;
								allocation.dimensions = new Expression[localDeclaration.type.dimensions()];
								addStatement(Assign(Name(new String(localDeclaration.name)), Expr(allocation)));
							} else {
								addStatement(Assign(Name(new String(localDeclaration.name)), Expr(localDeclaration.initialization)));
							}
						}
					}
				});

				return super.visit(localDeclaration, scope);
			}

			@Override
			public boolean visit(final Argument argument, final BlockScope scope) {
				current = new Scope(current, argument) {
					@Override
					public void refactor() {
					}
				};
				if (!(current.parent.node instanceof MethodDeclaration)) {
					variableDecls.add(current);
				}

				return super.visit(argument, scope);
			}

			@Override
			public void endVisit(final Argument argument, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final ReturnStatement returnStatement, final BlockScope scope) {
				method.node().addError("The 'return' expression is permitted.");
				return false;
			}

			@Override
			public void endVisit(final ReturnStatement returnStatement, final BlockScope scope) {
			}

			@Override
			public boolean visit(final BreakStatement breakStatement, final BlockScope scope) {
				Scope target = null;
				char[] label = breakStatement.label;
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

				current = new Scope(current, breakStatement) {
					@Override
					public void refactor() {
						addStatement(setStateId(labelLiteral(getBreakLabel(target))));
						addStatement(Continue());
					}
				};

				current.target = target;
				breaks.add(current);

				return false;
			}

			@Override
			public void endVisit(final BreakStatement breakStatement, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final ContinueStatement continueStatement, final BlockScope scope) {
				Scope target = null;
				char[] label = continueStatement.label;
				if (label != null) {
					Scope labelScope = current;
					while (labelScope != null) {
						if (labelScope.node instanceof LabeledStatement) {
							LabeledStatement labeledStatement = (LabeledStatement) labelScope.node;
							if (label == labeledStatement.label) {
								if (target != null) {
									method.node().addError("Invalid label.");
								}
								if (isOneOf(labelScope, ForStatement.class, ForeachStatement.class, WhileStatement.class, DoStatement.class)) {
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
						if (isOneOf(labelScope, ForStatement.class, ForeachStatement.class, WhileStatement.class, DoStatement.class)) {
							target = labelScope;
							break;
						}
						labelScope = labelScope.parent;
					}
				}

				if (target == null) {
					method.node().addError("Invalid continue.");
				}

				current = new Scope(current, continueStatement) {
					@Override
					public void refactor() {
						addStatement(setStateId(labelLiteral(getIterationLabel(target))));
						addStatement(Continue());
					}
				};

				current.target = target;
				breaks.add(current);
				return false;
			}

			@Override
			public void endVisit(final ContinueStatement continueStatement, final BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final ThisReference thisReference, final BlockScope scope) {
				if (!thisReference.isImplicitThis()) {
					method.node().addError("No unqualified 'this' expression is permitted.");
				}
				return false;
			}

			@Override
			public boolean visit(final SuperReference thisReference, final BlockScope scope) {
				method.node().addError("No unqualified 'super' expression is permitted.");
				return false;
			}

			@Override
			public boolean visit(final SingleNameReference singleNameReference, final BlockScope scope) {
				names.add(new String(singleNameReference.token));
				return super.visit(singleNameReference, scope);
			}

			@Override
			public boolean visit(final MessageSend messageSend, final BlockScope scope) {
				final Expression expression = getYieldExpression(messageSend);
				if (expression != null) {
					yields.add(new Scope(current, messageSend) {
						@Override
						public void refactor() {
							Label label = getBreakLabel(this);
							addStatement(Assign(Name(nextName), Expr(expression)));
							addStatement(setStateId(labelLiteral(label)));
							addStatement(Return(True()));
							addStatement(label);
						}
					});
					expression.traverse(this, scope);
					return false;
				} else {
					if (messageSend.receiver.isImplicitThis()) {
						String name = new String(messageSend.selector);
						if ("hasNext".equals(name) || "next".equals(name) || "remove".equals(name)) {
							method.node().addError("Cannot call method " + name + "(), as it is hidden.");
						}
					}
					return super.visit(messageSend, scope);
				}
			}
		}
	}

	private static class Label extends CaseStatement {
		public int id = -1;

		public Label() {
			super(null, 0, 0);
		}
	}

	private abstract static class Scope {
		public ASTNode node;
		public Scope parent;
		public Scope target;
		public Label iterationLabel;
		public Label breakLabel;

		public Scope(final Scope parent, final ASTNode node) {
			this.parent = parent;
			this.node = node;
		}

		public abstract void refactor();
	}
}