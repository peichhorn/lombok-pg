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

import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Arrays.*;
import static lombok.eclipse.handlers.Eclipse.*;
import static lombok.eclipse.handlers.ast.ASTBuilder.*;
import static org.eclipse.jdt.core.dom.Modifier.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;

import lombok.Singleton;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.mangosdk.spi.ProviderFor;

@ProviderFor(EclipseAnnotationHandler.class)
public class HandleSingleton implements EclipseAnnotationHandler<Singleton> {
	@Override public void handle(AnnotationValues<Singleton> annotation, Annotation source, EclipseNode annotationNode) {
		EclipseNode typeNode = annotationNode.up();
		TypeDeclaration typeDecl = typeDeclFiltering(typeNode, AccInterface | AccAnnotation | AccEnum);
		if (typeDecl == null) {
			annotationNode.addError(canBeUsedOnClassOnly(Singleton.class));
			return;
		}
		if (typeDecl.superclass != null) {
			annotationNode.addError(canBeUsedOnConcreteClassOnly(Singleton.class));
			return;
		}
		if (hasMultiArgumentConstructor(typeDecl)) {
			annotationNode.addError(requiresDefaultOrNoArgumentConstructor(Singleton.class));
			return;
		}
		
		Singleton singleton = annotation.getInstance();
		String typeName = typeNode.getName();
		
		switch(singleton.style()) {
		case HOLDER: {
			String holderName = typeName + "Holder";
			replaceConstructorVisibility(typeDecl);
			ClassDef(holderName).makePrivate().makeStatic() //
				.withField(FieldDef(Type(typeName), "INSTANCE").makePrivateFinal().makeStatic().withInitialization(New(Type(typeName)))).injectInto(typeNode, source);
			MethodDef(Type(typeName), "getInstance").makePublic().makeStatic() //
				.withStatement(Return(Name(holderName + ".INSTANCE"))).injectInto(typeNode, source);
			break;
		}
		default:
		case ENUM: {
			typeDecl.modifiers |= AccEnum;
			replaceConstructorVisibility(typeDecl);
			EnumConstant("INSTANCE").injectInto(typeNode, source);
			MethodDef(Type(typeName), "getInstance").makePublic().makeStatic() //
				.withStatement(Return(Name("INSTANCE"))).injectInto(typeNode, source);
		}
		}

		typeNode.rebuild();
	}

	private void replaceConstructorVisibility(TypeDeclaration type) {
		if (isNotEmpty(type.methods)) for (AbstractMethodDeclaration def : type.methods) {
			if (def instanceof ConstructorDeclaration) def.modifiers &= ~(PUBLIC | PROTECTED);
		}
	}

	private boolean hasMultiArgumentConstructor(TypeDeclaration type) {
		if (isNotEmpty(type.methods)) for (AbstractMethodDeclaration def : type.methods) {
			if ((def instanceof ConstructorDeclaration) && isNotEmpty(def.arguments)) return true;
		}
		return false;
	}
}
