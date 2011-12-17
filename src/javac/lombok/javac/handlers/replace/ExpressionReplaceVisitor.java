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

	protected ExpressionReplaceVisitor(final JavacMethod method, final lombok.ast.Statement<?> replacement) {
		super(method, replacement);
	}

	@Override
	public Void visitArrayAccess(final ArrayAccessTree tree, final Void p) {
		JCArrayAccess arrayAccess = (JCArrayAccess) tree;
		arrayAccess.index = replace(arrayAccess.index);
		arrayAccess.indexed = replace(arrayAccess.indexed);
		return super.visitArrayAccess(tree, p);
	}

	@Override
	public Void visitAssignment(final AssignmentTree tree, final Void p) {
		JCAssign assign = (JCAssign) tree;
		assign.lhs = replace(assign.lhs);
		assign.rhs = replace(assign.rhs);
		return super.visitAssignment(tree, p);
	}

	@Override
	public Void visitBinary(final BinaryTree tree, final Void p) {
		JCBinary assign = (JCBinary) tree;
		assign.lhs = replace(assign.lhs);
		assign.rhs = replace(assign.rhs);
		return super.visitBinary(tree, p);
	}

	@Override
	public Void visitCompoundAssignment(final CompoundAssignmentTree tree, final Void p) {
		JCAssignOp assignOp = (JCAssignOp) tree;
		assignOp.lhs = replace(assignOp.lhs);
		assignOp.rhs = replace(assignOp.rhs);
		return super.visitCompoundAssignment(tree, p);
	}

	@Override
	public Void visitConditionalExpression(final ConditionalExpressionTree tree, final Void p) {
		JCConditional conditional = (JCConditional) tree;
		conditional.cond = replace(conditional.cond);
		conditional.truepart = replace(conditional.truepart);
		conditional.falsepart = replace(conditional.falsepart);
		return super.visitConditionalExpression(tree, p);
	}

	@Override
	public Void visitDoWhileLoop(final DoWhileLoopTree tree, final Void p) {
		JCDoWhileLoop doWhileLoop = (JCDoWhileLoop) tree;
		doWhileLoop.cond = replace(doWhileLoop.cond);
		return super.visitDoWhileLoop(tree, p);
	}

	@Override
	public Void visitEnhancedForLoop(final EnhancedForLoopTree tree, final Void p) {
		JCEnhancedForLoop enhancedForLoop = (JCEnhancedForLoop) tree;
		enhancedForLoop.expr = replace(enhancedForLoop.expr);
		return super.visitEnhancedForLoop(tree, p);
	}

	@Override
	public Void visitForLoop(final ForLoopTree tree, final Void p) {
		JCForLoop forLoop = (JCForLoop) tree;
		forLoop.cond = replace(forLoop.cond);
		return super.visitForLoop(tree, p);
	}

	@Override
	public Void visitIf(final IfTree tree, final Void p) {
		JCIf ifStatement = (JCIf) tree;
		ifStatement.cond = replace(ifStatement.cond);
		return super.visitIf(tree, p);
	}

	@Override
	public Void visitInstanceOf(final InstanceOfTree tree, final Void p) {
		JCInstanceOf instanceOfExpression = (JCInstanceOf) tree;
		instanceOfExpression.expr = replace(instanceOfExpression.expr);
		return super.visitInstanceOf(tree, p);
	}

	@Override
	public Void visitMethodInvocation(final MethodInvocationTree tree, final Void p) {
		JCMethodInvocation methodInvocation = (JCMethodInvocation) tree;
		methodInvocation.args = replace(methodInvocation.args);
		return super.visitMethodInvocation(tree, p);
	}

	@Override
	public Void visitMemberSelect(final MemberSelectTree tree, final Void p) {
		JCFieldAccess fieldAccess = (JCFieldAccess) tree;
		fieldAccess.selected = replace(fieldAccess.selected);
		return super.visitMemberSelect(tree, p);
	}

	@Override
	public Void visitNewClass(final NewClassTree tree, final Void p) {
		JCNewClass newClass = (JCNewClass) tree;
		newClass.args = replace(newClass.args);
		return super.visitNewClass(tree, p);
	}

	@Override
	public Void visitNewArray(final NewArrayTree tree, final Void p) {
		JCNewArray newArray = (JCNewArray) tree;
		newArray.elems = replace(newArray.elems);
		newArray.dims = replace(newArray.dims);
		return super.visitNewArray(tree, p);
	}

	@Override
	public Void visitReturn(final ReturnTree tree, final Void p) {
		JCReturn returnStatement = (JCReturn) tree;
		returnStatement.expr = replace(returnStatement.expr);
		return super.visitReturn(tree, p);
	}

	@Override
	public Void visitSwitch(final SwitchTree tree, final Void p) {
		JCSwitch switchStatement = (JCSwitch) tree;
		switchStatement.selector = replace(switchStatement.selector);
		return super.visitSwitch(tree, p);
	}

	@Override
	public Void visitSynchronized(final SynchronizedTree tree, final Void p) {
		JCSynchronized synchronizedStatement = (JCSynchronized) tree;
		synchronizedStatement.lock = replace(synchronizedStatement.lock);
		return super.visitSynchronized(tree, p);
	}

	@Override
	public Void visitThrow(final ThrowTree tree, final Void p) {
		JCThrow throwStatement = (JCThrow) tree;
		throwStatement.expr = replace(throwStatement.expr);
		return super.visitThrow(tree, p);
	}

	@Override
	public Void visitTypeCast(final TypeCastTree tree, final Void p) {
		JCTypeCast typeCast = (JCTypeCast) tree;
		typeCast.expr = replace(typeCast.expr);
		return super.visitTypeCast(tree, p);
	}

	@Override
	public Void visitUnary(final UnaryTree tree, final Void p) {
		JCUnary unary = (JCUnary) tree;
		unary.arg = replace(unary.arg);
		return super.visitUnary(tree, p);
	}

	@Override
	public Void visitVariable(final VariableTree tree, final Void p) {
		JCVariableDecl variableDecl = (JCVariableDecl) tree;
		variableDecl.init = replace(variableDecl.init);
		return super.visitVariable(tree, p);
	}

	@Override
	public Void visitWhileLoop(final WhileLoopTree tree, final Void p) {
		JCWhileLoop whileLoop = (JCWhileLoop) tree;
		whileLoop.cond = replace(whileLoop.cond);
		return super.visitWhileLoop(tree, p);
	}
}
