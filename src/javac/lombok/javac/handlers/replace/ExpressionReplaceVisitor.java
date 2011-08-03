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
package lombok.javac.handlers.replace;

import lombok.javac.handlers.ast.JavacMethod;

import com.sun.source.tree.*;
import com.sun.tools.javac.tree.JCTree.*;

public abstract class ExpressionReplaceVisitor extends ReplaceVisitor<JCExpression> {

	protected ExpressionReplaceVisitor(JavacMethod method, lombok.ast.Statement replacement) {
		super(method, replacement);
	}

	@Override public Void visitArrayAccess(ArrayAccessTree tree, Void p) {
		JCArrayAccess arrayAccess = (JCArrayAccess) tree;
		arrayAccess.index = replace(arrayAccess.index);
		arrayAccess.indexed = replace(arrayAccess.indexed);
		return super.visitArrayAccess(tree, p);
	}

	@Override public Void visitAssignment(AssignmentTree tree, Void p) {
		JCAssign assign = (JCAssign) tree;
		assign.lhs = replace(assign.lhs);
		assign.rhs = replace(assign.rhs);
		return super.visitAssignment(tree, p);
	}

	@Override public Void visitBinary(BinaryTree tree, Void p) {
		JCBinary assign = (JCBinary) tree;
		assign.lhs = replace(assign.lhs);
		assign.rhs = replace(assign.rhs);
		return super.visitBinary(tree, p);
	}

	@Override public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
		JCAssignOp assignOp = (JCAssignOp) tree;
		assignOp.lhs = replace(assignOp.lhs);
		assignOp.rhs = replace(assignOp.rhs);
		return super.visitCompoundAssignment(tree, p);
	}

	@Override public Void visitConditionalExpression(ConditionalExpressionTree tree, Void p) {
		JCConditional conditional = (JCConditional) tree;
		conditional.cond = replace(conditional.cond);
		conditional.truepart = replace(conditional.truepart);
		conditional.falsepart = replace(conditional.falsepart);
		return super.visitConditionalExpression(tree, p);
	}

	@Override public Void visitDoWhileLoop(DoWhileLoopTree tree, Void p) {
		JCDoWhileLoop doWhileLoop = (JCDoWhileLoop) tree;
		doWhileLoop.cond = replace(doWhileLoop.cond);
		return super.visitDoWhileLoop(tree, p);
	}

	@Override public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
		JCEnhancedForLoop enhancedForLoop = (JCEnhancedForLoop) tree;
		enhancedForLoop.expr = replace(enhancedForLoop.expr);
		return super.visitEnhancedForLoop(tree, p);
	}

	@Override public Void visitForLoop(ForLoopTree tree, Void p) {
		JCForLoop forLoop = (JCForLoop) tree;
		forLoop.cond = replace(forLoop.cond);
		return super.visitForLoop(tree, p);
	}

	@Override public Void visitIf(IfTree tree, Void p) {
		JCIf ifStatement = (JCIf) tree;
		ifStatement.cond = replace(ifStatement.cond);
		return super.visitIf(tree, p);
	}

	@Override public Void visitInstanceOf(InstanceOfTree tree, Void p) {
		JCInstanceOf instanceOfExpression = (JCInstanceOf) tree;
		instanceOfExpression.expr = replace(instanceOfExpression.expr);
		return super.visitInstanceOf(tree, p);
	}

	@Override
	public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
		JCMethodInvocation methodInvocation = (JCMethodInvocation)tree;
		methodInvocation.args = replace(methodInvocation.args);
		return super.visitMethodInvocation(tree, p);
	}

	@Override
	public Void visitMemberSelect(MemberSelectTree tree, Void p) {
		JCFieldAccess fieldAccess = (JCFieldAccess)tree;
		fieldAccess.selected = replace(fieldAccess.selected);
		return super.visitMemberSelect(tree, p);
	}

	@Override
	public Void visitNewClass(NewClassTree tree, Void p) {
		JCNewClass newClass = (JCNewClass)tree;
		newClass.args = replace(newClass.args);
		return super.visitNewClass(tree, p);
	}

	@Override
	public Void visitNewArray(NewArrayTree tree, Void p) {
		JCNewArray newArray = (JCNewArray)tree;
		newArray.elems = replace(newArray.elems);
		newArray.dims = replace(newArray.dims);
		return super.visitNewArray(tree, p);
	}

	@Override public Void visitReturn(ReturnTree tree, Void p) {
		JCReturn returnStatement = (JCReturn) tree;
		returnStatement.expr = replace(returnStatement.expr);
		return super.visitReturn(tree, p);
	}

	@Override public Void visitSwitch(SwitchTree tree, Void p) {
		JCSwitch switchStatement = (JCSwitch) tree;
		switchStatement.selector = replace(switchStatement.selector);
		return super.visitSwitch(tree, p);
	}

	@Override public Void visitSynchronized(SynchronizedTree tree, Void p) {
		JCSynchronized synchronizedStatement = (JCSynchronized) tree;
		synchronizedStatement.lock = replace(synchronizedStatement.lock);
		return super.visitSynchronized(tree, p);
	}

	@Override public Void visitThrow(ThrowTree tree, Void p) {
		JCThrow throwStatement = (JCThrow) tree;
		throwStatement.expr = replace(throwStatement.expr);
		return super.visitThrow(tree, p);
	}

	@Override public Void visitTypeCast(TypeCastTree tree, Void p) {
		JCTypeCast typeCast = (JCTypeCast) tree;
		typeCast.expr = replace(typeCast.expr);
		return super.visitTypeCast(tree, p);
	}

	@Override public Void visitUnary(UnaryTree tree, Void p) {
		JCUnary unary = (JCUnary) tree;
		unary.arg = replace(unary.arg);
		return super.visitUnary(tree, p);
	}

	@Override public Void visitVariable(VariableTree tree, Void p) {
		JCVariableDecl variableDecl = (JCVariableDecl) tree;
		variableDecl.init = replace(variableDecl.init);
		return super.visitVariable(tree, p);
	}

	@Override public Void visitWhileLoop(WhileLoopTree tree, Void p) {
		JCWhileLoop whileLoop = (JCWhileLoop) tree;
		whileLoop.cond = replace(whileLoop.cond);
		return super.visitWhileLoop(tree, p);
	}
}
