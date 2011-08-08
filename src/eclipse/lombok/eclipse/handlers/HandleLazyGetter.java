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
import static lombok.core.handlers.TransformationsUtil.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.eclipse.Eclipse.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;

import lombok.*;
import lombok.ast.Expression;
import lombok.core.AnnotationValues;
import lombok.core.DiagnosticsReceiver;
import lombok.core.AST.Kind;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.LazyGetter} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleLazyGetter extends EclipseAnnotationHandler<LazyGetter> {

	public void handle(final AnnotationValues<LazyGetter> annotation, final Annotation ast, final EclipseNode annotationNode) {
		EclipseType type = EclipseType.typeOf(annotationNode, ast);
		Class<? extends java.lang.annotation.Annotation> annotationType = LazyGetter.class;
		LazyGetter annotationInstance = annotation.getInstance();
		createLazyGetterForField(type, annotationInstance.value(), annotationNode.up(), annotationNode, annotationType);
	}

	private void createLazyGetterForField(final EclipseType type, final AccessLevel level, final EclipseNode fieldNode, final DiagnosticsReceiver diagnosticsReceiver,
			final Class<? extends java.lang.annotation.Annotation> annotationType) {
		if (fieldNode.getKind() != Kind.FIELD) {
			diagnosticsReceiver.addError(canBeUsedOnFieldOnly(annotationType));
			return;
		}

		FieldDeclaration fieldDecl = (FieldDeclaration) fieldNode.get();

		if ((fieldDecl.modifiers & ClassFileConstants.AccPrivate) == 0 || (fieldDecl.modifiers & ClassFileConstants.AccFinal) == 0) {
			diagnosticsReceiver.addError(canBeUsedOnPrivateFinalFieldOnly(annotationType));
			return;
		}
		if (fieldDecl.initialization == null) {
			diagnosticsReceiver.addError(canBeUsedOnInitializedFieldOnly(annotationType));
			return;
		}

		TypeReference fieldType = copyType(fieldDecl.type, fieldDecl);
		String fieldName = string(fieldDecl.name);
		boolean isBoolean = nameEquals(fieldType.getTypeName(), "boolean") && fieldType.dimensions() == 0;
		String methodName = toGetterName(fieldName, isBoolean);

		for (String altName : toAllGetterNames(fieldName, isBoolean)) {
			switch (methodExists(altName, fieldNode, false)) {
			case EXISTS_BY_LOMBOK:
				return;
			case EXISTS_BY_USER:
				String altNameExpl = "";
				if (!altName.equals(methodName)) altNameExpl = String.format(" (%s)", altName);
				diagnosticsReceiver.addWarning(String.format("Not generating %s(): A method with that name already exists%s", methodName, altNameExpl));
				return;
			default:
			case NOT_EXISTS:
				//continue scanning the other alt names.
			}
		}

		createGetter(type, level, fieldDecl, methodName);
	}

	private void createGetter(final EclipseType type, final AccessLevel level, final FieldDeclaration field, final String methodName) {
		String fieldName = string(field.name);
		String initializedFieldName = "$" + fieldName + "Initialized";
		String lockFieldName = "$" + fieldName + "Lock";

		Expression init = Expr(field.initialization);
		field.initialization = null;
		field.modifiers &= ~ClassFileConstants.AccFinal;

		type.injectField(FieldDecl(Type("boolean"), initializedFieldName).makePrivate().makeVolatile());
		type.injectField(FieldDecl(Type("java.lang.Object").withDimensions(1), lockFieldName).makePrivate().makeFinal() //
			.withInitialization(NewArray(Type("java.lang.Object")).withDimensionExpression(Number(0))));

		type.injectMethod(MethodDecl(Type(field.type), methodName).makePublic().withAccessLevel(level) //
			.withStatement(If(Not(Field(This(), initializedFieldName))).Then(Block() //
				.withStatement(Synchronized(Field(This(), lockFieldName)) //
					.withStatement(If(Not(Field(This(), initializedFieldName))).Then(Block() //
						.withStatement(Assign(Field(This(), fieldName), init)) //
						.withStatement(Assign(Field(This(), initializedFieldName), True()))))))) //
			.withStatement(Return(Field(This(), fieldName))));
	}
}
