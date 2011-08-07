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
package lombok.core.handlers;

import static lombok.ast.AST.*;
import static lombok.core.util.Lists.unmodifiableList;

import java.util.List;

import lombok.*;
import lombok.ast.*;

public interface IParameterValidator<METHOD_TYPE extends IMethod<?, ?, ?, ?>> {
	public List<lombok.ast.Statement> validateParameterOf(METHOD_TYPE method);

	@RequiredArgsConstructor
	@Getter
	public enum ValidationStrategy {
		WITH(Validate.With.class) {
			@Override
			public Statement getStatementFor(final String argumentName, final String validateMethodName) {
				return If(Not(Call(validateMethodName).withArgument(Name(argumentName)))).Then(Block() //
					.withStatement(Throw(New(Type("java.lang.IllegalArgumentException")).withArgument(String("The validated expression is false")))));
			}
		},
		NOT_NULL(Validate.NotNull.class) {
			@Override
			public Statement getStatementFor(final String argumentName, final String validateMethodName) {
				return If(Equal(Name(argumentName), Null())).Then(Block() //
					.withStatement(Throw(New(Type("java.lang.IllegalArgumentException")).withArgument(String("The validated object is null")))));
			}
		},
		NOT_EMPTY(Validate.NotEmpty.class) {
			@Override
			public Statement getStatementFor(final String argumentName, final String validateMethodName) {
				return If(Or(Equal(Name(argumentName), Null()), Call(Name(argumentName), "isEmpty"))).Then(Block() //
					.withStatement(Throw(New(Type("java.lang.IllegalArgumentException")).withArgument(String("The validated object is empty")))));
			}
		};

		public final static Iterable<ValidationStrategy> IN_ORDER = unmodifiableList(WITH, NOT_NULL, NOT_EMPTY);

		private final Class<? extends java.lang.annotation.Annotation> type;

		public abstract lombok.ast.Statement getStatementFor(final String argumentName, final String validateMethodName);
	}
}
