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
import static lombok.eclipse.handlers.EclipseNodeBuilder.*;
import static lombok.core.util.ErrorMessages.*;
import static org.eclipse.jdt.core.dom.Modifier.*;
import static lombok.core.util.Names.camelCase;
import static lombok.core.util.Types.isOneOf;
import static org.eclipse.jdt.internal.compiler.ast.ASTNode.*;

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
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.BreakStatement;
import org.eclipse.jdt.internal.compiler.ast.CaseStatement;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
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

@ProviderFor(EclipseASTVisitor.class)
public class HandleYield extends EclipseASTAdapter {
	private boolean handled;
	private String methodName;
	
	@Override public void visitCompilationUnit(EclipseNode top, CompilationUnitDeclaration unit) {
		handled = false;
	}
	
	@Override public void visitStatement(EclipseNode statementNode, Statement statement) {
		if (statement instanceof MessageSend) {
			MessageSend methodCall = (MessageSend) statement;
			methodName = new String(methodCall.selector);
			if (isMethodCallValid(statementNode, methodName, Yield.class, "yield")) {
				final EclipseMethod method = EclipseMethod.methodOf(statementNode);
				if ((method == null) || method.isConstructor()) {
					method.node().addError(canBeUsedInBodyOfMethodsOnly("yield"));
				} else {
					handled = handle(method);
				}
			}
		}
	}
	
