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

import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.DiagnosticsReceiver;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacType;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

/**
 * Handles the {@code lombok.LazyGetter} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleLazyGetter extends JavacAnnotationHandler<LazyGetter> {

	@Override public void handle(AnnotationValues<LazyGetter> annotation, JCAnnotation ast, JavacNode annotationNode) {
		deleteAnnotationIfNeccessary(annotationNode, LazyGetter.class);
		deleteImportFromCompilationUnit(annotationNode, "lombok.AccessLevel");
		JavacType type = JavacType.typeOf(annotationNode, ast);
		Class<? extends java.lang.annotation.Annotation> annotationType = LazyGetter.class;
		LazyGetter annotationInstance = annotation.getInstance();
		createLazyGetterForField(type, annotationInstance.value(), annotationNode.up(), annotationNode, annotationType);
	}

	private void createLazyGetterForField(JavacType type, AccessLevel level, JavacNode fieldNode, DiagnosticsReceiver diagnosticsReceiver, Class<? extends java.lang.annotation.Annotation> annotationType) {
		if (fieldNode.getKind() != Kind.FIELD) {
			diagnosticsReceiver.addError(canBeUsedOnFieldOnly(annotationType));
			return;
		}

		JCVariableDecl fieldDecl = (JCVariableDecl)fieldNode.get();
		
		if ((fieldDecl.mods.flags & Flags.PRIVATE) == 0 || (fieldDecl.mods.flags & Flags.FINAL) == 0) {
			diagnosticsReceiver.addError(canBeUsedOnPrivateFinalFieldOnly(annotationType));
			return;
		}
		if (fieldDecl.init == null) {
			diagnosticsReceiver.addError(canBeUsedOnInitializedFieldOnly(annotationType));
			return;
		}

		String methodName = toGetterName(fieldDecl);

		for (String altName : toAllGetterNames(fieldDecl)) {
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

	private void createGetter(JavacType type, AccessLevel level, JCVariableDecl field, String methodName) {
		String fieldName = string(field.name);
		String initializedFieldName = "$" + fieldName + "Initialized";
		String lockFieldName = "$" + fieldName + "Lock";

		Expression init = Expr(field.init);
		field.init = null;
		field.mods.flags &= ~Flags.FINAL;

		type.injectField(FieldDecl(Type("boolean"), initializedFieldName).makePrivate().makeVolatile());
		type.injectField(FieldDecl(Type("java.lang.Object").withDimensions(1), lockFieldName).makePrivate().makeFinal() //
			.withInitialization(NewArray(Type("java.lang.Object")).withDimensionExpression(Number(0))));

		type.injectMethod(MethodDecl(Type(field.vartype), methodName).makePublic().withAccessLevel(level) //
			.withStatement(If(Not(Field(This(), initializedFieldName))).Then(Block() //
				.withStatement(Synchronized(Field(This(), lockFieldName)) //
					.withStatement(If(Not(Field(This(), initializedFieldName))).Then(Block() //
						.withStatement(Assign(Field(This(), fieldName), init)) //
						.withStatement(Assign(Field(This(), initializedFieldName), True()))))))) //
			.withStatement(Return(Field(This(), fieldName))));
	}
}
