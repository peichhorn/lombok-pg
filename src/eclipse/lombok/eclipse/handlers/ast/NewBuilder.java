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
package lombok.eclipse.handlers.ast;

import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.handlers.ast.Arrays.buildArray;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class NewBuilder extends GenericTypeArgumentBuilder<NewBuilder, AllocationExpression> {
	private final List<ExpressionBuilder<? extends Expression>> args = new ArrayList<ExpressionBuilder<? extends Expression>>();
	private final ExpressionBuilder<? extends TypeReference> type;

	public NewBuilder withArgument(final ExpressionBuilder<? extends Expression> arg) {
		args.add(arg);
		return self();
	}

	@Override
	public final AllocationExpression build(final EclipseNode node, final ASTNode source) {
		final AllocationExpression initExpression = new AllocationExpression();
		setGeneratedByAndCopyPos(initExpression, source);
		initExpression.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		initExpression.type = type.build(node, source);
		initExpression.typeArguments = buildTypeArguments(node, source);
		initExpression.arguments = buildArray(args, new Expression[0], node, source);
		return initExpression;
	}
}
