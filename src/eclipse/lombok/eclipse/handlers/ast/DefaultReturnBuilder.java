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

import static lombok.eclipse.handlers.ast.ASTBuilder.Char;
import static lombok.eclipse.handlers.ast.ASTBuilder.False;
import static lombok.eclipse.handlers.ast.ASTBuilder.Null;
import static lombok.eclipse.handlers.ast.ASTBuilder.Number;
import static lombok.eclipse.handlers.ast.ASTBuilder.Return;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DefaultReturnBuilder implements StatementBuilder<ReturnStatement> {
	private ExpressionBuilder<? extends TypeReference> returnType;

	public DefaultReturnBuilder withReturnType(final ExpressionBuilder<? extends TypeReference> returnType) {
		this.returnType = returnType;
		return this;
	}

	@Override
	public ReturnStatement build(EclipseNode node, ASTNode source) {
		ReturnBuilder builder = Return(Null());
		if (returnType != null) {
			final TypeReference type = returnType.build(node, source);
			if (type instanceof SingleTypeReference) {
				final String name = new String(type.getLastToken());
				if ("int".equals(name)) {
					builder = Return(Number(Integer.valueOf(0)));
				} else if ("byte".equals(name)) {
					builder = Return(Number(Integer.valueOf(0)));
				} else if ("short".equals(name)) {
					builder = Return(Number(Integer.valueOf(0)));
				} else if ("char".equals(name)) {
					builder = Return(Char(""));
				} else if ("long".equals(name)) {
					builder = Return(Number(Long.valueOf(0)));
				} else if ("float".equals(name)) {
					builder = Return(Number(Float.valueOf(0)));
				} else if ("double".equals(name)) {
					builder = Return(Number(Double.valueOf(0)));
				} else if ("boolean".equals(name)) {
					builder = Return(False());
				} else if ("void".equals(name)) {
					builder = Return();
				}
			}
		}
		return builder.build(node, source);
	}
}
