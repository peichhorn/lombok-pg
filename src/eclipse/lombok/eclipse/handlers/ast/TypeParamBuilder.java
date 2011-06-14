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

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import static lombok.eclipse.handlers.ast.Arrays.buildArray;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class TypeParamBuilder implements StatementBuilder<TypeParameter>{
	protected final List<ExpressionBuilder<? extends TypeReference>> bounds = new ArrayList<ExpressionBuilder<? extends TypeReference>>();
	protected final String name;
	protected ExpressionBuilder<? extends TypeReference> type;

	public TypeParamBuilder withBound(final ExpressionBuilder<? extends TypeReference> bound) {
		bounds.add(bound);
		return this;
	}

	public TypeParamBuilder withType(final ExpressionBuilder<? extends TypeReference> type) {
		this.type = type;
		return this;
	}

	@Override
	public TypeParameter build(final EclipseNode node, final ASTNode source) {
		final TypeParameter typeParameter = new TypeParameter();
		typeParameter.type = type.build(node, source);
		typeParameter.name = name.toCharArray();
		typeParameter.bounds = buildArray(bounds, new TypeReference[0], node, source);
		setGeneratedByAndCopyPos(typeParameter, source);
		return typeParameter;
	}
}
