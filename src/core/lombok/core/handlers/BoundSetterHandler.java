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
import static lombok.core.TransformationsUtil.toSetterName;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.camelCaseToConstant;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.*;
import lombok.ast.*;
import lombok.core.AnnotationValues;
import lombok.core.LombokNode;
import lombok.core.AST.Kind;
import lombok.core.TransformationsUtil;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
public abstract class BoundSetterHandler<TYPE_TYPE extends IType<?, FIELD_TYPE, ?, ?, ?, ?>, FIELD_TYPE extends IField<?, ?, ?>, LOMBOK_NODE_TYPE extends LombokNode<?, LOMBOK_NODE_TYPE, ?>, SOURCE_TYPE> {
	private static final String PROPERTY_CHANGE_SUPPORT_METHOD_NAME = "getPropertyChangeSupport";
	private static final String FIRE_PROPERTY_CHANGE_METHOD_NAME = "firePropertyChange";
	private static final String OLD_VALUE_VARIABLE_NAME = "old";
	private static final Pattern SETTER_PATTERN = Pattern.compile("^(?:setter|fluentsetter|boundsetter)$", Pattern.CASE_INSENSITIVE);

	private final LOMBOK_NODE_TYPE annotationNode;
	private final SOURCE_TYPE ast;

	public void handle(final AccessLevel level) {
		LOMBOK_NODE_TYPE mayBeField = annotationNode.up();
		if (mayBeField == null) return;
		TYPE_TYPE type = typeOf(annotationNode, ast);
		List<FIELD_TYPE> fields = new ArrayList<FIELD_TYPE>();
		if (mayBeField.getKind() == Kind.FIELD) {
			for (LOMBOK_NODE_TYPE node : annotationNode.upFromAnnotationToFields()) {
				fields.add(fieldOf(node, ast));
			}
		} else if (mayBeField.getKind() == Kind.TYPE) {
			for (FIELD_TYPE field : type.fields()) {
				if (!field.annotations(SETTER_PATTERN).isEmpty()) continue;
				if (field.name().startsWith("$")) continue;
				if (field.isFinal()) continue;
				if (field.isStatic()) continue;
				fields.add(field);
			}
		} else {
			annotationNode.addError(canBeUsedOnClassAndFieldOnly(BoundSetter.class));
			return;
		}
		generateSetter(type, fields, level);
	}

	protected abstract TYPE_TYPE typeOf(final LOMBOK_NODE_TYPE node, final SOURCE_TYPE ast);

	protected abstract FIELD_TYPE fieldOf(final LOMBOK_NODE_TYPE node, final SOURCE_TYPE ast);

	private void generateSetter(final TYPE_TYPE type, final List<FIELD_TYPE> fields, final AccessLevel level) {
		for (FIELD_TYPE field : fields) {
			String propertyNameFieldName = "PROP_" + camelCaseToConstant(field.name());
			generatePropertyNameConstant(type, field, propertyNameFieldName);
			generateSetter(type, field, level, propertyNameFieldName);
		}
	}

	private void generatePropertyNameConstant(final TYPE_TYPE type, final FIELD_TYPE field, final String propertyNameFieldName) {
		String propertyName = field.name();
		if (type.hasField(propertyNameFieldName)) return;
		type.injectField(FieldDecl(Type(String.class), propertyNameFieldName).makePublic().makeStatic().makeFinal() //
				.withInitialization(String(propertyName)));
	}

	private void generateSetter(final TYPE_TYPE type, final FIELD_TYPE field, final AccessLevel level, final String propertyNameFieldName) {
		String fieldName = field.name();
		boolean isBoolean = field.isOfType("boolean");
		AnnotationValues<Accessors> accessors = AnnotationValues.of(Accessors.class, field.node());
		String setterName = toSetterName(accessors, fieldName, isBoolean);
		if (type.hasMethod(setterName, 1)) return;
		String oldValueName = OLD_VALUE_VARIABLE_NAME;
		List<lombok.ast.Annotation> nonNulls = field.annotations(TransformationsUtil.NON_NULL_PATTERN);
		MethodDecl methodDecl = MethodDecl(Type("void"), setterName).withAccessLevel(level).withArgument(Arg(field.type(), fieldName).withAnnotations(nonNulls));
		if (!nonNulls.isEmpty() && !field.isPrimitive()) {
			methodDecl.withStatement(If(Equal(Name(fieldName), Null())).Then(Throw(New(Type(NullPointerException.class)).withArgument(String(fieldName)))));
		}
		methodDecl.withStatement(LocalDecl(field.type(), oldValueName).makeFinal().withInitialization(Field(fieldName))) //
				.withStatement(Assign(Field(fieldName), Name(fieldName))) //
				.withStatement(Call(Call(PROPERTY_CHANGE_SUPPORT_METHOD_NAME), FIRE_PROPERTY_CHANGE_METHOD_NAME) //
						.withArgument(Name(propertyNameFieldName)).withArgument(Name(oldValueName)).withArgument(Field(fieldName)));
		type.injectMethod(methodDecl);
	}
}
