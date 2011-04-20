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

import static lombok.core.util.Arrays.isNotEmpty;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;

/**
 * Replaces all implicit and explicit occurrences of 'this' with a specified expression.
 */
// TODO incomplete, but works for the current use-case
public class ThisReferenceReplaceVisitor extends ASTVisitor {
	private final IReplacementProvider replacement;

	public ThisReferenceReplaceVisitor(final IReplacementProvider replacement) {
		super();
		this.replacement = replacement;
	}

	public void visit(ASTNode astNode) {
		if (astNode instanceof MethodDeclaration) {
			((MethodDeclaration)astNode).traverse(this, (ClassScope)null);
		} else {
			astNode.traverse(this, null);
		}
	}

	@Override
	public boolean visit(MessageSend messageSend, BlockScope scope) {
		replaceArguments(messageSend.arguments);
		return true;
	}

	@Override
	public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
		replaceArguments(allocationExpression.arguments);
		return true;
	}

	private void replaceArguments(Expression[] arguments) {
		if (isNotEmpty(arguments)) for (int i = 0, iend = arguments.length; i < iend; i++) {
			if (arguments[i] instanceof ThisReference) {
				arguments[i] = replacement.getReplacement();
			}
		}
	}

	public static interface IReplacementProvider {
		public Expression getReplacement();
	}
}