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

import static lombok.ast.AST.*;
import static lombok.core.util.Arrays.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.eclipse.handlers.Eclipse.*;

import java.util.*;

import lombok.*;
import lombok.eclipse.EclipseASTAdapter;
import lombok.eclipse.EclipseASTVisitor;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseASTMaker;
import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.mangosdk.spi.ProviderFor;

@ProviderFor(EclipseASTVisitor.class)
public class HandleTuple extends EclipseASTAdapter {
	private final Set<String> methodNames = new HashSet<String>();
	private int withVarCounter;

	@Override public void visitCompilationUnit(final EclipseNode top, final CompilationUnitDeclaration unit) {
		methodNames.clear();
		withVarCounter = 0;
	}
	
	@Override public void visitLocal(final EclipseNode localNode, final LocalDeclaration local) {
		MessageSend initTupleCall = getTupelCall(localNode, local.initialization);
		if (initTupleCall != null) {
			final EclipseMethod method = EclipseMethod.methodOf(localNode, local);
			if (method == null) {
				localNode.addError(canBeUsedInBodyOfMethodsOnly("tuple"));
			} else if (handle(localNode, initTupleCall)) {
				methodNames.add(getMethodName(initTupleCall));
			}
		}
	}

	@Override public void visitStatement(final EclipseNode statementNode, final Statement statement) {
		if (statement instanceof Assignment) {
			final Assignment assignment = (Assignment) statement;
			final MessageSend leftTupleCall = getTupelCall(statementNode, assignment.lhs);
			final MessageSend rightTupleCall = getTupelCall(statementNode, assignment.expression);
			if ((leftTupleCall != null) && (rightTupleCall != null)) {
				final EclipseMethod method = EclipseMethod.methodOf(statementNode, statement);
				if (method == null) {
					statementNode.addError(canBeUsedInBodyOfMethodsOnly("tuple"));
				} else if (handle(statementNode, leftTupleCall, rightTupleCall)) {
					methodNames.add(getMethodName(leftTupleCall));
					methodNames.add(getMethodName(rightTupleCall));
				}
			}
		}
	}

	private MessageSend getTupelCall(final EclipseNode node, final Expression expression) {
		if (expression instanceof MessageSend) {
			final MessageSend tupleCall = (MessageSend) expression ;
			final String methodName = getMethodName(tupleCall);
			if (isMethodCallValid(node, methodName, Tuple.class, "tuple")) {
				return tupleCall;
			}
		}
		return null;
	}

	@Override public void endVisitCompilationUnit(final EclipseNode top, final CompilationUnitDeclaration unit) {
		for (String methodName : methodNames) {
			deleteMethodCallImports(top, methodName, Tuple.class, "tuple");
		}
	}

	public boolean handle(final EclipseNode tupleInitNode, final MessageSend initTupleCall) {
		if (isEmpty(initTupleCall.arguments)) {
			return true;
		}
		int numberOfArguments = initTupleCall.arguments.length;
		List<LocalDeclaration> localDecls = new ArrayList<LocalDeclaration>();
		String type = ((LocalDeclaration)tupleInitNode.get()).type.toString();
		for (EclipseNode node : tupleInitNode.directUp().down()) {
			if (!(node.get() instanceof LocalDeclaration)) continue;
			LocalDeclaration localDecl = (LocalDeclaration)node.get();
			if (!type.equals(localDecl.type.toString())) continue;
			localDecls.add(localDecl);
			if (localDecls.size() > numberOfArguments) {
				localDecls.remove(0);
			}
			if (node.equals(tupleInitNode)) {
				break;
			}
		}
		if (numberOfArguments != localDecls.size()) {
			tupleInitNode.addError(String.format("Argument mismatch on the right side. (required: %s found: %s)", localDecls.size(), numberOfArguments));
			return false;
		}
		int index = 0;
		for (LocalDeclaration localDecl : localDecls) {
			localDecl.initialization = initTupleCall.arguments[index++];
		}
		return true;
	}
	
