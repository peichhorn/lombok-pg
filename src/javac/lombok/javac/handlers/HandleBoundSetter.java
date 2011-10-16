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

import static lombok.javac.handlers.Javac.deleteImport;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.handlers.BoundSetterHandler;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacField;
import lombok.javac.handlers.ast.JavacType;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;

/**
 * Handles the {@code lombok.BoundSetter} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleBoundSetter extends JavacAnnotationHandler<BoundSetter> {

	@Override
	public void handle(final AnnotationValues<BoundSetter> annotation, final JCAnnotation ast, final JavacNode annotationNode) {
		BoundSetter annotationInstance = annotation.getInstance();
		new BoundSetterHandler<JavacType, JavacField, JavacNode, JCTree>(annotationNode, ast) {

			@Override protected JavacType typeOf(JavacNode node, JCTree ast) {
				return JavacType.typeOf(node, ast);
			}

			@Override protected JavacField fieldOf(JavacNode node, JCTree ast) {
				return JavacField.fieldOf(node, ast);
			}
		}.handle(annotationInstance.value(), annotationInstance.getClass());
		deleteAnnotationIfNeccessary(annotationNode, BoundSetter.class);
		deleteImport(annotationNode, AccessLevel.class);
	}
}
