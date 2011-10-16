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
import static lombok.core.util.Names.*;

import java.util.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.DiagnosticsReceiver;

@RequiredArgsConstructor
public class EnumIdHandler<TYPE_TYPE extends IType<METHOD_TYPE, ?, ?, ?, ?, ?>, METHOD_TYPE extends IMethod<TYPE_TYPE, ?, ?, ?>> {
	private final TYPE_TYPE type;
	private final DiagnosticsReceiver diagnosticsReceiver;

	public void handle(String fieldName, TypeRef fieldType, TypeRef boxedType) {
		if (!type.isEnum()) {
			diagnosticsReceiver.addError(canBeUsedOnEnumFieldsOnly(EnumId.class));
			return;
		}

		String lookupFieldName = "$" + camelCaseToConstant(camelCase(fieldName, "lookup"));
		String foreachVarName = decapitalize(type.name());
		String exceptionText = "Enumeration '" + type.name() + "' has no value '%s'";
		type.injectField(FieldDecl(Type(Map.class).withTypeArgument(boxedType).withTypeArgument(Type(type.name())), lookupFieldName).makePrivate().makeStatic().makeFinal() //
			.withInitialization(New(Type(HashMap.class).withTypeArgument(boxedType).withTypeArgument(Type(type.name())))));

		type.injectInitializer(Initializer().makeStatic().withStatement(Foreach(LocalDecl(Type(type.name()), foreachVarName)).In(Call(Name(type.name()), "values")).Do(Block() //
			.withStatement(Call(Name(lookupFieldName), "put").withArgument(Field(Name(foreachVarName), fieldName)).withArgument(Name(foreachVarName))))));

		type.injectMethod(MethodDecl(Type(type.name()), camelCase("find", "by", fieldName)).makePublic().makeStatic().withArgument(Arg(fieldType, fieldName)) //
			.withStatement(If(Call(Name(lookupFieldName), "containsKey").withArgument(Name(fieldName))).Then(Block().withStatement(Return(Call(Name(lookupFieldName), "get").withArgument(Name(fieldName)))))) //
			.withStatement(Throw(New(Type(IllegalArgumentException.class)).withArgument(Call(Name(String.class), "format").withArgument(String(exceptionText)).withArgument(Name(fieldName))))));
	}
}