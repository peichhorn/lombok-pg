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

import static lombok.eclipse.handlers.ast.ASTBuilder.*;
import static org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers.AccImplementing;
import static lombok.core.util.ErrorMessages.*;
import static lombok.eclipse.handlers.Eclipse.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;
import lombok.AutoGenMethodStub;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@link AutoGenMethodStub} annotation for eclipse using the {@link PatchAutoGenMethodStub}.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleAutoGenMethodStub implements EclipseAnnotationHandler<AutoGenMethodStub> {
	// error handling only
	@Override public void handle(final AnnotationValues<AutoGenMethodStub> annotation, final Annotation source, final EclipseNode annotationNode) {
		final EclipseNode typeNode = annotationNode.up();
		final TypeDeclaration typeDecl = typeDeclFiltering(typeNode, AccInterface | AccAnnotation);
		if (typeDecl == null) {
			annotationNode.addError(canBeUsedOnClassAndEnumOnly(AutoGenMethodStub.class));
		}
	}

	// real meat
	public MethodDeclaration handle(final MethodBinding abstractMethod, final Annotation annotation, final EclipseNode typeNode) {
		boolean throwException = false;
		for (final MemberValuePair pair : annotation.memberValuePairs()) {
			if ("throwException".equals(new String(pair.name))) {
				throwException = pair.value instanceof TrueLiteral;
			}
		}
		if (throwException) {
			return MethodDef(abstractMethod).withModifiers(AccImplementing).withStatement(Throw(New(Type("java.lang.UnsupportedOperationException")).withArgument(String("This method was not implemented yet.")))).injectInto(typeNode, annotation);
		} else {
			return MethodDef(abstractMethod).withModifiers(AccImplementing).withStatement(ReturnDefault()).injectInto(typeNode, annotation);
		}
	}
}