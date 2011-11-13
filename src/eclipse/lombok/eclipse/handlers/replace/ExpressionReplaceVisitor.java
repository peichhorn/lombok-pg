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

	protected ExpressionReplaceVisitor(final EclipseMethod method, final lombok.ast.Statement<?> replacement) {
		super(method, replacement);
	}

	@Override
	public boolean visit(final AllocationExpression allocationExpression, final BlockScope scope) {
		replace(allocationExpression.arguments);
		return true;
	}

	@Override
	public boolean visit(final AND_AND_Expression and_and_Expression, final BlockScope scope) {
		return visit((BinaryExpression)and_and_Expression, scope);
	}

	@Override
	public boolean visit(final ArrayAllocationExpression arrayAllocationExpression, final BlockScope scope) {
		replace(arrayAllocationExpression.dimensions);
		return true;
	}

	@Override
	public boolean visit(final ArrayInitializer arrayInitializer, final BlockScope scope) {
		replace(arrayInitializer.expressions);
		return true;
	}

	@Override
	public boolean visit(final ArrayReference arrayReference, final BlockScope scope) {
		arrayReference.receiver = replace(arrayReference.receiver);
		arrayReference.position = replace(arrayReference.position);
		return true;
	}

	@Override
	public boolean visit(final Assignment assignment, final BlockScope scope) {
		assignment.lhs = replace(assignment.lhs);
		assignment.expression = replace(assignment.expression);
		return true;
	}

	@Override
	public boolean visit(final BinaryExpression binaryExpression, final BlockScope scope) {
		binaryExpression.left = replace(binaryExpression.left);
		binaryExpression.right = replace(binaryExpression.right);
		return true;
	}

	@Override
	public boolean visit(final CastExpression castExpression, final BlockScope scope) {
		castExpression.expression = replace(castExpression.expression);
		return true;
	}

	@Override
	public boolean visit(final CompoundAssignment compoundAssignment, final BlockScope scope) {
		compoundAssignment.lhs = replace(compoundAssignment.lhs);
		compoundAssignment.expression = replace(compoundAssignment.expression);
		return true;
	}

	@Override
	public boolean visit(final ConditionalExpression conditionalExpression, final BlockScope scope) {
		conditionalExpression.condition = replace(conditionalExpression.condition);
		conditionalExpression.valueIfTrue = replace(conditionalExpression.valueIfTrue);
		conditionalExpression.valueIfFalse = replace(conditionalExpression.valueIfFalse);
		return true;
	}

	@Override
	public boolean visit(final DoStatement doStatement, final BlockScope scope) {
		doStatement.condition = replace(doStatement.condition);
		return true;
	}

	@Override
	public boolean visit(final EqualExpression equalExpression, final BlockScope scope) {
		return visit((BinaryExpression)equalExpression, scope);
	}

	@Override
	public boolean visit(final ExplicitConstructorCall explicitConstructor, final BlockScope scope) {
		replace(explicitConstructor.arguments);
		return true;
	}

	@Override
	public boolean visit(final ForeachStatement forStatement, final BlockScope scope) {
		forStatement.collection = replace(forStatement.collection);
		return true;
	}

	@Override
	public boolean visit(final ForStatement forStatement, final BlockScope scope) {
		forStatement.condition = replace(forStatement.condition);
		return true;
	}

	@Override
	public boolean visit(final IfStatement ifStatement, final BlockScope scope) {
		ifStatement.condition = replace(ifStatement.condition);
		return true;
	}

	@Override
	public boolean visit(final InstanceOfExpression instanceOfExpression, final BlockScope scope) {
		instanceOfExpression.expression = replace(instanceOfExpression.expression);
		return true;
	}

	@Override
	public boolean visit(final LocalDeclaration localDeclaration, final BlockScope scope) {
		localDeclaration.initialization = replace(localDeclaration.initialization);
		return true;
	}

	@Override
	public boolean visit(final MessageSend messageSend, final BlockScope scope) {
		messageSend.receiver = replace(messageSend.receiver);
		replace(messageSend.arguments);
		return true;
	}

	@Override
	public boolean visit(final OR_OR_Expression or_or_Expression, final BlockScope scope) {
		return visit((BinaryExpression)or_or_Expression, scope);
	}

	@Override
	public boolean visit(final PostfixExpression postfixExpression, final BlockScope scope) {
		return visit((CompoundAssignment)postfixExpression, scope);
	}

	@Override
	public boolean visit(final PrefixExpression prefixExpression, final BlockScope scope) {
		return visit((CompoundAssignment)prefixExpression, scope);
	}

	@Override
	public boolean visit(final QualifiedAllocationExpression qualifiedAllocationExpression, final BlockScope scope) {
		return visit((AllocationExpression)qualifiedAllocationExpression, scope);
	}

	@Override
	public boolean visit(final ReturnStatement returnStatement, final BlockScope scope) {
		returnStatement.expression = replace(returnStatement.expression);
		return true;
	}

	@Override
	public boolean visit(final SwitchStatement switchStatement, final BlockScope scope) {
		switchStatement.expression = replace(switchStatement.expression);
		return true;
	}

	@Override
	public boolean visit(final SynchronizedStatement synchronizedStatement, final BlockScope scope) {
		synchronizedStatement.expression = replace(synchronizedStatement.expression);
		return true;
	}

	@Override
	public boolean visit(final ThrowStatement throwStatement, final BlockScope scope) {
		throwStatement.exception = replace(throwStatement.exception);
		return true;
	}

	@Override
	public boolean visit(final UnaryExpression unaryExpression, final BlockScope scope) {
		unaryExpression.expression = replace(unaryExpression.expression);
		return true;
	}

	@Override
	public boolean visit(final WhileStatement whileStatement, final BlockScope scope) {
		whileStatement.condition = replace(whileStatement.condition);
		return true;
	}
}
