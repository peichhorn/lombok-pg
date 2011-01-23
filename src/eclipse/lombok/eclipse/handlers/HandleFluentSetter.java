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
import static lombok.eclipse.handlers.EclipseNodeBuilder.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;

import java.util.Collection;

import lombok.AccessLevel;
import lombok.FluentSetter;
import lombok.Setter;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.handlers.TransformationsUtil;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.EclipseHandlerUtil.FieldAccess;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.FluentSetter} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleFluentSetter implements EclipseAnnotationHandler<FluentSetter> {
	@Override public boolean handle(AnnotationValues<FluentSetter> annotation, Annotation ast, EclipseNode annotationNode) {
		FluentSetter annotationInstance = annotation.getInstance();
		AccessLevel level = annotationInstance.value();
		if (level == AccessLevel.NONE) return true;
		EclipseNode node = annotationNode.up();
		if (node == null) return false;
		Annotation[] onMethod = getAndRemoveAnnotationParameter(ast, "onMethod");
		Annotation[] onParam = getAndRemoveAnnotationParameter(ast, "onParam");
		if (node.getKind() == Kind.FIELD) {
			return createSetterForFields(level, annotationNode.upFromAnnotationToFields(), annotationNode, annotationNode.get(), true, onMethod, onParam);
		}
		if (node.getKind() == Kind.TYPE) {
			if (isNotEmpty(onMethod)) annotationNode.addError("'onMethod' is not supported for @Setter on a type.");
			if (isNotEmpty(onParam)) annotationNode.addError("'onParam' is not supported for @Setter on a type.");
			return generateSetterForType(node, annotationNode, level, false);
		}
		return false;
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
		
		TypeDeclaration typeDecl = null;
		if (typeNode.get() instanceof TypeDeclaration) typeDecl = (TypeDeclaration) typeNode.get();
		int modifiers = typeDecl == null ? 0 : typeDecl.modifiers;
		boolean notAClass = (modifiers & (AccInterface | AccAnnotation | AccEnum)) != 0;
		
		if (typeDecl == null || notAClass) {
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
		
		createSetterForField(level, fieldNode, fieldNode, pos, false, onMethod, onParam);
	}

	private boolean createSetterForFields(AccessLevel level, Collection<EclipseNode> fieldNodes, EclipseNode errorNode, ASTNode source, boolean whineIfExists, Annotation[] onMethod , Annotation[] onParam) {
		for (EclipseNode fieldNode : fieldNodes) {
			createSetterForField(level, fieldNode, errorNode, source, whineIfExists, onMethod, onParam);
		}
		return true;
	}
	
	private boolean createSetterForField(AccessLevel level, EclipseNode fieldNode, EclipseNode errorNode, ASTNode source, boolean whineIfExists,
			Annotation[] onMethod , Annotation[] onParam) {
		if (fieldNode.getKind() != Kind.FIELD) {
			errorNode.addError("@Setter is only supported on a class or a field.");
			return true;
		}
		
		FieldDeclaration field = (FieldDeclaration) fieldNode.get();
		String fieldName = new String(field.name);
		
		String setterName = fieldName;
		
		int modifier = toEclipseModifier(level) | (field.modifiers & AccStatic);
		
		switch (methodExists(setterName, fieldNode, false)) {
		case EXISTS_BY_LOMBOK:
			return true;
		case EXISTS_BY_USER:
			if (whineIfExists) errorNode.addWarning(
				String.format("Not generating %s(%s %s): A method with that name already exists",
				setterName, field.type, fieldName));
			return true;
		default:
		case NOT_EXISTS:
			//continue with creating the setter
		}

		MethodDeclaration method = generateSetter((TypeDeclaration) fieldNode.up().get(), fieldNode, setterName, modifier, source, onParam);
		Annotation[] copiedAnnotations = copyAnnotations(source, onMethod);
		if (isNotEmpty(copiedAnnotations)) {
			method.annotations = copiedAnnotations;
		}
		
		injectMethod(fieldNode.up(), method);
		
		return true;
	}
	
	private MethodDeclaration generateSetter(TypeDeclaration parent, EclipseNode fieldNode, String name, int modifier, ASTNode source, Annotation[] onParam) {
		FieldDeclaration field = (FieldDeclaration) fieldNode.get();
		Annotation[] nonNulls = findAnnotations(field, TransformationsUtil.NON_NULL_PATTERN);
		Annotation[] nullables = findAnnotations(field, TransformationsUtil.NULLABLE_PATTERN);
		
		TypeReference returnType;
		if (isNotEmpty(parent.typeParameters)) {
			TypeReference[] refs = new TypeReference[parent.typeParameters.length];
			int idx = 0;
			for (TypeParameter param : parent.typeParameters) {
				refs[idx++] = typeReference(source, new String(param.name));
			}
			returnType = typeReference(source, new String(parent.name), refs);
		} else returnType = typeReference(source, new String(parent.name));
		
		Argument param = argument(source, field.type, new String(field.name));
		Annotation[] copiedAnnotations = copyAnnotations(source, nonNulls, nullables, onParam);
		if (isNotEmpty(copiedAnnotations)) param.annotations = copiedAnnotations;
		
		MethodBuilder builder = method(fieldNode, source, modifier, returnType, name).withParameter(param);
		if (isNotEmpty(nonNulls)) {
			Statement nullCheck = generateNullCheck(field, source);
			if (nullCheck != null) builder.withStatement(nullCheck); 
		}
		builder.withAssignStatement(createFieldAccessor(fieldNode, FieldAccess.ALWAYS_FIELD, source), nameReference(source, new String(field.name)))
			.withReturnStatement(thisReference(source));
		return builder.build();
	}
}
