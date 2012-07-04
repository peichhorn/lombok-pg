/*
 * Copyright © 2011-2012 Philipp Eichhorn
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
import lombok.core.handlers.BoundSetterHandler;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseField;
import lombok.eclipse.handlers.ast.EclipseType;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.BoundSetter} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleBoundSetter extends EclipseAnnotationHandler<BoundSetter> {

	@Override
	public void handle(final AnnotationValues<BoundSetter> annotation, final Annotation ast, final EclipseNode annotationNode) {
		BoundSetter annotationInstance = annotation.getInstance();
		new BoundSetterHandler<EclipseType, EclipseField, EclipseNode, ASTNode>(annotationNode, ast) {

			@Override
			protected EclipseType typeOf(EclipseNode node, ASTNode ast) {
				return EclipseType.typeOf(node, ast);
			}

			@Override
			protected EclipseField fieldOf(EclipseNode node, ASTNode ast) {
				return EclipseField.fieldOf(node, ast);
			}
		}.handle(annotationInstance.value(), annotationInstance.vetoable(), annotationInstance.throwVetoException());
	}
}
