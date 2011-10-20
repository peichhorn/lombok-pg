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
import lombok.core.handlers.YieldHandler;
import lombok.core.handlers.YieldHandler.AbstractYieldDataCollector;
import lombok.core.handlers.YieldHandler.ErrorHandler;
import lombok.core.handlers.YieldHandler.Scope;
import lombok.core.util.As;
import lombok.core.util.Each;
import lombok.core.util.Is;
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
				} else if (new YieldHandler<EclipseMethod, ASTNode>().handle(method, new EclipseYieldDataCollector())) {
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

	private static class EclipseYieldDataCollector extends AbstractYieldDataCollector<EclipseMethod, ASTNode> {

		public String elementType(final EclipseMethod method) {
			MethodDeclaration methodDecl = (MethodDeclaration)method.get();
			TypeReference type = methodDecl.returnType;
			if (type instanceof ParameterizedSingleTypeReference) {
				ParameterizedSingleTypeReference returnType = (ParameterizedSingleTypeReference)type;
				if (returnType.typeArguments != null) {
					return returnType.typeArguments[0].toString();
				}
			}
			return Object.class.getName();
		}

		public boolean scan() {
			try {
				method.get().traverse(new YieldQuickScanner(), (ClassScope)null);
				return false;
			} catch (final IllegalStateException ignore) {
				// this means there are unhandled yields left
			}

			ValidationScanner scanner = new ValidationScanner();
			method.get().traverse(scanner, (ClassScope)null);

			for (Scope<ASTNode> scope : yields) {
				Scope<ASTNode> yieldScope = scope;
				do {
					allScopes.put(yieldScope.node, yieldScope);
					yieldScope = yieldScope.parent;
				} while (yieldScope != null);
			}

			boolean collected = !breaks.isEmpty();
			while (collected) {
				collected = false;
				for (Scope<ASTNode> scope : breaks) {
					Scope<ASTNode> target = scope.target;
					if (((target == null) || allScopes.containsKey(target.node)) && !allScopes.containsKey(scope.node)) {
						collected = true;
						Scope<ASTNode> breakScope = scope;
						do {
							allScopes.put(breakScope.node, breakScope);
							breakScope = breakScope.parent;
						} while (breakScope != null);
					}
				}
			}

			for (Scope<ASTNode> scope : variableDecls) {
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
					lombok.ast.FieldDecl field = FieldDecl(Type(variable.type), As.string(variable.name)).makePrivate();
					if (scope.parent.node instanceof TryStatement) {
						field.withAnnotation(Annotation(Type(SuppressWarnings.class)).withValue(String("unused")));
					}
					stateVariables.add(field);
				}
			}
			return true;
		}

		public void refactor() {
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
			if ("yield".equals(As.string(invoke.selector)) && (invoke.arguments != null) && (invoke.arguments.length == 1)) {
				return invoke.arguments[0];
			}
			return null;
		}

		private boolean isTrueLiteral(final Expression expression) {
			return expression instanceof TrueLiteral;
		}

		private Scope<ASTNode> getFinallyScope(Scope<ASTNode> scope, Scope<ASTNode> top) {
			for (ASTNode previous = null; scope != null; scope = scope.parent) {
				ASTNode tree = scope.node;
				if (tree instanceof TryStatement) {
					TryStatement statement = (TryStatement) tree;
					if ((statement.finallyBlock != null) && (statement.finallyBlock != previous)) {
						return scope;
					}
				}
				if (scope == top) break;
				previous = tree;
			}
			return null;
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
			private Scope<ASTNode> current;

			@Override
			public boolean visit(final MethodDeclaration tree, final ClassScope scope) {
				current = new Scope<ASTNode>(current, tree) {
					@Override
					public void refactor() {
						for (Statement statement : Each.elementIn(tree.statements)) {
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
				current = new Scope<ASTNode>(current, tree) {
					@Override
					public void refactor() {
						for (Statement statement : Each.elementIn(tree.statements)) {
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
				current = new Scope<ASTNode>(current, tree) {
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
				current = new Scope<ASTNode>(current, tree) {
					@Override
					public void refactor() {
						for (Statement statement : Each.elementIn(tree.initializations)) {
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
						for (Statement statement : Each.elementIn(tree.increments)) {
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
				current = new Scope<ASTNode>(current, tree) {
					@Override
					public void refactor() {
						String iteratorVar = "$" + As.string(tree.elementVariable.name) + "Iter";
						stateVariables.add(FieldDecl(Type("java.util.Iterator"), iteratorVar).makePrivate().withAnnotation(Annotation(Type(SuppressWarnings.class)).withValue(String("all"))));

						addStatement(Assign(Name(iteratorVar), Call(Expr(tree.collection), "iterator")));
						addLabel(getIterationLabel(this));
						addStatement(If(Not(Call(Name(iteratorVar), "hasNext"))).Then(Block().withStatement(setState(literal(getBreakLabel(this)))).withStatement(Continue())));
						addStatement(Assign(Name(As.string(tree.elementVariable.name)), Cast(Type(tree.elementVariable.type), Call(Name(iteratorVar), "next"))));

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
				current = new Scope<ASTNode>(current, tree) {
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
				current = new Scope<ASTNode>(current, tree) {
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
				current = new Scope<ASTNode>(current, tree) {
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
				current = new Scope<ASTNode>(current, tree) {
					@Override
					public void refactor() {
						lombok.ast.Case breakLabel = getBreakLabel(this);
						lombok.ast.Switch switchStatement = Switch(Expr(tree.expression));
						addStatement(switchStatement);
						if (Is.notEmpty(tree.statements)) {
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
				current = new Scope<ASTNode>(current, tree) {
					@Override
					public void refactor() {
						boolean hasFinally = tree.finallyBlock != null;
						boolean hasCatch = Is.notEmpty(tree.catchArguments);
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
							labelName = "$state" + finallyBlocks;
							
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
									.withStatement(Assign(Name(As.string(argument.name)), Cast(Type(argument.type), Name(errorName)))) //
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

							Scope<ASTNode> next = getFinallyScope(parent, null);
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
				variableDecls.add(new Scope<ASTNode>(current, tree) {
					@Override
					public void refactor() {
						if (tree.initialization != null) {
							if (tree.initialization instanceof ArrayInitializer) {
								ArrayInitializer initializer = (ArrayInitializer) tree.initialization;
								ArrayAllocationExpression allocation = new ArrayAllocationExpression();
								allocation.type = method.build(Type(tree.type.toString()));
								allocation.initializer = initializer;
								allocation.dimensions = new Expression[tree.type.dimensions()];
								addStatement(Assign(Name(As.string(tree.name)), Expr(allocation)));
							} else {
								addStatement(Assign(Name(As.string(tree.name)), Expr(tree.initialization)));
							}
						}
					}
				});

				return super.visit(tree, scope);
			}

			@Override
			public boolean visit(final Argument tree, final BlockScope scope) {
				current = new Scope<ASTNode>(current, tree) {
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
				Scope<ASTNode> target = null;
				char[] label = tree.label;
				if (label != null) {
					for (Scope<ASTNode> labelScope = current; labelScope != null; labelScope = labelScope.parent) {
						if (labelScope.node instanceof LabeledStatement) {
							LabeledStatement labeledStatement = (LabeledStatement) labelScope.node;
							if (Arrays.equals(label, labeledStatement.label)) {
								if (target != null) {
									method.node().addError("Invalid label.");
								}
								target = labelScope;
							}
						}
					}
				} else {
					for (Scope<ASTNode> labelScope = current; labelScope != null; labelScope = labelScope.parent) {
						if (Is.oneOf(labelScope.node, ForStatement.class, ForeachStatement.class, WhileStatement.class, DoStatement.class, SwitchStatement.class)) {
							target = labelScope;
							break;
						}
					}
				}

				if (target == null) {
					method.node().addError("Invalid break.");
				}

				current = new Scope<ASTNode>(current, tree) {
					@Override
					public void refactor() {
						Scope<ASTNode> next = getFinallyScope(parent, target);
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
				Scope<ASTNode> target = null;
				char[] label = tree.label;
				if (label != null) {
					for (Scope<ASTNode> labelScope = current; labelScope != null; labelScope = labelScope.parent) {
						if (labelScope.node instanceof LabeledStatement) {
							LabeledStatement labeledStatement = (LabeledStatement) labelScope.node;
							if (label == labeledStatement.label) {
								if (target != null) {
									method.node().addError("Invalid label.");
								}
								if (Is.oneOf(labelScope.node, ForStatement.class, ForeachStatement.class, WhileStatement.class, DoStatement.class)) {
									target = labelScope;
								} else {
									method.node().addError("Invalid continue.");
								}
							}
						}
					}
				} else {
					for (Scope<ASTNode> labelScope = current; labelScope != null; labelScope = labelScope.parent) {
						if (Is.oneOf(labelScope.node, ForStatement.class, ForeachStatement.class, WhileStatement.class, DoStatement.class)) {
							target = labelScope;
							break;
						}
					}
				}

				if (target == null) {
					method.node().addError("Invalid continue.");
				}

				current = new Scope<ASTNode>(current, tree) {
					@Override
					public void refactor() {
						Scope<ASTNode> next = getFinallyScope(parent, target);
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
				names.add(As.string(tree.token));
				return super.visit(tree, scope);
			}

			@Override
			public boolean visit(final MessageSend tree, final BlockScope scope) {
				final Expression expression = getYieldExpression(tree);
				if (expression != null) {
					current = new Scope<ASTNode>(current, tree) {
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
						String name = As.string(tree.selector);
						if (Is.oneOf(name, "hasNext", "next", "remove")) {
							method.node().addError(String.format("Cannot call method %s(), as it is hidden.", name));
						}
					}
					return super.visit(tree, scope);
				}
			}
		}
	}
}
