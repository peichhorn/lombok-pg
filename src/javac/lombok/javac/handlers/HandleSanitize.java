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
package lombok.javac.handlers;

import static lombok.core.util.ErrorMessages.*;
import static lombok.javac.handlers.JavacHandlerUtil.deleteAnnotationIfNeccessary;

import com.sun.tools.javac.tree.JCTree.JCAnnotation;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacMethod;

import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.Sanitize} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleSanitize extends JavacAnnotationHandler<Sanitize> {

	@Override
	public void handle(AnnotationValues<Sanitize> annotation, JCAnnotation source, JavacNode annotationNode) {
		final Class<? extends java.lang.annotation.Annotation> annotationType = Sanitize.class;
		deleteAnnotationIfNeccessary(annotationNode, annotationType);
		final JavacMethod method = JavacMethod.methodOf(annotationNode, source);
		if (method == null) {
			annotationNode.addError(canBeUsedOnMethodOnly(annotationType));
			return;
		}

		if (method.isAbstract() || method.isEmpty()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return;
		}

		final JavacParameterSanitizer sanitizer = new JavacParameterSanitizer();
		sanitizer.sanitizeParameterOf(method);
	}
}