	public boolean handle(final EclipseNode tupleAssignNode, final MessageSend leftTupleCall, final MessageSend rightTupleCall) {
		if (!sameSize(leftTupleCall.arguments, rightTupleCall.arguments) && (rightTupleCall.arguments.length != 1)) {
			tupleAssignNode.addError("The left and right hand side of the assignment must have the same amount of arguments or" +
					" must have one array-type argument for the tuple assignment to work.");
			return false;
		}
		if (!containsOnlyNames(leftTupleCall.arguments)) {
			tupleAssignNode.addError("Only variable names are allowed as arguments of the left hand side in a tuple assignment.");
			return false;
		}

		List<Statement> tempVarAssignments = new ArrayList<Statement>();
		List<Statement> assignments = new ArrayList<Statement>();

		List<String> varnames = collectVarnames(leftTupleCall.arguments);
		EclipseASTMaker builder = new EclipseASTMaker(tupleAssignNode, leftTupleCall);
		if (sameSize(leftTupleCall.arguments, rightTupleCall.arguments)) {
			Iterator<String> varnameIter = varnames.listIterator();
			final Set<String> blacklistedNames = new HashSet<String>();
			if (rightTupleCall.arguments != null) for (Expression arg : rightTupleCall.arguments) {
				String varname = varnameIter.next();
				final boolean canUseSimpleAssignment = new SimpleAssignmentAnalyser(blacklistedNames).scan(arg);
				blacklistedNames.add(varname);
				if (!canUseSimpleAssignment) {
					final TypeReference vartype = new VarTypeFinder(varname, tupleAssignNode.get()).scan(tupleAssignNode.top().get());
					if (vartype != null) {
						String tempVarname = "$tuple" + withVarCounter++;
						tempVarAssignments.add(builder.build(LocalDecl(Type(vartype), tempVarname).makeFinal().withInitialization(Expr(arg)), Statement.class));
						assignments.add(builder.build(Assign(Name(varname), Name(tempVarname)), Statement.class));
					} else {
						tupleAssignNode.addError("Lombok-pg Bug. Unable to find vartype.");
						return false;
					}
				} else {
					assignments.add(builder.build(Assign(Name(varname), Expr(arg)), Statement.class));
				}
			}
		} else {
			final TypeReference vartype = new VarTypeFinder(varnames.get(0), tupleAssignNode.get()).scan(tupleAssignNode.top().get());
			if (vartype != null) {
				String tempVarname = "$tuple" + withVarCounter++;
				tempVarAssignments.add(builder.build(LocalDecl(Type(vartype), tempVarname).makeFinal().withInitialization(Expr(rightTupleCall.arguments[0])), Statement.class));
				int arrayIndex = 0;
				for (String varname : varnames) {
					assignments.add(builder.build(Assign(Name(varname), ArrayRef(Name(tempVarname), Number(arrayIndex++))), Statement.class));
				}
			}
		}
		tempVarAssignments.addAll(assignments);
		tryToInjectStatements(tupleAssignNode, tupleAssignNode.get(), tempVarAssignments);

		return true;
	}

	private void tryToInjectStatements(final EclipseNode node, final ASTNode nodeThatUsesTupel, final List<Statement> statementsToInject) {
		EclipseNode parent = node;
		ASTNode statementThatUsesTupel = nodeThatUsesTupel;
		while ((!(parent.directUp().get() instanceof AbstractMethodDeclaration)) && (!(parent.directUp().get() instanceof Block))) {
			parent = parent.directUp();
			statementThatUsesTupel = parent.get();
		}
		Statement statement = (Statement) statementThatUsesTupel;
		EclipseNode grandParent = parent.directUp();
		ASTNode block = grandParent.get();
		if (block instanceof Block) {
			((Block)block).statements = injectStatements(((Block)block).statements, statement, statementsToInject);
		} else if (block instanceof AbstractMethodDeclaration) {
			((AbstractMethodDeclaration)block).statements = injectStatements(((AbstractMethodDeclaration)block).statements, statement, statementsToInject);
		} else {
			// this would be odd but what the hell
			return;
		}
		grandParent.rebuild();
	}

