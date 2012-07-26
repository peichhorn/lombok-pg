/*
 * Copyright © 2011-2012 Philipp Eichhorn
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
public class EnumIdHandler<TYPE_TYPE extends IType<?, ?, ?, ?, ?, ?>, FIELD_TYPE extends IField<?, ?, ?, ?>> {
	private final TYPE_TYPE type;
	private final FIELD_TYPE field;
	private final DiagnosticsReceiver diagnosticsReceiver;

	public void handle() {
		if (!type.isEnum()) {
			diagnosticsReceiver.addError(canBeUsedOnEnumFieldsOnly(EnumId.class));
			return;
		}

		String filteredFieldName = field.filteredName();
		String lookupFieldName = "$" + camelCaseToConstant(camelCase(filteredFieldName, "lookup"));
		String foreachVarName = decapitalize(type.name());
		String exceptionText = "Enumeration '" + type.name() + "' has no value for '" + filteredFieldName + " = %s'";
		type.editor().injectField(FieldDecl(Type(Map.class).withTypeArgument(field.boxedType()).withTypeArgument(Type(type.name())), lookupFieldName).makePrivate().makeStatic().makeFinal() //
				.withInitialization(New(Type(HashMap.class).withTypeArgument(field.boxedType()).withTypeArgument(Type(type.name())))));

		type.editor().injectInitializer(Initializer().makeStatic().withStatement(Foreach(LocalDecl(Type(type.name()), foreachVarName)).In(Call(Name(type.name()), "values")).Do(Block() //
				.withStatement(Call(Name(lookupFieldName), "put").withArgument(Field(Name(foreachVarName), field.name())).withArgument(Name(foreachVarName))))));

		type.editor().injectMethod(MethodDecl(Type(type.name()), camelCase("find", "by", filteredFieldName)).makePublic().makeStatic().withArgument(Arg(field.type(), filteredFieldName)) //
				.withStatement(If(Call(Name(lookupFieldName), "containsKey").withArgument(Name(filteredFieldName))).Then(Block().withStatement(Return(Call(Name(lookupFieldName), "get").withArgument(Name(filteredFieldName)))))) //
				.withStatement(Throw(New(Type(IllegalArgumentException.class)).withArgument(Call(Name(String.class), "format").withArgument(String(exceptionText)).withArgument(Name(filteredFieldName))))));
	}
}
