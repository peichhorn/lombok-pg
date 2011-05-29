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

import static lombok.eclipse.Eclipse.makeType;
import static org.eclipse.jdt.internal.compiler.ast.ASTNode.IsSuperType;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ResolutionBasedTypeBuilder implements ExpressionBuilder<TypeReference> {
	private final TypeBinding typeBinding;
	protected int bits;
	
	public ResolutionBasedTypeBuilder makeSuperType() {
		return withBits(IsSuperType);
	}
	
	public ResolutionBasedTypeBuilder withBits(final int bits) {
		this.bits |= bits;
		return this;
	}
	
	@Override
	public TypeReference build(final EclipseNode node, final ASTNode source) {
		final TypeReference typeReference = makeType(typeBinding, source, false);
		typeReference.bits |= bits;
		return typeReference;
	}
}
