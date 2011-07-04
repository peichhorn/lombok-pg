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
package lombok.javac.handlers;

import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static com.sun.tools.javac.code.Flags.*;
import static lombok.javac.handlers.Javac.typeDeclFiltering;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import lombok.*;
import lombok.ast.*;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.handlers.TransformationsUtil;
import lombok.javac.Javac;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.JavacHandlerUtil.FieldAccess;
import lombok.javac.handlers.ast.JavacType;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree;

/**
 * Handles the {@code lombok.FluentSetter} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleFluentSetter extends JavacAnnotationHandler<FluentSetter> {

	@Override public void handle(AnnotationValues<FluentSetter> annotation, JCAnnotation ast, JavacNode annotationNode) {
		Collection<JavacNode> fields = annotationNode.upFromAnnotationToFields();
		deleteAnnotationIfNeccessary(annotationNode, FluentSetter.class);
		deleteImportFromCompilationUnit(annotationNode, "lombok.AccessLevel");
		FluentSetter annotationInstance = annotation.getInstance();
		AccessLevel level = annotationInstance.value();
		handle(ast, annotationNode, level, fields);
	}

	public void generateSetterForType(JavacNode typeNode, JavacNode errorNode, AccessLevel level, boolean checkForTypeLevelSetter) {
		if (checkForTypeLevelSetter) {
			if (typeNode != null) for (JavacNode child : typeNode.down()) {
				if (child.getKind() == Kind.ANNOTATION) {
					if (Javac.annotationTypeMatches(FluentSetter.class, child)) {
						return;
					}
				}
			}
		}

		JCClassDecl typeDecl = typeDeclFiltering(typeNode, INTERFACE | ANNOTATION | ENUM);
		if (typeDecl == null) {
			errorNode.addError(canBeUsedOnClassAndFieldOnly(FluentSetter.class));
			return;
		}

		for (JavacNode field : typeNode.down()) {
			if (field.getKind() != Kind.FIELD) continue;
			JCVariableDecl fieldDecl = (JCVariableDecl) field.get();
			if (fieldDecl.name.toString().startsWith("$")) continue;
			if ((fieldDecl.mods.flags & STATIC) != 0) continue;
			if ((fieldDecl.mods.flags & FINAL) != 0) continue;

			generateSetterForField(field, errorNode.get(), level);
		}
	}

	public void generateSetterForField(JavacNode fieldNode, JCTree source, AccessLevel level) {
		for (JavacNode child : fieldNode.down()) {
			if (child.getKind() == Kind.ANNOTATION) {
				if (Javac.annotationTypeMatches(FluentSetter.class, child)) {
					return;
				}
			}
		}
		createSetterForField(level, fieldNode, fieldNode, source, false);
	}

	public void handle(JCAnnotation source, JavacNode annotationNode, AccessLevel level, Collection<JavacNode> fields) {
		if (level == AccessLevel.NONE) return;

		JavacNode node = annotationNode.up();
		if (node == null) return;
		List<JCExpression> onMethod = getAndRemoveAnnotationParameter(source, "onMethod");
		List<JCExpression> onParam = getAndRemoveAnnotationParameter(source, "onParam");
		if (node.getKind() == Kind.FIELD) {
			createSetterForFields(level, fields, annotationNode, source, true);
		}
		if (node.getKind() == Kind.TYPE) {
			if (!onMethod.isEmpty()) annotationNode.addError("'onMethod' is not supported for @Setter on a type.");
			if (!onParam.isEmpty()) annotationNode.addError("'onParam' is not supported for @Setter on a type.");
			generateSetterForType(node, annotationNode, level, false);
		}
	}

	private void createSetterForFields(AccessLevel level, Collection<JavacNode> fieldNodes, JavacNode errorNode, JCTree source, boolean whineIfExists) {
		for (JavacNode fieldNode : fieldNodes) {
			createSetterForField(level, fieldNode, errorNode, source, whineIfExists);
		}
	}

	private void createSetterForField(AccessLevel level, JavacNode fieldNode, JavacNode errorNode, JCTree source, boolean whineIfExists) {
		if (fieldNode.getKind() != Kind.FIELD) {
			fieldNode.addError(canBeUsedOnClassAndFieldOnly(FluentSetter.class));
			return;
		}

		JCVariableDecl fieldDecl = (JCVariableDecl)fieldNode.get();

		String fieldName = fieldDecl.name.toString();

		switch (methodExists(fieldName, fieldNode, false)) {
		case EXISTS_BY_LOMBOK:
			return;
		case EXISTS_BY_USER:
			if (whineIfExists) errorNode.addWarning(
					String.format("Not generating %s(%s %s): A method with that name already exists",
							fieldName, fieldDecl.vartype, fieldName));
			return;
		default:
		case NOT_EXISTS:
		}

		generateSetter(JavacType.typeOf(fieldNode, source), fieldNode, level, source);
	}

	public static List<lombok.ast.Annotation> findAnnotations(JCVariableDecl variable, Pattern namePattern) {
		List<lombok.ast.Annotation> result = new ArrayList<lombok.ast.Annotation>();
		for (JCAnnotation annotation : variable.mods.annotations) {
			String name = annotation.annotationType.toString();
			int idx = name.lastIndexOf(".");
			String suspect = idx == -1 ? name : name.substring(idx + 1);
			if (namePattern.matcher(suspect).matches()) {
				result.add(Annotation(Type(annotation.annotationType)));
			}
		}
		return result;
	}

	private void generateSetter(JavacType type, JavacNode fieldNode, AccessLevel level, JCTree source) {
		JCVariableDecl field = (JCVariableDecl) fieldNode.get();
		String fieldName = fieldNode.getName();

		List<lombok.ast.Annotation> nonNulls = findAnnotations(field, TransformationsUtil.NON_NULL_PATTERN);

		MethodDecl methodDecl = MethodDecl(Type(new String(type.name())).withTypeArguments(type.typeParameters()), fieldName).withAccessLevel(level) //
			.withArgument(Arg(Type(field.vartype), fieldName).withAnnotations(nonNulls));

		if ((field.mods.flags & STATIC) != 0) methodDecl.makeStatic();
		if (!nonNulls.isEmpty() && !isPrimitive(field.vartype)) {
			methodDecl.withStatement(If(Equal(Name(fieldName), Null())).Then(Throw(New(Type("java.lang.NullPointerException")).withArgument(String(fieldName)))));
		}
		methodDecl.withStatement(Assign(Expr(createFieldAccessor(fieldNode.getTreeMaker(), fieldNode, FieldAccess.ALWAYS_FIELD)), Name(new String(fieldName)))) //
			.withStatement(Return(This()));
		type.injectMethod(methodDecl);
	}
}