	private static Statement[] injectStatements(final Statement[] statements, final Statement statement, final List<Statement> withCallStatements) {
		final List<Statement> newStatements = new ArrayList<Statement>();
		for (Statement stat : statements) {
			if (stat == statement) {
				newStatements.addAll(withCallStatements);
			} else newStatements.add(stat);
		}
		return newStatements.toArray(new Statement[newStatements.size()]);
	}

	private List<String> collectVarnames(final Expression[] expressions) {
		List<String> varnames = new ArrayList<String>();
		if (expressions != null) for (Expression expression : expressions) {
				varnames.add(new String(((SingleNameReference)expression).token));
		}
		return varnames;
	}

	private boolean containsOnlyNames(final Expression[] expressions) {
		if (expressions != null) for (Expression expression : expressions) {
			if (!(expression instanceof SingleNameReference)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Look for the type of a variable in the scope of the given expression.
	 * <p>
	 * {@link VarTypeFinder#scan(com.sun.source.tree.Tree, Void) VarTypeFinder.scan(Tree, Void)} will
	 * return the type of a variable in the scope of the given expression.
	 */
	@RequiredArgsConstructor
	private static class VarTypeFinder extends ASTVisitor {
		private final String varname;
		private final ASTNode expr;
		private boolean lockVarname;
		private TypeReference vartype;

		public TypeReference scan(final ASTNode astNode) {
			if (astNode instanceof CompilationUnitDeclaration) {
				((CompilationUnitDeclaration)astNode).traverse(this, (CompilationUnitScope)null);
			} else if (astNode instanceof MethodDeclaration) {
				((MethodDeclaration)astNode).traverse(this, (ClassScope)null);
			} else {
				astNode.traverse(this, null);
			}
			return vartype;
		}

		@Override public boolean visit(final LocalDeclaration localDeclaration, final BlockScope scope) {
			return visit(localDeclaration);
		}

		@Override public boolean visit(final FieldDeclaration fieldDeclaration, final MethodScope scope) {
			return visit(fieldDeclaration);
		}

		@Override public boolean visit(final Argument argument, final BlockScope scope) {
			return visit(argument);
		}

		@Override public boolean visit(final Argument argument, final ClassScope scope) {
			return visit(argument);
		}

		@Override public boolean visit(final Assignment assignment, final BlockScope scope) {
			if ((expr != null) && (expr.equals(assignment))) {
				lockVarname = true;
			}
			return true;
		}

		public boolean visit(final AbstractVariableDeclaration variableDeclaration) {
			if (!lockVarname && varname.equals(new String(variableDeclaration.name))) {
				vartype = variableDeclaration.type;
			}
			return true;
		}
	}

	/**
	 * Look for variable names that would break a simple assignment after transforming the tuple.
	 * <p>
	 * If {@link SimpleAssignmentAnalyser#scan(com.sun.source.tree.Tree, Void) AssignmentAnalyser.scan(Tree, Void)}
	 * return {@code null} or {@code true} everything is fine, otherwise a temporary assignment is needed.
	 */
	@RequiredArgsConstructor
	private static class SimpleAssignmentAnalyser extends ASTVisitor {
		private final Set<String> blacklistedVarnames;
		private boolean canUseSimpleAssignment;

		public boolean scan(final ASTNode astNode) {
			canUseSimpleAssignment = true;
			if (astNode instanceof CompilationUnitDeclaration) {
				((CompilationUnitDeclaration)astNode).traverse(this, (CompilationUnitScope)null);
			} else if (astNode instanceof MethodDeclaration) {
				((MethodDeclaration)astNode).traverse(this, (ClassScope)null);
			} else {
				astNode.traverse(this, null);
			}
			return canUseSimpleAssignment;
		}

		@Override public boolean visit(final SingleNameReference singleNameReference, final BlockScope scope) {
			if (blacklistedVarnames.contains(new String(singleNameReference.token))) {
				canUseSimpleAssignment = false;
				return false;
			}
			return true;
		}
	}
}
