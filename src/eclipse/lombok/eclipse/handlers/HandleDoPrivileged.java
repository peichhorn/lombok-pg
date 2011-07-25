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

import static lombok.ast.AST.*;
import static lombok.core.util.Arrays.*;
import static lombok.eclipse.handlers.Eclipse.*;

import java.util.*;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.handlers.DoPrivilegedHandler;
import lombok.eclipse.DeferUntilPostDiet;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.DoPrivileged} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class)
@DeferUntilPostDiet
public class HandleDoPrivileged extends EclipseAnnotationHandler<DoPrivileged> {

	@Override public void handle(AnnotationValues<DoPrivileged> annotation, Annotation source, EclipseNode annotationNode) {
		final Class<? extends java.lang.annotation.Annotation> annotationType = DoPrivileged.class;
		new EclipseDoPrivilegedHandler(annotationNode, source).handle(annotationType);
	}
	
	private static class EclipseDoPrivilegedHandler extends DoPrivilegedHandler<EclipseMethod> {
		public EclipseDoPrivilegedHandler(EclipseNode node, Annotation source) {
			super(EclipseMethod.methodOf(node, source), node);
		}

		@Override protected List<lombok.ast.Statement> sanitizeParameter(final EclipseMethod method) {
			final List<lombok.ast.Statement> sanitizeStatements = new ArrayList<lombok.ast.Statement>();
			if (isNotEmpty(method.get().arguments)) for (Argument argument : method.get().arguments) {
				final Annotation ann = getAnnotation(DoPrivileged.SanitizeWith.class, argument);
				if (ann != null) {
					final EclipseNode annotationNode = method.node().getNodeFor(ann);
					String sanatizeMethodName = Eclipse.createAnnotation(DoPrivileged.SanitizeWith.class, annotationNode).getInstance().value();
					final String argumentName = new String(argument.name);
					final String newArgumentName = "$" + argumentName;
					sanitizeStatements.add(LocalDecl(Type(argument.type), argumentName).withInitialization(Call(sanatizeMethodName).withArgument(Name(newArgumentName))));
					argument.name = newArgumentName.toCharArray();
					argument.modifiers |= Modifier.FINAL;
				}
			}
			return sanitizeStatements;
		}
	}
}
