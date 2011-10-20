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

import java.beans.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.DiagnosticsReceiver;
import lombok.core.util.As;

@RequiredArgsConstructor
public class BoundPropertySupportHandler<TYPE_TYPE extends IType<? extends IMethod<TYPE_TYPE, ?, ?, ?>, ?, ?, ?, ?, ?>> {
	private static final String PROPERTY_SUPPORT_FIELD_NAME = "propertySupport";
	private static final String LISTENER_ARG_NAME = "listener";
	private static final String[] PROPERTY_CHANGE_METHOD_NAMES = As.array("addPropertyChangeListener", "removePropertyChangeListener");

	private final TYPE_TYPE type;
	private final DiagnosticsReceiver diagnosticsReceiver;

	public void handle() {
		if (!type.isClass()) {
			diagnosticsReceiver.addError(canBeUsedOnClassOnly(BoundPropertySupport.class));
			return;
		}

		generatePropertyChangeSupportField(type);
		generateChangeListenerMethods(type);
	}

	private void generatePropertyChangeSupportField(final TYPE_TYPE type) {
		if (type.hasField(PROPERTY_SUPPORT_FIELD_NAME)) return;
		type.injectField(FieldDecl(Type(PropertyChangeSupport.class), PROPERTY_SUPPORT_FIELD_NAME).makePrivate().makeFinal() //
			.withInitialization(New(Type(PropertyChangeSupport.class)).withArgument(This())));
	}

	private void generateChangeListenerMethods(final TYPE_TYPE type) {
		for (String methodName : PROPERTY_CHANGE_METHOD_NAMES) {
			generateChangeListenerMethod(methodName, type);
		}
	}

	private void generateChangeListenerMethod(final String methodName, final TYPE_TYPE type) {
		if (type.hasMethod(methodName)) return;
		type.injectMethod(MethodDecl(Type("void"), methodName).makePublic().withArgument(Arg(Type(PropertyChangeListener.class), LISTENER_ARG_NAME)) //
			.withStatement(Call(Field(PROPERTY_SUPPORT_FIELD_NAME), methodName).withArgument(Name(LISTENER_ARG_NAME))));
	}
}
