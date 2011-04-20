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
import static lombok.eclipse.handlers.ast.ASTBuilder.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.camelCase;
import static lombok.core.util.Types.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
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
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.mangosdk.spi.ProviderFor;

import lombok.Yield;
import lombok.eclipse.EclipseASTAdapter;
import lombok.eclipse.EclipseASTVisitor;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.ASTNodeBuilder;
import lombok.eclipse.handlers.ast.ASTNodeWrapper;
import lombok.eclipse.handlers.ast.ExpressionWrapper;
import lombok.eclipse.handlers.ast.StatementBuilder;
import lombok.eclipse.handlers.ast.TypeDeclarationBuilder;

@ProviderFor(EclipseASTVisitor.class)
public class HandleYield extends EclipseASTAdapter {
	private final Set<String> methodNames = new HashSet<String>();

	@Override public void visitCompilationUnit(EclipseNode top, CompilationUnitDeclaration unit) {
		methodNames.clear();
	}

	@Override public void visitStatement(EclipseNode statementNode, Statement statement) {
		if (statement instanceof MessageSend) {
			MessageSend methodCall = (MessageSend) statement;
			String methodName = (methodCall.receiver instanceof ThisReference) ? "" : methodCall.receiver + ".";
			methodName += new String(methodCall.selector);
			if (isMethodCallValid(statementNode, methodName, Yield.class, "yield")) {
				final EclipseMethod method = EclipseMethod.methodOf(statementNode);
				if ((method == null) || method.isConstructor()) {
					method.node().addError(canBeUsedInBodyOfMethodsOnly("yield"));
				} else if (handle(method)) {
					methodNames.add(methodName);
				}
			}
		}
	}

	@Override public void endVisitCompilationUnit(EclipseNode top, CompilationUnitDeclaration unit) {
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
		if (method.hasNonFinalParameter()) {
			method.node().addError("Parameters should be final.");
			return true;
		}


		YieldDataCollector collector = new YieldDataCollector();
		collector.collect(method.node(), "$state", "$next");

		if (!collector.hasYields()) {
			return true;
		}

		ASTNode source = method.get();

		final String yielderName = yielderName(method.node());
		final String elementType = elementType(method.node());

		List<ASTNodeBuilder<? extends TypeDeclaration>> classes = collector.getClasses();
		SwitchStatement switchStatement = collector.getStateSwitch();
		List<StatementBuilder<? extends FieldDeclaration>> variables = collector.getVariables();
		
		TypeDeclarationBuilder builder = ClassDef(yielderName).makeLocal().implementing(Type("java.util.Iterator").withTypeArgument(Type(elementType))) //
			.withFields(variables) //
			.withField(FieldDef(Type("int"), "$state").makePrivate()) //
			.withField(FieldDef(Type("boolean"), "$hasNext").makePrivate()) //
			.withField(FieldDef(Type("boolean"), "$nextDefined").makePrivate()) //
			.withField(FieldDef(Type(elementType), "$next").makePrivate()) //
			.withMethod(ConstructorDef(yielderName).withImplicitSuper().makePrivate()); //
		if (returnsIterable) {
			builder.implementing(Type("java.lang.Iterable").withTypeArgument(Type(elementType))) //
				.withMethod(MethodDef(Type("java.util.Iterator").withTypeArgument(Type(elementType)), "iterator").makePublic().withStatement(Return(New(Type(yielderName)))));
		}
		TypeDeclaration yielder = builder //
			.withMethod(MethodDef(Type("boolean"), "hasNext").makePublic() //
					.withStatement(If(Not(Name("$nextDefined"))).Then(Block() //
						.withStatement(Assign(Name("$hasNext"), Call("getNext"))) //
						.withStatement(Assign(Name("$nextDefined"), True())))) //
					.withStatement(Return(Name("$hasNext")))) //
			.withMethod(MethodDef(Type(elementType), "next").makePublic() //
					.withStatement(If(Not(Call("hasNext"))).Then(Block().withStatement(Throw(New(Type("java.util.NoSuchElementException")))))) //
					.withStatement(Assign(Name("$nextDefined"), False())) //
					.withStatement(Return(Name("$next")))) //
			.withMethod(MethodDef(Type("void"), "remove").makePublic().withStatement(Throw(New(Type("java.lang.UnsupportedOperationException"))))) //
			.withMethod(MethodDef(Type("boolean"), "getNext").makePrivate().withStatement(While(True()).Do(switchStatement))) //
			.withTypes(classes) //
			.build(method.node(), source);

		method.body(source, yielder, Return(New(Type(yielderName))).build(method.node(), source));
		method.rebuild();

		return true;
	}

	private String yielderName(EclipseNode methodNode) {
		String[] parts = methodNode.getName().split("_");
		String[] newParts = new String[parts.length + 1];
		newParts[0] = "yielder";
		System.arraycopy(parts, 0, newParts, 1, parts.length);
		return camelCase("$", newParts);
	}

