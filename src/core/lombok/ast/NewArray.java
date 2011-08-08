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
public final class NewArray extends Expression {
	// remember to add one dimension after all so you end up with: a[exp1][exp2][exp3][]
	private final List<Expression> dimensionExpressions = new ArrayList<Expression>();
	private final List<Expression> initializerExpressions = new ArrayList<Expression>();
	private final TypeRef type;
	private final int dimensions;

	public NewArray(final TypeRef type, final int dimensions) {
		this.type = child(type);
		this.dimensions = dimensions;
	}

	public NewArray(final TypeRef type) {
		this(type, 1);
	}

	public NewArray withDimensionExpression(final Expression dimensionExpression) {
		dimensionExpressions.add(child(dimensionExpression));
		return this;
	}

	public NewArray withInitializerExpression(final Expression initializerExpression) {
		initializerExpressions.add(child(initializerExpression));
		return this;
	}

	public NewArray withInitializerExpressions(final List<Expression> initializerExpressions) {
		for (Expression initializerExpression : initializerExpressions) withInitializerExpression(initializerExpression);
		return this;
	}

	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(final ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, final PARAMETER_TYPE p) {
		return v.visitNewArray(this, p);
	}
}
