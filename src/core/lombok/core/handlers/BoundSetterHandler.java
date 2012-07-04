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
import static lombok.core.TransformationsUtil.toSetterName;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.camelCaseToConstant;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.*;
import lombok.ast.*;
import lombok.core.AnnotationValues;
import lombok.core.LombokNode;
import lombok.core.AST.Kind;
import lombok.core.util.As;
import lombok.core.TransformationsUtil;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
public abstract class BoundSetterHandler<TYPE_TYPE extends IType<?, FIELD_TYPE, ?, ?, ?, ?>, FIELD_TYPE extends IField<?, ?, ?>, LOMBOK_NODE_TYPE extends LombokNode<?, LOMBOK_NODE_TYPE, ?>, SOURCE_TYPE> {
	private static final String PROPERTY_CHANGE_SUPPORT_FIELD_NAME = "$propertyChangeSupport";
	private static final String VETOABLE_CHANGE_SUPPORT_FIELD_NAME = "$vetoableChangeSupport";
	private static final String PROPERTY_CHANGE_SUPPORT_METHOD_NAME = "getPropertyChangeSupport";
	private static final String VETOABLE_CHANGE_SUPPORT_METHOD_NAME = "getVetoableChangeSupport";
	private static final String LISTENER_ARG_NAME = "listener";
	private static final String[] PROPERTY_CHANGE_METHOD_NAMES = As.array("addPropertyChangeListener", "removePropertyChangeListener");
	private static final String[] VETOABLE_CHANGE_METHOD_NAMES = As.array("addVetoableChangeListener", "removeVetoableChangeListener");
	private static final String FIRE_PROPERTY_CHANGE_METHOD_NAME = "firePropertyChange";
	private static final String FIRE_VETOABLE_CHANGE_METHOD_NAME = "fireVetoableChange";
	private static final String OLD_VALUE_VARIABLE_NAME = "$old";
	private static final String E_VALUE_VARIABLE_NAME = "$e";
	private static final Pattern SETTER_PATTERN = Pattern.compile("^(?:setter|fluentsetter|boundsetter)$", Pattern.CASE_INSENSITIVE);

	private final LOMBOK_NODE_TYPE annotationNode;
	private final SOURCE_TYPE ast;

	public void handle(final AccessLevel level, final boolean vetoable, final boolean throwVetoException) {
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
		generateSetter(type, fields, level, vetoable | throwVetoException, throwVetoException);
	}

	protected abstract TYPE_TYPE typeOf(final LOMBOK_NODE_TYPE node, final SOURCE_TYPE ast);

	protected abstract FIELD_TYPE fieldOf(final LOMBOK_NODE_TYPE node, final SOURCE_TYPE ast);

	private void generateSetter(final TYPE_TYPE type, final List<FIELD_TYPE> fields, final AccessLevel level, final boolean vetoable, final boolean throwVetoException) {
		if (!fields.isEmpty()) {
			if (!type.hasMethod(PROPERTY_CHANGE_SUPPORT_METHOD_NAME, 0)) {
				generatePropertyChangeSupportFields(type);
				generateGetPropertySupportMethod(type);
				generatePropertyChangeListenerMethods(type);
			}
			if (vetoable && !type.hasMethod(VETOABLE_CHANGE_SUPPORT_METHOD_NAME, 0)) {
				generateVetoableChangeSupportFields(type);
				generateGetVetoableSupportMethod(type);
				generateVetoableChangeListenerMethods(type);
			}
		}
		for (FIELD_TYPE field : fields) {
			String propertyNameFieldName = "PROP_" + camelCaseToConstant(field.name());
			generatePropertyNameConstant(type, field, propertyNameFieldName);
			generateSetter(type, field, level, vetoable, throwVetoException, propertyNameFieldName);
		}
	}

	private void generatePropertyNameConstant(final TYPE_TYPE type, final FIELD_TYPE field, final String propertyNameFieldName) {
		String propertyName = field.name();
		if (type.hasField(propertyNameFieldName)) return;
		type.injectField(FieldDecl(Type(String.class), propertyNameFieldName).makePublic().makeStatic().makeFinal() //
				.withInitialization(String(propertyName)));
	}

	private void generateSetter(final TYPE_TYPE type, final FIELD_TYPE field, final AccessLevel level, final boolean vetoable, final boolean throwVetoException, final String propertyNameFieldName) {
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

		methodDecl.withStatement(LocalDecl(field.type(), oldValueName).makeFinal().withInitialization(Field(fieldName)));

		if (vetoable) {
			if (throwVetoException) {
				methodDecl.withThrownException(Type(PropertyVetoException.class));
				methodDecl.withStatement(Call(Call(VETOABLE_CHANGE_SUPPORT_METHOD_NAME), FIRE_VETOABLE_CHANGE_METHOD_NAME) //
						.withArgument(Name(propertyNameFieldName)).withArgument(Name(oldValueName)).withArgument(Name(fieldName)));
			} else {
				methodDecl.withStatement(Try(Block().withStatement(Call(Call(VETOABLE_CHANGE_SUPPORT_METHOD_NAME), FIRE_VETOABLE_CHANGE_METHOD_NAME) //
						.withArgument(Name(propertyNameFieldName)).withArgument(Name(oldValueName)).withArgument(Name(fieldName)))) //
						.Catch(Arg(Type(PropertyVetoException.class), E_VALUE_VARIABLE_NAME), Block().withStatement(Return())));
			}
		}
		
		methodDecl.withStatement(Assign(Field(fieldName), Name(fieldName))) //
				.withStatement(Call(Call(PROPERTY_CHANGE_SUPPORT_METHOD_NAME), FIRE_PROPERTY_CHANGE_METHOD_NAME) //
						.withArgument(Name(propertyNameFieldName)).withArgument(Name(oldValueName)).withArgument(Name(fieldName)));
		type.injectMethod(methodDecl);
	}

