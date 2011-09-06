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

import static lombok.ast.AST.*;
import static lombok.core.util.Arrays.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;

import java.util.*;
import java.util.regex.Pattern;

import lombok.*;
import lombok.ast.*;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.handlers.TransformationsUtil;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.FluentSetter} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleFluentSetter extends EclipseAnnotationHandler<FluentSetter> {
	private static final Pattern SETTER_PATTERN = Pattern.compile("^(?:setter|fluentsetter|boundsetter)$", Pattern.CASE_INSENSITIVE);

	@Override public void handle(final AnnotationValues<FluentSetter> annotation, final Annotation ast, final EclipseNode annotationNode) {
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
			annotationNode.addError(canBeUsedOnClassAndFieldOnly(FluentSetter.class));
			return;
		}
		generateSetter(fields, annotation.getInstance(), type);
	}

	private void generateSetter(final List<EclipseNode> fields, final FluentSetter setter, final EclipseType type) {
		for (EclipseNode fieldNode : fields) {
			generateSetter(setter, fieldNode, type);
		}
	}

	private void generateSetter(final FluentSetter setter, final EclipseNode fieldNode, final EclipseType type) {
		FieldDeclaration field = (FieldDeclaration) fieldNode.get();
		String fieldName = fieldNode.getName();
		TypeReference fieldType = field.type;
		if (type.hasMethod(fieldName)) return;
		List<lombok.ast.Annotation> nonNulls = findAnnotations(field, TransformationsUtil.NON_NULL_PATTERN);
		List<lombok.ast.Annotation> nullables = findAnnotations(field, TransformationsUtil.NULLABLE_PATTERN);
		MethodDecl methodDecl = MethodDecl(Type(type.name()).withTypeArguments(type.typeParameters()), fieldName).withAccessLevel(setter.value()) //
			.withArgument(Arg(Type(fieldType), fieldName).withAnnotations(nonNulls).withAnnotations(nullables));
		if (!nonNulls.isEmpty() && !isPrimitive(fieldType)) {
			methodDecl.withStatement(If(Equal(Name(fieldName), Null())).Then(Throw(New(Type("java.lang.NullPointerException")).withArgument(String(fieldName)))));
		}
		methodDecl.withStatement(Assign(Field(fieldName), Name(fieldName))) //
			.withStatement(Return(This()));
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