	@Override public void endVisitCompilationUnit(EclipseNode top, CompilationUnitDeclaration unit) {
		if (handled) {
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
		
		List<TypeDeclaration> classes = collector.getClasses();
		SwitchStatement switchStatement = collector.getStateSwitch();
		List<FieldDeclaration> variables = collector.getVariables();
		
		MethodDeclaration hasNext = method(method.node(), source, PUBLIC, "boolean", "hasNext") //
			.withStatement(ifNotStatement(source, nameReference(source, "$nextDefined"), block(source, //
				assignment(source, "$hasNext", methodCall(source, "getNext")), //
				assignment(source, "$nextDefined", booleanLiteral(source, true))))) //
			.withReturnStatement(nameReference(source, "$hasNext")) //
			.build();
		
		MethodDeclaration next = method(method.node(), source, PUBLIC, elementType, "next") //
			.withStatement(ifNotStatement(source, methodCall(source, "hasNext"), block(source, //
				throwNewException(source, "java.util.NoSuchElementException")))) //
			.withAssignStatement("$nextDefined", booleanLiteral(source, false)) //
			.withReturnStatement(nameReference(source, "$next")) //
			.build();
		
		MethodDeclaration remove = method(method.node(), source, PUBLIC, "void", "remove") //
			.withStatement(throwNewException(source, "java.lang.UnsupportedOperationException")) //
			.build();
		
		MethodDeclaration getNext = method(method.node(), source, PRIVATE, "boolean", "getNext") //
			.withStatement(whileStatement(source, booleanLiteral(source, true), switchStatement)) //
			.build();
		
		ClassBuilder builder = clazz(method.node(), source, 0, yielderName).withBits(IsLocalType) //
			.implementing(typeReference(source, "java.util.Iterator", elementType)) //
			.withFields(variables) //
			.withField(field(method.node(), source, PRIVATE, "int", "$state").build()) //
			.withField(field(method.node(), source, PRIVATE, "boolean", "$hasNext").build()) //
			.withField(field(method.node(), source, PRIVATE, "boolean", "$nextDefined").build()) //
			.withField(field(method.node(), source, PRIVATE, elementType, "$next").build()) //
			.withMethod(constructor(method.node(), source, PRIVATE, yielderName).withImplicitSuper().build());
		if (returnsIterable) {
			AllocationExpression initYielder = new AllocationExpression();
			setGeneratedByAndCopyPos(initYielder, source);
			initYielder.type = typeReference(source, yielderName);
			
			MethodDeclaration iterator = method(method.node(), source, PUBLIC, typeReference(source, "java.util.Iterator", elementType), "iterator") //
				.withReturnStatement(initYielder) //
				.build();
			
			builder.implementing(typeReference(source, "java.lang.Iterable", elementType)) //
				.withMethod(iterator);
		}
		TypeDeclaration yielder = 	builder.withMethod(hasNext) //
			.withMethod(next) //
			.withMethod(remove) //
			.withMethod(getNext) //
			.withTypes(classes).build();
		
		AllocationExpression initYielder = new AllocationExpression();
		setGeneratedByAndCopyPos(initYielder, source);
		initYielder.type = typeReference(source, yielderName);
		
		method.body(yielder, returnStatement(source, initYielder));
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
		private List<TypeDeclaration> classes = new ArrayList<TypeDeclaration>();
		private List<Scope> yields = new ArrayList<Scope>();
		private List<Scope> breaks = new ArrayList<Scope>();
		private List<Scope> variableDecls = new ArrayList<Scope>();
		private List<FieldDeclaration> stateVariables = new ArrayList<FieldDeclaration>();
		private Scope root;
		private Map<ASTNode, Scope> allScopes = new HashMap<ASTNode, Scope>();
		private List<Statement> statements = new ArrayList<Statement>();
		private Map<Expression, Label> labelLiterals = new HashMap<Expression, Label>();
		private Set<Label> usedLabels = new HashSet<Label>();
		private String stateName;
		private String nextName;
		
		public List<FieldDeclaration> getVariables() {
			return stateVariables;
		}
		
		public List<TypeDeclaration> getClasses() {
			return classes;
		}
		
		public SwitchStatement getStateSwitch() {
			List<Statement> switchStatements = new ArrayList<Statement>(statements);
			switchStatements.add(new CaseStatement(null, 0, 0));
			switchStatements.add(returnStatement(declaration, false));
			SwitchStatement switchStatement = new SwitchStatement();
			switchStatement.expression = nameReference(declaration, stateName);
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
					stateVariables.add(field(methodNode, declaration, PRIVATE, variable.type, new String(variable.name)).build());
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
			optimizeBreaks();
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
			Expression literal = new UnaryExpression(label.constantExpression != null ? label.constantExpression : intLiteral(declaration, -1), OperatorIds.PLUS);
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

		private Statement setStateId(Expression expression) {
			return assignment(declaration, stateName, expression);
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

		private void optimizeBreaks() {
			// TODO optimize the shit out of this
			// missing:
			//   case 2: someThing(); state = 3; continue;
			//   case 3: someThingElse();
			//
			// should be:
			//   case 2: someThing(); someThingElse();
			int id = 0;
			Statement previous = null;
			for (int i = 0; i < statements.size(); i++) {
				Statement statement = statements.get(i);
				if (statement instanceof Label) {
					Label label = (Label) statement;
					if ((previous != null) && (previous instanceof Label)) {
						label.id = id;
						statements.remove(i--);
					} else {
						label.id = id++;
					}
					label.constantExpression = intLiteral(declaration, label.id);
				}
				previous = statement;
			}
		}
		
		private void synchronizeLiteralsAndLabels() {
			for (Map.Entry<Expression, Label> entry : labelLiterals.entrySet()) {
				UnaryExpression expression = (UnaryExpression) entry.getKey();
				Label label = entry.getValue();
				expression.expression = intLiteral(declaration, label.id);
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
							addStatement(ifNotStatement(declaration, forStatement.condition, block(declaration, setStateId(labelLiteral(breakLabel)), new ContinueStatement(null, 0, 0))));
						}
						refactorStatement(forStatement.action);
						addStatement(getIterationLabel(this));
						if (forStatement.increments != null) {
							for (Statement statement : forStatement.increments) {
								refactorStatement(statement);
							}
						}
						addStatement(setStateId(labelLiteral(label)));
						addStatement(new ContinueStatement(null, 0, 0));
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
						stateVariables.add(field(methodNode, declaration, PRIVATE, "java.util.Iterator", iteratorVarName).withAnnotation("java.lang.SuppressWarnings", "rawtypes").build());
						addStatement(assignment(declaration, iteratorVarName, methodCall(declaration, forStatement.collection, "iterator")));
						addStatement(getIterationLabel(this));
						addStatement(ifNotStatement(declaration, methodCall(declaration, iteratorVarName, "hasNext"), block(declaration, setStateId(labelLiteral(getBreakLabel(this))), new ContinueStatement(null, 0, 0))));
						addStatement(assignment(declaration, new String(forStatement.elementVariable.name), new CastExpression(methodCall(declaration, iteratorVarName, "next"), forStatement.elementVariable.type)));
						refactorStatement(forStatement.action);
						addStatement(setStateId(labelLiteral(getIterationLabel(this))));
						addStatement(new ContinueStatement(null, 0, 0));
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
						addStatement(ifStatement(declaration, doStatement.condition, block(declaration, setStateId(labelLiteral(getIterationLabel(this))), new ContinueStatement(null, 0, 0))));
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
							addStatement(ifNotStatement(declaration, whileStatement.condition, block(declaration, setStateId(labelLiteral(getBreakLabel(this))), new ContinueStatement(null, 0, 0))));
						}
						refactorStatement(whileStatement.action);
						addStatement(setStateId(labelLiteral(getIterationLabel(this))));
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
						if (!(ifStatement.condition instanceof TrueLiteral)) {
							addStatement(ifNotStatement(declaration, ifStatement.condition, block(declaration, setStateId(labelLiteral(label)), new ContinueStatement(null, 0, 0))));
						}
						if (ifStatement.elseStatement != null) {
							refactorStatement(ifStatement.elseStatement);
							addStatement(setStateId(labelLiteral(getBreakLabel(this))));
							addStatement(new ContinueStatement(null, 0, 0));
							addStatement(label);
						}
						refactorStatement(ifStatement.thenStatement);
						addStatement(getBreakLabel(this));
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
									cases.add(setStateId(labelLiteral(label)));
									cases.add(new ContinueStatement(null, 0, 0));
									addStatement(label);
								} else {
									refactorStatement(statement);
								}
							}
							if (!hasDefault) {
								cases.add(new CaseStatement(null, 0, 0));
								cases.add(setStateId(labelLiteral(breakLabel)));
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
								allocation.type = typeReference(declaration, localDeclaration.type.toString());
								allocation.initializer = initializer;
								allocation.dimensions = new Expression[localDeclaration.type.dimensions()];
								addStatement(assignment(declaration, new String(localDeclaration.name), allocation));
							} else {
								addStatement(assignment(declaration, new String(localDeclaration.name), localDeclaration.initialization));
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
						addStatement(setStateId(labelLiteral(getBreakLabel(target))));
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
						addStatement(setStateId(labelLiteral(getIterationLabel(target))));
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
					classes.add(localTypeDeclaration);
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
							addStatement(assignment(declaration, new String(nextName), expression));
							addStatement(setStateId(labelLiteral(label)));
							addStatement(returnStatement(declaration, true));
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