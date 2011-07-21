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

import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;

/**
 * Replaces all implicit and explicit occurrences of 'this' with a specified expression.
 */
// TODO incomplete, but works for the current use-case
public class ThisReferenceReplaceVisitor extends ReplaceVisitor<Expression> {

	public ThisReferenceReplaceVisitor(EclipseMethod method, lombok.ast.Statement replacement) {
		super(method, replacement);
	}

	@Override
	public boolean visit(MessageSend messageSend, BlockScope scope) {
		replace(messageSend.arguments);
		return true;
	}

	@Override
	public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
		replace(allocationExpression.arguments);
		return true;
	}

	@Override
	protected boolean needsReplacing(Expression node) {
		return node instanceof ThisReference;
	}
}
