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
import lombok.eclipse.DeferUntilPostDiet;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.mangosdk.spi.ProviderFor;

public class HandleSwingInvoke {

	/**
	 * Handles the {@code lombok.SwingInvokeLater} annotation for eclipse.
	 */
	@ProviderFor(EclipseAnnotationHandler.class)
	@DeferUntilPostDiet
	public static class HandleSwingInvokeLater extends EclipseAnnotationHandler<SwingInvokeLater> {
		@Override public void handle(final AnnotationValues<SwingInvokeLater> annotation, final Annotation source, final EclipseNode annotationNode) {
			new SwingInvokeHandler<EclipseMethod>(EclipseMethod.methodOf(annotationNode, source), annotationNode) //
				.handle("invokeLater", SwingInvokeLater.class, new EclipseParameterValidator(), new EclipseParameterSanitizer());
		}
	}

	/**
	 * Handles the {@code lombok.SwingInvokeAndWait} annotation for eclipse.
	 */
	@ProviderFor(EclipseAnnotationHandler.class)
	@DeferUntilPostDiet
	public static class HandleSwingInvokeAndWait extends EclipseAnnotationHandler<SwingInvokeAndWait> {
		@Override public void handle(final AnnotationValues<SwingInvokeAndWait> annotation, final Annotation source, final EclipseNode annotationNode) {
			new SwingInvokeHandler<EclipseMethod>(EclipseMethod.methodOf(annotationNode, source), annotationNode) //
				.handle("invokeAndWait", SwingInvokeAndWait.class, new EclipseParameterValidator(), new EclipseParameterSanitizer());
		}
	}
}