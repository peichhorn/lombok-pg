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
package lombok.ast;

import java.util.*;

import lombok.*;

@Getter
public final class New extends Expression<New> {
	private final List<Expression<?>> args = new ArrayList<Expression<?>>();
	private final List<TypeRef> typeArgs = new ArrayList<TypeRef>();
	private final TypeRef type;
	private ClassDecl anonymousType;

	public New(final TypeRef type) {
		this.type = child(type);
	}

	public New withArgument(final Expression<?> arg) {
		args.add(child(arg));
		return this;
	}

	public New withTypeArgument(final TypeRef typeArg) {
		typeArgs.add(child(typeArg));
		return this;
	}

	public New withTypeArguments(final List<TypeRef> typeArgs) {
		for (TypeRef typeArg : typeArgs) withTypeArgument(typeArg);
		return this;
	}

	public New withTypeDeclaration(final ClassDecl anonymousType) {
		this.anonymousType = child(anonymousType);
		return this;
	}

	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(final ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, final PARAMETER_TYPE p) {
		return v.visitNew(this, p);
	}
}
