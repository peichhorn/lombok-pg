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
package lombok.eclipse.handlers;

import static lombok.core.util.Arrays.isNotEmpty;
import static lombok.eclipse.Eclipse.*;
import static lombok.eclipse.handlers.Eclipse.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;

import lombok.core.handlers.IParameterValidator;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseMethod;

public class EclipseParameterValidator implements IParameterValidator<EclipseMethod> {
	@Override
	public List<lombok.ast.Statement> validateParameterOf(final EclipseMethod method) {
		final List<lombok.ast.Statement> validateStatements = new ArrayList<lombok.ast.Statement>();
		if (isNotEmpty(method.get().arguments)) for (Argument argument : method.get().arguments) {
			final String argumentName = new String(argument.name);
			for (ValidationStrategy validationStrategy : ValidationStrategy.IN_ORDER) {
				final Annotation ann = getAnnotation(validationStrategy.getType(), argument.annotations);
				if (ann == null) continue;
				if (isGenerated(ann)) continue;
				final EclipseNode annotationNode = method.node().getNodeFor(ann);
				final java.lang.annotation.Annotation annotation = Eclipse.createAnnotation(validationStrategy.getType(), annotationNode).getInstance();
				validateStatements.add(validationStrategy.getStatementFor(argumentName, annotation));
				setGeneratedBy(ann, ann);
				argument.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
				break;
			}
		}
		return validateStatements;
	}
}
