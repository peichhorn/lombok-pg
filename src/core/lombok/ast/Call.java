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

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class Call extends Expression {
	private final List<Expression> args = new ArrayList<Expression>();
	private final List<TypeRef> typeArgs = new ArrayList<TypeRef>();
	private final Expression receiver;
	private final String name;

	public Call(final Expression receiver, final String name) {
		this.receiver = child(receiver);
		this.name = name;
	}
	
	public Call(final String name) {
		this(new This().implicit(), name);
	}

	public Call withArgument(final Expression argument) {
		args.add(child(argument));
		return this;
	}

	public Call withArguments(final List<Expression> arguments) {
		for (Expression argument : arguments) withArgument(argument);
		return this;
	}

	public Call withTypeArgument(final TypeRef typeArg) {
		typeArgs.add(child(typeArg));
		return this;
	}
	
	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitCall(this, p);
	}
}
