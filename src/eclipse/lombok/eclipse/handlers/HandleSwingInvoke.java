/*
 * Copyright © 2010-2011 Philipp Eichhorn
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

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.handlers.SwingInvokeHandler;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.SwingInvokeLater} and {@code lombok.SwingInvokeAndWait} annotation for eclipse.
 */
public class HandleSwingInvoke {

	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleSwingInvokeLater extends EclipseAnnotationHandler<SwingInvokeLater> {
		@Override public void handle(AnnotationValues<SwingInvokeLater> annotation, Annotation source, EclipseNode annotationNode) {
			new EclipseSwingInvokeHandler(annotationNode, source).generateSwingInvoke("invokeLater", SwingInvokeLater.class);
		}

		@Override
		public boolean deferUntilPostDiet() {
			return true;
		}
	}

	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleSwingInvokeAndWait extends EclipseAnnotationHandler<SwingInvokeAndWait> {
		@Override public void handle(AnnotationValues<SwingInvokeAndWait> annotation, Annotation source, EclipseNode annotationNode) {
			new EclipseSwingInvokeHandler(annotationNode, source).generateSwingInvoke("invokeAndWait", SwingInvokeAndWait.class);
		}

		@Override
		public boolean deferUntilPostDiet() {
			return true;
		}
	}

	private static class EclipseSwingInvokeHandler extends SwingInvokeHandler<EclipseMethod> {
		public EclipseSwingInvokeHandler(EclipseNode node, Annotation source) {
			super(EclipseMethod.methodOf(node, source), node);
		}

		protected void replaceWithQualifiedThisReference(final EclipseMethod method) {
			final IReplacementProvider<Expression> replacement = new QualifiedThisReplacementProvider(method.surroundingType());
			new ThisReferenceReplaceVisitor(replacement).visit(method.get());
		}
	}
}