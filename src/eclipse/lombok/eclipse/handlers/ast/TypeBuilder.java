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

import static lombok.core.util.Arrays.isNotEmpty;
import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.Eclipse.poss;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class TypeBuilder extends GenericTypeArgumentBuilder<TypeBuilder, TypeReference> {
	private final String typeName;
	private int dims;

	public TypeBuilder withDimensions(final int dims) {
		this.dims = dims;
		return this;
	}

	@Override
	public TypeReference build(final EclipseNode node, final ASTNode source) {
		final TypeReference[] paramTypes = buildTypeArguments(node, source);
		final TypeReference typeReference;
		if (typeName.equals("void")) {
			typeReference = new SingleTypeReference(TypeBinding.VOID.simpleName, 0);
		} else if (typeName.contains(".")) {
			final char[][] typeNameTokens = fromQualifiedName(typeName);
			if (isNotEmpty(paramTypes)) {
				final TypeReference[][] typeArguments = new TypeReference[typeNameTokens.length][];
				typeArguments[typeNameTokens.length - 1] = paramTypes;
				typeReference = new ParameterizedQualifiedTypeReference(typeNameTokens, typeArguments, 0, poss(source, typeNameTokens.length));
			} else {
				if (dims > 0) {
					typeReference = new ArrayQualifiedTypeReference(typeNameTokens, dims, poss(source, typeNameTokens.length));
				} else {
					typeReference = new QualifiedTypeReference(typeNameTokens, poss(source, typeNameTokens.length));
				}
			}
		} else {
			final char[] typeNameToken = typeName.toCharArray();
			if (isNotEmpty(paramTypes)) {
				typeReference = new ParameterizedSingleTypeReference(typeNameToken, paramTypes, 0, 0);
			} else {
				if (dims > 0) {
					typeReference = new ArrayTypeReference(typeNameToken, dims, 0);
				} else {
					typeReference = new SingleTypeReference(typeNameToken, 0);
				}
			}
		}
		setGeneratedByAndCopyPos(typeReference, source);
		return typeReference;
	}
}
