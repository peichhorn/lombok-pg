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

import static java.util.Collections.singletonList;
import static lombok.ast.AST.*;

import java.util.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.util.As;

public interface IParameterValidator<METHOD_TYPE extends IMethod<?, ?, ?, ?>> {
	public List<Statement<?>> validateParameterOf(METHOD_TYPE method);

	@RequiredArgsConstructor
	@Getter
	public enum ValidationStrategy {
		WITH(Validate.With.class) {
			@Override
			public List<? extends Statement<?>> getStatementsFor(final String argumentName, final int argumentIndex, final java.lang.annotation.Annotation annotation) {
				final List<Statement<?>> statements = new ArrayList<Statement<?>>();
				statements.addAll(NOT_NULL.getStatementsFor(argumentName, argumentIndex, annotation));
				statements.add(If(Not(Call(((Validate.With) annotation).value()).withArgument(Name(argumentName)))).Then(Block() //
					.withStatement(Throw(New(Type(IllegalArgumentException.class)).withArgument(formattedMessage("The object '%s' (argument #%s) is invalid", argumentName, argumentIndex))))));
				return statements;
			}
		},
		NOT_NULL(Validate.NotNull.class) {
			@Override
			public List<? extends Statement<?>> getStatementsFor(final String argumentName, final int argumentIndex, final java.lang.annotation.Annotation annotation) {
				return singletonList(If(Equal(Name(argumentName), Null())).Then(Block() //
					.withStatement(Throw(New(Type(NullPointerException.class)).withArgument(formattedMessage("The validated object '%s' (argument #%s) is null", argumentName, argumentIndex))))));
			}
		},
		NOT_EMPTY(Validate.NotEmpty.class) {
			@Override
			public List<? extends Statement<?>> getStatementsFor(final String argumentName, final int argumentIndex, final java.lang.annotation.Annotation annotation) {
				final List<Statement<?>> statements = new ArrayList<Statement<?>>();
				statements.addAll(NOT_NULL.getStatementsFor(argumentName, argumentIndex, annotation));
				statements.add(If(Call(Name(argumentName), "isEmpty")).Then(Block() //
					.withStatement(Throw(New(Type(IllegalArgumentException.class)).withArgument(formattedMessage("The validated object '%s' (argument #%s) is empty", argumentName, argumentIndex))))));
				return statements;
			}
		};

		public static final Iterable<ValidationStrategy> IN_ORDER = As.unmodifiableList(WITH, NOT_NULL, NOT_EMPTY);

		private final Class<? extends java.lang.annotation.Annotation> type;

		public abstract List<? extends Statement<?>> getStatementsFor(final String argumentName, final int argumentIndex, final java.lang.annotation.Annotation annotation);

		private static final Expression<?> formattedMessage(final String message, final String argumentName, final int argumentIndex) {
			return Call(Name(String.class), "format").withArgument(String(message)).withArgument(String(argumentName)).withArgument(Number(argumentIndex));
		}
	}
}
