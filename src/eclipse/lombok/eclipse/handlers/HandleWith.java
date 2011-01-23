/*
 * Copyright © 2010-2011 Philipp Eichhorn
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
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Arrays.*;
import static lombok.eclipse.handlers.EclipseNodeBuilder.*;
import static org.eclipse.jdt.core.dom.Modifier.FINAL;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.mangosdk.spi.ProviderFor;

import lombok.With;
import lombok.eclipse.EclipseASTAdapter;
import lombok.eclipse.EclipseASTVisitor;
import lombok.eclipse.EclipseNode;

@ProviderFor(EclipseASTVisitor.class)
public class HandleWith extends EclipseASTAdapter {
	private boolean handled;
	private String methodName;
	private int withVarCounter;
	
	@Override public void visitCompilationUnit(EclipseNode top, CompilationUnitDeclaration unit) {
		handled = false;
		withVarCounter = 0;
	}
	
	@Override public void visitStatement(EclipseNode statementNode, Statement statement) {
		if (statement instanceof MessageSend) {
			MessageSend methodCall = (MessageSend) statement;
			methodName = new String(methodCall.selector);
			if (methodCallIsValid(statementNode, methodName, With.class, "with")) {
				handled = handle(statementNode, methodCall);
			}
		}
	}
	
	@Override public void endVisitCompilationUnit(EclipseNode top, CompilationUnitDeclaration unit) {
		if (handled) {
			deleteMethodCallImports(top, methodName, With.class, "with");
		}
	}
	
	public boolean handle(EclipseNode methodCallNode, MessageSend withCall) {
		if (isEmpty(withCall.arguments) || (withCall.arguments.length < 2)) {
			return true;
		}
		
		ASTNode source = withCall;
		
		List<Statement> withCallStatements = new ArrayList<Statement>();
		Expression withExpr = withCall.arguments[0];
		String withExprName;
		if ((withExpr instanceof QualifiedNameReference) || (withExpr instanceof SingleNameReference)) {
			withExprName = withExpr.toString();
		} else if (withExpr instanceof AllocationExpression) {
			withExprName = "$with" + (withVarCounter++);
			withCallStatements.add(local(methodCallNode, source, FINAL, ((AllocationExpression)withExpr).type, withExprName).withInitialization(withExpr).build());
			withExpr = nameReference(source, withExprName);
		} else {
			methodCallNode.addError(firstArgumentCanBeVariableNameOrNewClassStatementOnly("with"));
			return false;
		}
		EclipseNode parent = methodCallNode.directUp();
		ASTNode statementThatUsesWith = parent.get();
		EclipseNode grandParent = parent.directUp();
		boolean wasNoMethodCall = true;
		if ((statementThatUsesWith instanceof Assignment) && ((Assignment)statementThatUsesWith).expression == withCall) {
			((Assignment)statementThatUsesWith).expression = withExpr;
		} else if (statementThatUsesWith instanceof LocalDeclaration) {
			((LocalDeclaration)statementThatUsesWith).initialization = withExpr;
		} else if (statementThatUsesWith instanceof ReturnStatement) {
			((ReturnStatement)statementThatUsesWith).expression = withExpr;
		} else if (statementThatUsesWith instanceof MessageSend) {
			MessageSend methodCall = (MessageSend)statementThatUsesWith;
			if (methodCall.receiver == withCall) {
				methodCall.receiver = withExpr;
			} else {
				if (isNotEmpty(methodCall.arguments)) for (int i = 0; i < methodCall.arguments.length; i++) {
					if (methodCall.arguments[i] == withCall) methodCall.arguments[i] = withExpr;
				}
			}
		} else if ((statementThatUsesWith instanceof AbstractMethodDeclaration) || (statementThatUsesWith instanceof Block)) {
			grandParent = parent;
			parent = methodCallNode;
			statementThatUsesWith = parent.get();
			wasNoMethodCall = false;
		} else {
			methodCallNode.addError(isNotAllowedHere("with"));
			return false;
		}
		
		Statement arg;
		for (int i = 1, iend = withCall.arguments.length; i < iend; i++) {
			arg = withCall.arguments[i];
			if (arg instanceof MessageSend) {
				MessageSend methodCall = (MessageSend) arg;
				methodCall.traverse(new WithReferenceReplaceVisitor(source, withExprName), (BlockScope)null);
				setGeneratedByAndCopyPos(methodCall, source);
				withCallStatements.add(arg);
			} else {
				methodCallNode.addError(unsupportedExpressionIn("with", arg));
				return false;
			}
		}
		
		while ((!(parent.directUp().get() instanceof AbstractMethodDeclaration)) && (!(parent.directUp().get() instanceof Block))) {
			parent = parent.directUp();
			statementThatUsesWith = parent.get();
		}
		
		grandParent = parent.directUp();
		ASTNode block = grandParent.get();
		if (block instanceof Block) {
			((Block)block).statements = injectStatements(((Block)block).statements, (Statement)statementThatUsesWith, wasNoMethodCall, withCallStatements);
		} else if (block instanceof AbstractMethodDeclaration) {
			((AbstractMethodDeclaration)block).statements = injectStatements(((AbstractMethodDeclaration)block).statements, (Statement)statementThatUsesWith, wasNoMethodCall, withCallStatements);
		} else {
			// this would be odd but what the hell
			return false;
		}
		
		grandParent.rebuild();
		
		return true;
	}
	
	private static Statement[] injectStatements(Statement[] statements, Statement statement, boolean wasNoMethodCall, List<Statement> withCallStatements) {
		final List<Statement> newStatements = new ArrayList<Statement>();
		for (Statement stat : statements) {
			if (stat == statement) {
				newStatements.addAll(withCallStatements);
				if (wasNoMethodCall) newStatements.add(stat);
			} else newStatements.add(stat);
		}
		return newStatements.toArray(new Statement[newStatements.size()]);
	}
	
	public static class WithReferenceReplaceVisitor extends ASTVisitor {
		private final ASTNode source;
		private final String withExprName;
		
		public WithReferenceReplaceVisitor(final ASTNode source, final String withExprName) {
			super();
			this.source = source;
			this.withExprName = withExprName;
		}
		
		public boolean visit(MessageSend messageSend, BlockScope scope) {
			messageSend.arguments = tryToReplace(messageSend.arguments);
			messageSend.receiver = tryToReplace(messageSend.receiver);
			return true;
		}
		
		public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
			allocationExpression.arguments = tryToReplace(allocationExpression.arguments);
			return true;
		}
		
		public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
			arrayAllocationExpression.dimensions = tryToReplace(arrayAllocationExpression.dimensions);
			return true;
		}
		
		public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
			arrayInitializer.expressions = tryToReplace(arrayInitializer.expressions);
			return true;
		}
		
		private Expression[] tryToReplace(Expression[] expressions) {
			Expression[] newExpressions = expressions;
			if (isNotEmpty(newExpressions)) {
				newExpressions = copy(expressions);
				for (int i = 0, iend = expressions.length; i < iend; i++) {
					newExpressions[i] = tryToReplace(newExpressions[i]);
				}
			}
			return newExpressions;
		}
		
		private Expression tryToReplace(Expression expr) {
			if (expr == null) return null;
			if (expr instanceof ThisReference) {
				if ((expr.bits & ASTNode.IsImplicitThis) != 0) {
					return nameReference(source, withExprName);
				} else {
					expr.bits |= ASTNode.IsImplicitThis;
				}
			} else if (expr instanceof SingleNameReference) {
				String s = expr.toString();
				if ("_".equals(s)) {
					((SingleNameReference) expr).token = withExprName.toCharArray();
				}
			} else if (expr instanceof FieldReference) {
				Expression receiver = ((FieldReference)expr).receiver;
				if (receiver instanceof ThisReference) {
					return nameReference(source, new String(((FieldReference)expr).token));
				}
			}
			return expr;
		}
	}
}