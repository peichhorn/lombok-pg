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
package lombok.core.handlers;

import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.DiagnosticsReceiver;

@RequiredArgsConstructor
public final class SingletonHandler<TYPE_TYPE extends IType<METHOD_TYPE, ?, ?, ?, ?, ?>, METHOD_TYPE extends IMethod<TYPE_TYPE, ?, ?, ?>> {
	private final TYPE_TYPE type;
	private final DiagnosticsReceiver diagnosticsReceiver;
	
	public void handle(final Singleton.Style style) {
		if (type.isAnnotation() || type.isInterface() || type.isEnum()) {
			diagnosticsReceiver.addError(canBeUsedOnClassOnly(Singleton.class));
			return;
		}
		if (type.hasSuperClass()) {
			diagnosticsReceiver.addError(canBeUsedOnConcreteClassOnly(Singleton.class));
			return;
		}
		if (type.hasMultiArgumentConstructor()) {
			diagnosticsReceiver.addError(requiresDefaultOrNoArgumentConstructor(Singleton.class));
			return;
		}

		String typeName = type.name();

		switch(style) {
		case HOLDER:
			String holderName = typeName + "Holder";
			replaceConstructorVisibility();

			type.injectType(ClassDecl(holderName).makePrivate().makeStatic() //
				.withField(FieldDecl(Type(typeName), "INSTANCE").makePrivate().makeFinal().makeStatic().withInitialization(New(Type(typeName)))));
			type.injectMethod(MethodDecl(Type(typeName), "getInstance").makePublic().makeStatic() //
				.withStatement(Return(Name(holderName + ".INSTANCE"))));
			break;
		default:
		case ENUM:
			type.makeEnum();
			replaceConstructorVisibility();

			type.injectField(EnumConstant("INSTANCE"));
			type.injectMethod(MethodDecl(Type(typeName), "getInstance").makePublic().makeStatic() //
				.withStatement(Return(Name("INSTANCE"))));
		}

		type.rebuild();
	}

	private void replaceConstructorVisibility() {
		for (METHOD_TYPE method : type.methods()) {
			if (method.isConstructor()) method.makePackagePrivate();
		}
	}
}
