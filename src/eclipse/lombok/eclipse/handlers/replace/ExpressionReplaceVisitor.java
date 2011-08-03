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
package lombok.eclipse.handlers.replace;

import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

public abstract class ExpressionReplaceVisitor extends ReplaceVisitor<Expression> {

	protected ExpressionReplaceVisitor(EclipseMethod method, lombok.ast.Statement replacement) {
		super(method, replacement);
	}

	@Override
	public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
		replace(allocationExpression.arguments);
		return true;
	}

	@Override
	public boolean visit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		return visit((BinaryExpression)and_and_Expression, scope);
	}

	@Override
	public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		replace(arrayAllocationExpression.dimensions);
		return true;
	}

	@Override
	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
		replace(arrayInitializer.expressions);
		return true;
	}

	@Override
	public boolean visit(ArrayReference arrayReference, BlockScope scope) {
		arrayReference.receiver = replace(arrayReference.receiver);
		arrayReference.position = replace(arrayReference.position);
		return true;
	}

	@Override
	public boolean visit(Assignment assignment, BlockScope scope) {
		assignment.lhs = replace(assignment.lhs);
		assignment.expression = replace(assignment.expression);
		return true;
	}

	@Override
	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
		binaryExpression.left = replace(binaryExpression.left);
		binaryExpression.right = replace(binaryExpression.right);
		return true;
	}

	@Override
	public boolean visit(CastExpression castExpression, BlockScope scope) {
		castExpression.expression = replace(castExpression.expression);
		return true;
	}

	@Override
	public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
		compoundAssignment.lhs = replace(compoundAssignment.lhs);
		compoundAssignment.expression = replace(compoundAssignment.expression);
		return true;
	}

	@Override
	public boolean visit(ConditionalExpression conditionalExpression, BlockScope scope) {
		conditionalExpression.condition = replace(conditionalExpression.condition);
		conditionalExpression.valueIfTrue = replace(conditionalExpression.valueIfTrue);
		conditionalExpression.valueIfFalse = replace(conditionalExpression.valueIfFalse);
		return true;
	}

	@Override
	public boolean visit(DoStatement doStatement, BlockScope scope) {
		doStatement.condition = replace(doStatement.condition);
		return true;
	}

	@Override
	public boolean visit(EqualExpression equalExpression, BlockScope scope) {
		return visit((BinaryExpression)equalExpression, scope);
	}

	@Override
	public boolean visit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {
		replace(explicitConstructor.arguments);
		return true;
	}

	@Override
	public boolean visit(ForeachStatement forStatement, BlockScope scope) {
		forStatement.collection = replace(forStatement.collection);
		return true;
	}

	@Override
	public boolean visit(ForStatement forStatement, BlockScope scope) {
		forStatement.condition = replace(forStatement.condition);
		return true;
	}

	@Override
	public boolean visit(IfStatement ifStatement, BlockScope scope) {
		ifStatement.condition = replace(ifStatement.condition);
		return true;
	}

	@Override
	public boolean visit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		instanceOfExpression.expression = replace(instanceOfExpression.expression);
		return true;
	}

	@Override
	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		localDeclaration.initialization = replace(localDeclaration.initialization);
		return true;
	}

	@Override
	public boolean visit(MessageSend messageSend, BlockScope scope) {
		messageSend.receiver = replace(messageSend.receiver);
		replace(messageSend.arguments);
		return true;
	}

	@Override
	public boolean visit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		return visit((BinaryExpression)or_or_Expression, scope);
	}

	@Override
	public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
		return visit((CompoundAssignment)postfixExpression, scope);
	}

	@Override
	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
		return visit((CompoundAssignment)prefixExpression, scope);
	}

	@Override
	public boolean visit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
		return visit((AllocationExpression)qualifiedAllocationExpression, scope);
	}

	@Override
	public boolean visit(ReturnStatement returnStatement, BlockScope scope) {
		returnStatement.expression = replace(returnStatement.expression);
		return true;
	}

	@Override
	public boolean visit(SwitchStatement switchStatement, BlockScope scope) {
		switchStatement.expression = replace(switchStatement.expression);
		return true;
	}

	@Override
	public boolean visit(SynchronizedStatement synchronizedStatement, BlockScope scope) {
		synchronizedStatement.expression = replace(synchronizedStatement.expression);
		return true;
	}

	@Override
	public boolean visit(ThrowStatement throwStatement, BlockScope scope) {
		throwStatement.exception = replace(throwStatement.exception);
		return true;
	}

	@Override
	public boolean visit(UnaryExpression unaryExpression, BlockScope scope) {
		unaryExpression.expression = replace(unaryExpression.expression);
		return true;
	}

	@Override
	public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		whileStatement.condition = replace(whileStatement.condition);
		return true;
	}
}
