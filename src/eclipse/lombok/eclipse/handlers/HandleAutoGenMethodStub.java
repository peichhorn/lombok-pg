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

import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import lombok.*;
import lombok.ast.*;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@link AutoGenMethodStub} annotation for eclipse using the {@link PatchAutoGenMethodStub}.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleAutoGenMethodStub extends EclipseAnnotationHandler<AutoGenMethodStub> {
	// error handling only
	@Override public void handle(final AnnotationValues<AutoGenMethodStub> annotation, final Annotation source, final EclipseNode annotationNode) {
		final EclipseType type = EclipseType.typeOf(annotationNode, source);
		if (type.isInterface() || type.isAnnotation()) {
			annotationNode.addError(canBeUsedOnClassAndEnumOnly(AutoGenMethodStub.class));
		}
	}

	// real meat
	public MethodDeclaration handle(final MethodBinding abstractMethod, final AnnotationValues<AutoGenMethodStub> annotation, final Annotation source, final EclipseNode annotationNode) {
		final EclipseType type = EclipseType.typeOf(annotationNode, source);
		final Statement statement;
		if (annotation.getInstance().throwException()) {
			statement = Throw(New(Type("java.lang.UnsupportedOperationException")).withArgument(String("This method is not implemented yet.")));
		} else {
			statement = ReturnDefault();
		}
		MethodDeclaration method = (MethodDeclaration) type.injectMethod(MethodDecl(abstractMethod).implementing().withStatement(statement));
		
		type.rebuild();

		return method;
	}
}