	private void generatePropertyChangeSupportFields(final TYPE_TYPE type) {
		if (!type.hasField(PROPERTY_CHANGE_SUPPORT_FIELD_NAME)) {
			type.injectField(FieldDecl(Type(PropertyChangeSupport.class), PROPERTY_CHANGE_SUPPORT_FIELD_NAME).makePrivate().makeTransient().makeVolatile());
		}
		if (!type.hasField(PROPERTY_CHANGE_SUPPORT_FIELD_NAME + "Lock")) {
			type.injectField(FieldDecl(Type(Object.class).withDimensions(1), PROPERTY_CHANGE_SUPPORT_FIELD_NAME + "Lock").makePrivate().makeFinal() //
					.withInitialization(NewArray(Type(Object.class)).withDimensionExpression(Number(0))));
		}
	}

	private void generateGetPropertySupportMethod(final TYPE_TYPE type) {
		if (type.hasMethod(PROPERTY_CHANGE_SUPPORT_METHOD_NAME, 0)) return;
		type.injectMethod(MethodDecl(Type(PropertyChangeSupport.class), PROPERTY_CHANGE_SUPPORT_METHOD_NAME).makePrivate() //
				.withStatement(If(Equal(Field(PROPERTY_CHANGE_SUPPORT_FIELD_NAME), Null())).Then(Block() //
						.withStatement(Synchronized(Field(PROPERTY_CHANGE_SUPPORT_FIELD_NAME + "Lock")) //
								.withStatement(If(Equal(Field(PROPERTY_CHANGE_SUPPORT_FIELD_NAME), Null())).Then(Block() //
										.withStatement(Assign(Field(PROPERTY_CHANGE_SUPPORT_FIELD_NAME), New(Type(PropertyChangeSupport.class)).withArgument(This())))))))) //
				.withStatement(Return(Field(PROPERTY_CHANGE_SUPPORT_FIELD_NAME))));
	}

	private void generatePropertyChangeListenerMethods(final TYPE_TYPE type) {
		for (String methodName : PROPERTY_CHANGE_METHOD_NAMES) {
			generatePropertyChangeListenerMethod(methodName, type);
		}
	}

	private void generatePropertyChangeListenerMethod(final String methodName, final TYPE_TYPE type) {
		if (type.hasMethod(methodName, 1)) return;
		type.injectMethod(MethodDecl(Type("void"), methodName).makePublic().withArgument(Arg(Type(PropertyChangeListener.class), LISTENER_ARG_NAME)) //
				.withStatement(Call(Call(PROPERTY_CHANGE_SUPPORT_METHOD_NAME), methodName).withArgument(Name(LISTENER_ARG_NAME))));
	}

	private void generateVetoableChangeSupportFields(final TYPE_TYPE type) {
		if (!type.hasField(VETOABLE_CHANGE_SUPPORT_FIELD_NAME)) {
			type.injectField(FieldDecl(Type(VetoableChangeSupport.class), VETOABLE_CHANGE_SUPPORT_FIELD_NAME).makePrivate().makeTransient().makeVolatile());
		}
		if (!type.hasField(VETOABLE_CHANGE_SUPPORT_FIELD_NAME + "Lock")) {
			type.injectField(FieldDecl(Type(Object.class).withDimensions(1), VETOABLE_CHANGE_SUPPORT_FIELD_NAME + "Lock").makePrivate().makeFinal() //
					.withInitialization(NewArray(Type(Object.class)).withDimensionExpression(Number(0))));
		}
	}

	private void generateGetVetoableSupportMethod(final TYPE_TYPE type) {
		if (type.hasMethod(VETOABLE_CHANGE_SUPPORT_METHOD_NAME, 0)) return;
		type.injectMethod(MethodDecl(Type(VetoableChangeSupport.class), VETOABLE_CHANGE_SUPPORT_METHOD_NAME).makePrivate() //
				.withStatement(If(Equal(Field(VETOABLE_CHANGE_SUPPORT_FIELD_NAME), Null())).Then(Block() //
						.withStatement(Synchronized(Field(VETOABLE_CHANGE_SUPPORT_FIELD_NAME + "Lock")) //
								.withStatement(If(Equal(Field(VETOABLE_CHANGE_SUPPORT_FIELD_NAME), Null())).Then(Block() //
										.withStatement(Assign(Field(VETOABLE_CHANGE_SUPPORT_FIELD_NAME), New(Type(VetoableChangeSupport.class)).withArgument(This())))))))) //
				.withStatement(Return(Field(VETOABLE_CHANGE_SUPPORT_FIELD_NAME))));
	}

	private void generateVetoableChangeListenerMethods(final TYPE_TYPE type) {
		for (String methodName : VETOABLE_CHANGE_METHOD_NAMES) {
			generateVetoableChangeListenerMethod(methodName, type);
		}
	}

	private void generateVetoableChangeListenerMethod(final String methodName, final TYPE_TYPE type) {
		if (type.hasMethod(methodName, 1)) return;
		type.injectMethod(MethodDecl(Type("void"), methodName).makePublic().withArgument(Arg(Type(VetoableChangeListener.class), LISTENER_ARG_NAME)) //
				.withStatement(Call(Call(VETOABLE_CHANGE_SUPPORT_METHOD_NAME), methodName).withArgument(Name(LISTENER_ARG_NAME))));
	}
}
