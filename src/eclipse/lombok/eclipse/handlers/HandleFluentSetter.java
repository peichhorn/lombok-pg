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

import static lombok.core.util.Arrays.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.eclipse.Eclipse.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;
import static lombok.eclipse.handlers.Eclipse.typeDeclFiltering;
import static lombok.ast.AST.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.FluentSetter;
import lombok.Setter;
import lombok.ast.MethodDecl;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.handlers.TransformationsUtil;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.EclipseHandlerUtil.FieldAccess;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.FluentSetter} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleFluentSetter extends EclipseAnnotationHandler<FluentSetter> {
	@Override public void handle(AnnotationValues<FluentSetter> annotation, Annotation ast, EclipseNode annotationNode) {
		FluentSetter annotationInstance = annotation.getInstance();
		AccessLevel level = annotationInstance.value();
		if (level == AccessLevel.NONE) return;
		EclipseNode node = annotationNode.up();
		if (node == null) return;
		if (node.getKind() == Kind.FIELD) {
			createSetterForFields(level, annotationNode.upFromAnnotationToFields(), annotationNode, annotationNode.get(), true);
		}
		if (node.getKind() == Kind.TYPE) {
			generateSetterForType(node, annotationNode, level, false);
		}
	}

	public boolean generateSetterForType(EclipseNode typeNode, EclipseNode pos, AccessLevel level, boolean checkForTypeLevelSetter) {
		if (checkForTypeLevelSetter) {
			if (typeNode != null) for (EclipseNode child : typeNode.down()) {
				if (child.getKind() == Kind.ANNOTATION) {
					if (annotationTypeMatches(Setter.class, child)) {
						//The annotation will make it happen, so we can skip it.
						return true;
					}
				}
			}
		}

		TypeDeclaration typeDecl = typeDeclFiltering(typeNode, AccInterface | AccAnnotation | AccEnum);
		if (typeDecl == null) {
			pos.addError(canBeUsedOnClassAndFieldOnly(FluentSetter.class));
			return false;
		}

		for (EclipseNode field : typeNode.down()) {
			if (field.getKind() != Kind.FIELD) continue;
			FieldDeclaration fieldDecl = (FieldDeclaration) field.get();
			if (!EclipseHandlerUtil.filterField(fieldDecl)) continue;

			//Skip final fields.
			if ((fieldDecl.modifiers & AccFinal) != 0) continue;

			generateSetterForField(field, pos.get(), level, new Annotation[0], new Annotation[0]);
		}
		return true;
	}

	public void generateSetterForField(EclipseNode fieldNode, ASTNode pos, AccessLevel level, Annotation[] onMethod , Annotation[] onParam) {
		for (EclipseNode child : fieldNode.down()) {
			if (child.getKind() == Kind.ANNOTATION) {
				if (annotationTypeMatches(Setter.class, child)) {
					//The annotation will make it happen, so we can skip it.
					return;
				}
			}
		}

		createSetterForField(level, fieldNode, fieldNode, pos, false);
	}

	private void createSetterForFields(AccessLevel level, Collection<EclipseNode> fieldNodes, EclipseNode errorNode, ASTNode source, boolean whineIfExists) {
		for (EclipseNode fieldNode : fieldNodes) {
			createSetterForField(level, fieldNode, errorNode, source, whineIfExists);
		}
	}

	private void createSetterForField(AccessLevel level, EclipseNode fieldNode, EclipseNode errorNode, ASTNode source, boolean whineIfExists) {
		if (fieldNode.getKind() != Kind.FIELD) {
			errorNode.addError(canBeUsedOnClassAndFieldOnly(FluentSetter.class));
			return;
		}

		FieldDeclaration field = (FieldDeclaration) fieldNode.get();

		String fieldName = new String(field.name);

		switch (methodExists(fieldName, fieldNode, false)) {
		case EXISTS_BY_LOMBOK:
			return;
		case EXISTS_BY_USER:
			if (whineIfExists) errorNode.addWarning(
				String.format("Not generating %s(%s %s): A method with that name already exists",
						fieldName, field.type, fieldName));
			return;
		default:
		case NOT_EXISTS:
			//continue with creating the setter
		}

		generateSetter(EclipseType.typeOf(fieldNode, source), fieldNode, fieldName, level, source);
	}
	
	public static List<lombok.ast.Annotation> findAnnotations(AbstractVariableDeclaration variable, Pattern namePattern) {
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
	
	private void generateSetter(EclipseType type, EclipseNode fieldNode, String name, AccessLevel level, ASTNode source) {
		FieldDeclaration field = (FieldDeclaration) fieldNode.get();
		List<lombok.ast.Annotation> nonNulls = findAnnotations(field, TransformationsUtil.NON_NULL_PATTERN);

		MethodDecl methodDecl = MethodDecl(Type(new String(type.name())).withTypeArguments(type.typeParameters()), name).withAccessLevel(level) //
			.withArgument(Arg(Type(field.type), new String(field.name)).withAnnotations(nonNulls));
		
		if ((field.modifiers & AccStatic) != 0) methodDecl.makeStatic();
		if (!nonNulls.isEmpty() && !isPrimitive(field.type)) {
			methodDecl.withStatement(If(Equal(Name(name), Null())).Then(Throw(New(Type("java.lang.NullPointerException")).withArgument(String(name)))));
		}
		methodDecl.withStatement(Assign(Expr(createFieldAccessor(fieldNode, FieldAccess.ALWAYS_FIELD, source)), Name(new String(field.name)))) //
			.withStatement(Return(This()));
		type.injectMethod(methodDecl);
	}
}
