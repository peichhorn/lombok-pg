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

import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.Eclipse.poss;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class NameBuilder implements ExpressionBuilder<NameReference> {
	private final String name;

	@Override
	public NameReference build(final EclipseNode node, final ASTNode source) {
		final NameReference nameReference;
		if (name.contains(".")) {
			char[][] nameTokens = fromQualifiedName(name);
			nameReference = new QualifiedNameReference(nameTokens, poss(source, nameTokens.length), 0, 0);
		} else {
			nameReference = new SingleNameReference(name.toCharArray(), 0);
		}
		setGeneratedByAndCopyPos(nameReference, source);
		return nameReference;
	}
}
