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
package lombok.eclipse.handlers;

import static lombok.ast.AST.*;
import static lombok.core.handlers.TransformationsUtil.*;
import static lombok.core.util.Arrays.isNotEmpty;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccFinal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.*;
import lombok.ast.MethodDecl;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.handlers.TransformationsUtil;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.*;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.BoundSetter} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleBoundSetter extends EclipseAnnotationHandler<BoundSetter> {
	private static final String PROPERTY_SUPPORT_FIELD_NAME = "propertySupport";
	private static final String FIRE_PROPERTY_CHANGE_METHOD_NAME = "firePropertyChange";
	private static final String OLD_VALUE_VARIABLE_NAME = "old";
	private static final Pattern SETTER_PATTERN = Pattern.compile("^(?:setter|fluentsetter|boundsetter)$", Pattern.CASE_INSENSITIVE);

	@Override
	public void handle(AnnotationValues<BoundSetter> annotation, Annotation ast, EclipseNode annotationNode) {
		EclipseNode mayBeField = annotationNode.up();
		if (mayBeField == null) return;
		EclipseType type = EclipseType.typeOf(annotationNode, ast);
		List<EclipseNode> fields = new ArrayList<EclipseNode>(annotationNode.upFromAnnotationToFields());
		if (mayBeField.getKind() == Kind.FIELD) {
			fields.addAll(annotationNode.upFromAnnotationToFields());
		} else if (mayBeField.getKind() == Kind.TYPE) {
			for (EclipseNode field : type.node().down()) {
				if (field.getKind() != Kind.FIELD) continue;
				FieldDeclaration fieldDecl = (FieldDeclaration) field.get();
				if (!findAnnotations(fieldDecl, SETTER_PATTERN).isEmpty()) continue;
				if (!EclipseHandlerUtil.filterField(fieldDecl)) continue;
				if ((fieldDecl.modifiers & AccFinal) != 0) continue;
				fields.add(field);
			}
		} else {
			annotationNode.addError(canBeUsedOnClassAndFieldOnly(BoundSetter.class));
			return;
		}
		generateSetter(fields, annotation.getInstance(), type);
	}

	private void generateSetter(List<EclipseNode> fields, BoundSetter setter, EclipseType type) {
		for (EclipseNode fieldNode : fields) {
			String propertyNameFieldName = nameOfConstantBasedOnProperty(fieldNode.getName());
			generatePropertyNameConstant(propertyNameFieldName, fieldNode, type);
			generateSetter(propertyNameFieldName, setter, fieldNode, type);
		}
	}

	private void generatePropertyNameConstant(String propertyNameFieldName, EclipseNode fieldNode, EclipseType type) {
		String propertyName = fieldNode.getName();
		if (type.hasField(propertyNameFieldName)) return;
		type.injectField(FieldDecl(Type(String.class), propertyNameFieldName).makePublic().makeStatic().makeFinal() //
			.withInitialization(New(Type(String.class)).withArgument(String(propertyName))));
	}

	private void generateSetter(String propertyNameFieldName, BoundSetter setter, EclipseNode fieldNode, EclipseType type) {
		FieldDeclaration field = (FieldDeclaration) fieldNode.get();
		String fieldName = fieldNode.getName();
		TypeReference fieldType = field.type;
		boolean isBoolean = nameEquals(fieldType.getTypeName(), "boolean") && fieldType.dimensions() == 0;
		String setterName = toSetterName(fieldName, isBoolean);
		if (type.hasMethod(setterName)) return;
		String oldValueName = OLD_VALUE_VARIABLE_NAME;
		List<lombok.ast.Annotation> nonNulls = findAnnotations(field, TransformationsUtil.NON_NULL_PATTERN);
		MethodDecl methodDecl = MethodDecl(Type("void"), setterName).withAccessLevel(setter.value()).withArgument(Arg(Type(fieldType), fieldName).withAnnotations(nonNulls));
		if (!nonNulls.isEmpty() && !isPrimitive(fieldType)) {
			methodDecl.withStatement(If(Equal(Name(fieldName), Null())).Then(Throw(New(Type("java.lang.NullPointerException")).withArgument(String(fieldName)))));
		}
		methodDecl.withStatement(LocalDecl(Type(fieldType), oldValueName).makeFinal().withInitialization(Field(fieldName))) //
			.withStatement(Assign(Field(fieldName), Name(fieldName))) //
			.withStatement(Call(Field(PROPERTY_SUPPORT_FIELD_NAME), FIRE_PROPERTY_CHANGE_METHOD_NAME) //
				.withArgument(Name(propertyNameFieldName)).withArgument(Name(oldValueName)).withArgument(Field(fieldName)));
		type.injectMethod(methodDecl);
	}

	private List<lombok.ast.Annotation> findAnnotations(final AbstractVariableDeclaration variable, final Pattern namePattern) {
		List<lombok.ast.Annotation> result = new ArrayList<lombok.ast.Annotation>();
		if (isNotEmpty(variable.annotations)) for (Annotation annotation : variable.annotations) {
			TypeReference typeRef = annotation.type;
			char[][] typeName = typeRef.getTypeName();
			String suspect = new String(typeName[typeName.length - 1]);
			if (namePattern.matcher(suspect).matches()) {
				result.add(Annotation(Type(typeRef)));
			}
		}
		return result;
	}
}