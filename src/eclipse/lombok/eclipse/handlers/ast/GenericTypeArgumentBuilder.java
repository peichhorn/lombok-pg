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

import static lombok.eclipse.handlers.ast.Arrays.buildArray;

import java.util.ArrayList;
import java.util.List;

import lombok.core.util.Cast;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

abstract class GenericTypeArgumentBuilder<SELF_TYPE extends GenericTypeArgumentBuilder<SELF_TYPE, NODE_TYPE>, NODE_TYPE extends Expression> implements ExpressionBuilder<NODE_TYPE> {
	protected final List<ExpressionBuilder<? extends TypeReference>> typeArgs = new ArrayList<ExpressionBuilder<? extends TypeReference>>();
	
	protected TypeReference[] buildTypeArguments(final EclipseNode node, final ASTNode source) {
		return buildArray(typeArgs, new TypeReference[0], node, source);
	}
	
	protected final SELF_TYPE self() {
		return Cast.<SELF_TYPE>uncheckedCast(this);
	}
	
	public SELF_TYPE withTypeArgument(final ExpressionBuilder<? extends TypeReference> typeArg) {
		typeArgs.add(typeArg);
		return self();
	}
	
	public SELF_TYPE withTypeArguments(final List<ExpressionBuilder<? extends TypeReference>> typeArgs) {
		this.typeArgs.addAll(typeArgs);
		return self();
	}
}
