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

import java.text.Normalizer;
import java.util.List;

import lombok.*;
import lombok.ast.*;
import lombok.core.util.As;

public interface IParameterSanitizer<METHOD_TYPE extends IMethod<?, ?, ?, ?>> {
	public List<Statement> sanitizeParameterOf(METHOD_TYPE method);

	@RequiredArgsConstructor
	@Getter
	public enum SanitizerStrategy {
		WITH(Sanitize.With.class) {
			@Override
			public Statement getStatementFor(final Object argumentType, final String argumentName, final String newArgumentName, final java.lang.annotation.Annotation annotation) {
				return LocalDecl(Type(argumentType), newArgumentName).makeFinal().withInitialization(Call(((Sanitize.With) annotation).value()).withArgument(Name(argumentName)));
			}
		},
		NORMALIZE(Sanitize.Normalize.class) {
			@Override
			public Statement getStatementFor(final Object argumentType, final String argumentName, final String newArgumentName, final java.lang.annotation.Annotation annotation) {
				final Normalizer.Form normalizerForm = ((Sanitize.Normalize) annotation).value();
				return LocalDecl(Type(argumentType), newArgumentName).makeFinal().withInitialization(Call(Name("java.text.Normalizer"), "normalize") //
					.withArgument(Name(argumentName)).withArgument(Name(String.format("java.text.Normalizer.Form.%s", normalizerForm.name()))));
			}
		};

		public static final Iterable<SanitizerStrategy> IN_ORDER = As.unmodifiableList(WITH, NORMALIZE);

		private final Class<? extends java.lang.annotation.Annotation> type;

		public abstract Statement getStatementFor(final Object argumentType, final String argumentName, final String newArgumentName,
				final java.lang.annotation.Annotation annotation);
	}
}
