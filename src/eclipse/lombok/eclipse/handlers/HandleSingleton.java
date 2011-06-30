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
import static lombok.ast.AST.*;
import static org.eclipse.jdt.core.dom.Modifier.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;

import lombok.Singleton;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseMethod;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.mangosdk.spi.ProviderFor;

@ProviderFor(EclipseAnnotationHandler.class)
public class HandleSingleton extends EclipseAnnotationHandler<Singleton> {
	@Override public void handle(AnnotationValues<Singleton> annotation, Annotation source, EclipseNode annotationNode) {
		EclipseType type = EclipseType.typeOf(annotationNode, source);
		if (type.isAnnotation() || type.isInterface() || type.isEnum()) {
			annotationNode.addError(canBeUsedOnClassOnly(Singleton.class));
			return;
		}
		if (type.hasSuperClass()) {
			annotationNode.addError(canBeUsedOnConcreteClassOnly(Singleton.class));
			return;
		}
		if (type.hasMultiArgumentConstructor()) {
			annotationNode.addError(requiresDefaultOrNoArgumentConstructor(Singleton.class));
			return;
		}

		Singleton singleton = annotation.getInstance();
		String typeName = type.name();

		switch(singleton.style()) {
		case HOLDER:
			String holderName = typeName + "Holder";
			replaceConstructorVisibility(type);

			type.injectType(ClassDecl(holderName).makePrivate().makeStatic() //
				.withField(FieldDecl(Type(typeName), "INSTANCE").makePrivate().makeFinal().makeStatic().withInitialization(New(Type(typeName)))));
			type.injectMethod(MethodDecl(Type(typeName), "getInstance").makePublic().makeStatic() //
				.withStatement(Return(Name(holderName + ".INSTANCE"))));
			break;
		default:
		case ENUM:
			type.get().modifiers |= AccEnum;
			replaceConstructorVisibility(type);
			type.injectField(EnumConstant("INSTANCE"));
			type.injectMethod(MethodDecl(Type(typeName), "getInstance").makePublic().makeStatic() //
				.withStatement(Return(Name("INSTANCE"))));
		}

		type.rebuild();
	}

	private void replaceConstructorVisibility(EclipseType type) {
		for (EclipseMethod method : type.methods()) {
			if (method.isConstructor()) {
				method.get().modifiers &= ~(PUBLIC | PROTECTED);
			}
		}
	}
}
