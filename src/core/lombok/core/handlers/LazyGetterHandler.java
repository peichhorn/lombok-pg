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
import static lombok.core.TransformationsUtil.*;
import static lombok.core.util.ErrorMessages.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.DiagnosticsReceiver;

@RequiredArgsConstructor
public class LazyGetterHandler<TYPE_TYPE extends IType<? extends IMethod<TYPE_TYPE, ?, ?, ?>, ?, ?, ?, ?, ?>, FIELD_TYPE extends IField<?, ?, ?>> {
	private final TYPE_TYPE type;
	private final FIELD_TYPE field;
	private final DiagnosticsReceiver diagnosticsReceiver;

	public void handle(final AccessLevel level) {
		if (field == null) {
			diagnosticsReceiver.addError(canBeUsedOnFieldOnly(LazyGetter.class));
			return;
		}
		if (!field.isFinal() && !field.isPrivate()) {
			diagnosticsReceiver.addError(canBeUsedOnPrivateFinalFieldOnly(LazyGetter.class));
			return;
		}
		if (!field.isInitialized()) {
			diagnosticsReceiver.addError(canBeUsedOnInitializedFieldOnly(LazyGetter.class));
			return;
		}

		String fieldName = field.name();
		boolean isBoolean = field.isOfType("boolean");
		String methodName = toGetterName(fieldName, isBoolean);

		for (String altName : toAllGetterNames(fieldName, isBoolean)) {
			if (type.hasMethod(altName)) return;
		}

		createGetter(type, field, level, methodName);
	}

	private void createGetter(final TYPE_TYPE type, final FIELD_TYPE field, final AccessLevel level, final String methodName) {
		String fieldName = field.name();
		String initializedFieldName = "$" + fieldName + "Initialized";
		String lockFieldName = "$" + fieldName + "Lock";

		type.injectField(FieldDecl(Type("boolean"), initializedFieldName).makePrivate().makeVolatile());
		type.injectField(FieldDecl(Type(Object.class).withDimensions(1), lockFieldName).makePrivate().makeFinal() //
				.withInitialization(NewArray(Type(Object.class)).withDimensionExpression(Number(0))));

		type.injectMethod(MethodDecl(field.type(), methodName).withAccessLevel(level) //
				.withStatement(If(Not(Field(initializedFieldName))).Then(Block() //
						.withStatement(Synchronized(Field(lockFieldName)) //
								.withStatement(If(Not(Field(initializedFieldName))).Then(Block() //
										.withStatement(Assign(Field(fieldName), field.initialization())) //
										.withStatement(Assign(Field(initializedFieldName), True()))))))) //
				.withStatement(Return(Field(fieldName))));

		field.replaceInitialization(null);
		field.makeNonFinal();
	}
}
