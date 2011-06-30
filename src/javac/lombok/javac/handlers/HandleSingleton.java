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
package lombok.javac.handlers;

import static com.sun.tools.javac.code.Flags.*;
import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import com.sun.tools.javac.tree.JCTree.JCAnnotation;

import lombok.Singleton;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacMethod;
import lombok.javac.handlers.ast.JavacType;

import org.mangosdk.spi.ProviderFor;

@ProviderFor(JavacAnnotationHandler.class)
public class HandleSingleton extends JavacAnnotationHandler<Singleton> {

	@Override public void handle(AnnotationValues<Singleton> annotation, JCAnnotation source, JavacNode annotationNode) {
		deleteAnnotationIfNeccessary(annotationNode, Singleton.class);

		JavacType type = JavacType.typeOf(annotationNode, source);
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
			type.get().mods.flags |= ENUM;
			replaceConstructorVisibility(type);

			type.injectField(EnumConstant("INSTANCE"));
			type.injectMethod(MethodDecl(Type(typeName), "getInstance").makePublic().makeStatic() //
				.withStatement(Return(Name("INSTANCE"))));
		}

		type.rebuild();
	}

	private void replaceConstructorVisibility(JavacType type) {
		for (JavacMethod method : type.methods()) {
			if (method.isConstructor()) {
				method.get().mods.flags &= ~(PUBLIC | PROTECTED);
			}
		}
	}
}