	private String elementType(EclipseNode methodNode) {
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
		private EclipseNode methodNode;
		private MethodDeclaration declaration;
		private Set<String> names = new HashSet<String>();
		private List<ASTNodeBuilder<? extends TypeDeclaration>> classes = new ArrayList<ASTNodeBuilder<? extends TypeDeclaration>>();
		private List<Scope> yields = new ArrayList<Scope>();
		private List<Scope> breaks = new ArrayList<Scope>();
		private List<Scope> variableDecls = new ArrayList<Scope>();
		private List<StatementBuilder<? extends FieldDeclaration>> stateVariables = new ArrayList<StatementBuilder<? extends FieldDeclaration>>();
		private Scope root;
		private Map<ASTNode, Scope> allScopes = new HashMap<ASTNode, Scope>();
		private List<Statement> statements = new ArrayList<Statement>();
		private Map<Expression, Label> labelLiterals = new HashMap<Expression, Label>();
		private Set<Label> usedLabels = new HashSet<Label>();
		private String stateName;
		private String nextName;

		public List<StatementBuilder<? extends FieldDeclaration>> getVariables() {
			return stateVariables;
		}

		public List<ASTNodeBuilder<? extends TypeDeclaration>> getClasses() {
			return classes;
		}

		public SwitchStatement getStateSwitch() {
			List<Statement> switchStatements = new ArrayList<Statement>(statements);
			switchStatements.add(new CaseStatement(null, 0, 0));
			switchStatements.add(Return(False()).build(methodNode, declaration));
			SwitchStatement switchStatement = new SwitchStatement();
			switchStatement.expression = Name(stateName).build(methodNode, declaration);
			switchStatement.statements = switchStatements.toArray(new Statement[switchStatements.size()]);
			return switchStatement;
		}

		public boolean hasYields() {
			return !yields.isEmpty();
		}

		public void collect(final EclipseNode methodNode, final String state, final String next) {
			this.methodNode = methodNode;
			this.declaration = (MethodDeclaration)methodNode.get();
			stateName = state;
			nextName = next;
			try {
				if (scan()) {
					refactor();
				}
			} catch (Exception ignore) {
			}
		}

		private boolean scan() {
			try {
				declaration.traverse(new YieldQuickScanner(), (ClassScope)null);
				return false;
			} catch (IllegalStateException ignore) {
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
					stateVariables.add(FieldDef(Type(variable.type), new String(variable.name)).makePrivate());
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

		private Expression getYieldExpression(MessageSend invoke) {
			if ("yield".equals(new String(invoke.selector)) && (invoke.arguments != null) && (invoke.arguments.length == 1)) {
				return invoke.arguments[0];
			}
			return null;
		}

		private Label label() {
			return new Label();
		}

		private void addStatement(Label label) {
			if (label != null) {
				label.id = statements.size();
				statements.add(label);
			}
		}

		private void addStatement(Statement statement) {
			if (statement != null) {
				statements.add(statement);
			}
		}

		private Expression labelLiteral(Label label) {
			Expression literal = new UnaryExpression(label.constantExpression != null ? label.constantExpression : Number(Integer.valueOf(-1)).build(methodNode, declaration), OperatorIds.PLUS);
			labelLiterals.put(literal, label);
			return literal;
		}

		public Label getBreakLabel(Scope scope) {
			Label label = scope.breakLabel;
			if (label == null) {
				label = label();
				scope.breakLabel = label;
			}
			return label;
		}

		public Label getIterationLabel(Scope scope) {
			Label label = scope.iterationLabel;
			if (label == null) {
				label = label();
				scope.iterationLabel = label;
			}
			return label;
		}

		private StatementBuilder<? extends Statement> setStateId(Expression expression) {
			return Assign(Name(stateName), new ExpressionWrapper<Expression>(expression));
		}

		private void refactorStatement(Statement statement) {
			if (statement == null) {
				return;
			}
			Scope scope = allScopes.get(statement);
			if (scope != null) {
				scope.refactor();
			} else {
				addStatement(statement);
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
					label.constantExpression = Number(Integer.valueOf(label.id)).build(methodNode, declaration);
				}
				previous = statement;
			}
		}

		private void synchronizeLiteralsAndLabels() {
			for (Map.Entry<Expression, Label> entry : labelLiterals.entrySet()) {
				UnaryExpression expression = (UnaryExpression) entry.getKey();
				Label label = entry.getValue();
				expression.expression = Number(Integer.valueOf(label.id)).build(methodNode, declaration);
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
			public void endVisit(MethodDeclaration methodDeclaration, ClassScope scope) {
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
			public void endVisit(Block block, BlockScope scope) {
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
			public void endVisit(LabeledStatement labeledStatement, BlockScope scope) {
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
							addStatement(If(Not(new ExpressionWrapper<Expression>(forStatement.condition))).Then(Block() //
								.withStatement(setStateId(labelLiteral(breakLabel))) //
								.withStatement(Continue()) //
							).build(methodNode, declaration));
						}
						refactorStatement(forStatement.action);
						addStatement(getIterationLabel(this));
						if (forStatement.increments != null) {
							for (Statement statement : forStatement.increments) {
								refactorStatement(statement);
							}
						}
						addStatement(setStateId(labelLiteral(label)).build(methodNode, declaration));
						addStatement(Continue().build(methodNode, declaration));
						addStatement(breakLabel);
					}
				};

				return super.visit(forStatement, scope);
			}

			@Override
			public void endVisit(ForStatement forStatement, BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final ForeachStatement forStatement, final BlockScope scope) {
				current = new Scope(current, forStatement) {
					@Override
					public void refactor() {
						String iteratorVarName = "$" + new String(forStatement.elementVariable.name) + "Iter";
						stateVariables.add(FieldDef(Type("java.util.Iterator"), iteratorVarName).makePrivate());
						addStatement(Assign(Name(iteratorVarName), Call(new ExpressionWrapper<Expression>(forStatement.collection), "iterator")).build(methodNode, declaration));
						addStatement(getIterationLabel(this));
						addStatement(If(Not(Call(Name(iteratorVarName), "hasNext"))).Then(Block() //
							.withStatement(setStateId(labelLiteral(getBreakLabel(this)))) //
							.withStatement(Continue()) //
						).build(methodNode, declaration));
						addStatement(Assign(Name(new String(forStatement.elementVariable.name)), Cast(Type(forStatement.elementVariable.type), Call(Name(iteratorVarName), "next"))).build(methodNode, declaration));
						refactorStatement(forStatement.action);
						addStatement(setStateId(labelLiteral(getIterationLabel(this))).build(methodNode, declaration));
						addStatement(Continue().build(methodNode, declaration));
						addStatement(getBreakLabel(this));
					}
				};

				return super.visit(forStatement, scope);
			}

			@Override
			public void endVisit(ForeachStatement forStatement, BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final DoStatement doStatement, final BlockScope scope) {
				current = new Scope(current, doStatement) {
					@Override
					public void refactor() {
						addStatement(getIterationLabel(this));
						refactorStatement(doStatement.action);
						addStatement(If(new ExpressionWrapper<Expression>(doStatement.condition)).Then(Block() //
							.withStatement(setStateId(labelLiteral(breakLabel))) //
							.withStatement(Continue()) //
						).build(methodNode, declaration));
						addStatement(getBreakLabel(this));
					}
				};

				return super.visit(doStatement, scope);
			}

			@Override
			public void endVisit(DoStatement doStatement, BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final WhileStatement whileStatement, final BlockScope scope) {
				current = new Scope(current, whileStatement) {
					@Override
					public void refactor() {
						addStatement(getIterationLabel(this));
						if (!(whileStatement.condition instanceof TrueLiteral)) {
							addStatement(If(Not(new ExpressionWrapper<Expression>(whileStatement.condition))).Then(Block() //
								.withStatement(setStateId(labelLiteral(getBreakLabel(this)))) //
								.withStatement(Continue()) //
							).build(methodNode, declaration));
						}
						refactorStatement(whileStatement.action);
						addStatement(setStateId(labelLiteral(getIterationLabel(this))).build(methodNode, declaration));
						addStatement(new ContinueStatement(null, 0, 0));
						addStatement(getBreakLabel(this));
					}
				};

				return super.visit(whileStatement, scope);
			}

			@Override
			public void endVisit(WhileStatement whileStatement, BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final IfStatement ifStatement, final BlockScope scope) {
				current = new Scope(current, ifStatement) {
					@Override
					public void refactor() {
						Label label = ifStatement.elseStatement == null ? getBreakLabel(this) : label();
						addStatement(If(Not(new ExpressionWrapper<Expression>(ifStatement.condition))).Then(Block() //
							.withStatement(setStateId(labelLiteral(label))) //
							.withStatement(Continue()) //
						).build(methodNode, declaration));
						if (ifStatement.elseStatement != null) {
							refactorStatement(ifStatement.thenStatement);
							addStatement(setStateId(labelLiteral(getBreakLabel(this))).build(methodNode, declaration));
							addStatement(new ContinueStatement(null, 0, 0));
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
			public void endVisit(IfStatement ifStatement, BlockScope scope) {
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
						addStatement(newSwitchStatement);
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
									cases.add(setStateId(labelLiteral(label)).build(methodNode, declaration));
									cases.add(new ContinueStatement(null, 0, 0));
									addStatement(label);
								} else {
									refactorStatement(statement);
								}
							}
							if (!hasDefault) {
								cases.add(new CaseStatement(null, 0, 0));
								cases.add(setStateId(labelLiteral(breakLabel)).build(methodNode, declaration));
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
			public void endVisit(SwitchStatement switchStatement, BlockScope scope) {
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
								allocation.type = Type(localDeclaration.type.toString()).build(methodNode, declaration);
								allocation.initializer = initializer;
								allocation.dimensions = new Expression[localDeclaration.type.dimensions()];
								addStatement(Assign(Name(new String(localDeclaration.name)), new ExpressionWrapper<Expression>(allocation)).build(methodNode, declaration));
							} else {
								addStatement(Assign(Name(new String(localDeclaration.name)), new ExpressionWrapper<Expression>(localDeclaration.initialization)).build(methodNode, declaration));
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
			public void endVisit(Argument argument, BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(final ReturnStatement returnStatement, final BlockScope scope) {
				methodNode.addError("The 'return' expression is permitted.");
				return false;
			}

			@Override
			public void endVisit(ReturnStatement returnStatement, BlockScope scope) {
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
						if (isOneOf(labelScope.node, ForStatement.class, ForeachStatement.class, WhileStatement.class, DoStatement.class, SwitchStatement.class)) {
							target = labelScope;
							break;
						}
						labelScope = labelScope.parent;
					}
				}

				if (target == null) {
					methodNode.addError("Invalid break.");
				}

				current = new Scope(current, breakStatement) {
					@Override
					public void refactor() {
						addStatement(setStateId(labelLiteral(getBreakLabel(target))).build(methodNode, declaration));
						addStatement(new ContinueStatement(null, 0, 0));
					}
				};

				current.target = target;
				breaks.add(current);

				return false;
			}

			@Override
			public void endVisit(BreakStatement breakStatement, BlockScope scope) {
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
									methodNode.addError("Invalid label.");
								}
								if (isOneOf(labelScope, ForStatement.class, ForeachStatement.class, WhileStatement.class, DoStatement.class)) {
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
						if (isOneOf(labelScope, ForStatement.class, ForeachStatement.class, WhileStatement.class, DoStatement.class)) {
							target = labelScope;
							break;
						}
						labelScope = labelScope.parent;
					}
				}

				if (target == null) {
					methodNode.addError("Invalid continue.");
				}

				current = new Scope(current, continueStatement) {
					@Override
					public void refactor() {
						addStatement(setStateId(labelLiteral(getIterationLabel(target))).build(methodNode, declaration));
						addStatement(new ContinueStatement(null, 0, 0));
					}
				};

				current.target = target;
				breaks.add(current);
				return false;
			}

			@Override
			public void endVisit(ContinueStatement continueStatement, BlockScope scope) {
				current = current.parent;
			}

			@Override
			public boolean visit(ThisReference thisReference, BlockScope scope) {
				if (!thisReference.isImplicitThis()) {
					methodNode.addError("No unqualified 'this' expression is permitted.");
				}
				return false;
			}

			@Override
			public boolean visit(SuperReference thisReference, BlockScope scope) {
				methodNode.addError("No unqualified 'super' expression is permitted.");
				return false;
			}

			@Override
			public boolean visit(SingleNameReference singleNameReference, BlockScope scope) {
				names.add(new String(singleNameReference.token));
				return super.visit(singleNameReference, scope);
			}

			@Override
			public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
				if (localTypeDeclaration.allocation == null) {
					classes.add(new ASTNodeWrapper<TypeDeclaration>(localTypeDeclaration));
				}
				return false;
			}

			@Override
			public boolean visit(final MessageSend messageSend, final BlockScope scope) {
				final Expression expression = getYieldExpression(messageSend);
				if (expression != null) {
					yields.add(new Scope(current, messageSend) {
						@Override
						public void refactor() {
							Label label = getBreakLabel(this);
							addStatement(Assign(Name(nextName), new ExpressionWrapper<Expression>(expression)).build(methodNode, declaration));
							addStatement(setStateId(labelLiteral(label)).build(methodNode, declaration));
							addStatement(Return(True()).build(methodNode, declaration));
							addStatement(label);
						}
					});
					expression.traverse(this, scope);
					return false;
				} else {
					if (messageSend.receiver.isImplicitThis()) {
						String name = new String(messageSend.selector);
						if ("hasNext".equals(name) || "next".equals(name) || "remove".equals(name)) {
							methodNode.addError("Cannot call method " + name + "(), as it is hidden.");
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

	private static abstract class Scope {
		public ASTNode node;
		public Scope parent;
		public Scope target;
		public Label iterationLabel;
		public Label breakLabel;

		public Scope(Scope parent, ASTNode node) {
			this.parent = parent;
			this.node = node;
		}

		public abstract void refactor();
	}
}