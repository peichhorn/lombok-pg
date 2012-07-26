/*
 * Copyright © 2012 Philipp Eichhorn
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

import lombok.*;
import lombok.ast.*;
import lombok.core.AnnotationValues;

@RequiredArgsConstructor
public class WarningHandler<METHOD_TYPE extends IMethod<?, ?, ?, ?>> {
	private final METHOD_TYPE method;

	public void handle(final AnnotationValues<Warning> annotation) {
		Warning warning = annotation.getInstance();
		final StringBuilder message = new StringBuilder("in ");
		if (method.isConstructor()) {
			message.append("construcor");
		} else {
			message.append("method ");
		}
		message.append(method.surroundingType().qualifiedName()).append(".").append(method.name()).append("(");
		if (method.hasArguments()) message.append("..");
		message.append("): \"").append(warning.value()).append("\"");
		method.node().addWarning(message.toString());
	}
}
