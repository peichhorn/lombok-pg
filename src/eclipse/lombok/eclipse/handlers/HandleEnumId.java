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
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.eclipse.handlers.ast.EclipseASTUtil.boxedType;
import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.handlers.EnumIdHandler;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseMethod;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.EnumId} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleEnumId extends EclipseAnnotationHandler<EnumId> {

	@Override
	public void handle(final AnnotationValues<EnumId> annotation, final Annotation source, final EclipseNode annotationNode) {
		EclipseNode fieldNode = annotationNode.up();
		if (fieldNode.getKind() != Kind.FIELD) {
			annotationNode.addError(canBeUsedOnFieldOnly(EnumId.class));
			return;
		}
		FieldDeclaration fieldDecl = (FieldDeclaration) fieldNode.get();

		new EnumIdHandler<EclipseType, EclipseMethod>(EclipseType.typeOf(annotationNode, source), annotationNode).handle(string(fieldDecl.name), Type(fieldDecl.type),
				boxedType(fieldDecl.type));
	}
}
