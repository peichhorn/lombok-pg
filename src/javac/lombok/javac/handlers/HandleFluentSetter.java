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
import static lombok.javac.handlers.Javac.deleteImport;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.util.*;
import java.util.regex.Pattern;

import lombok.*;
import lombok.ast.*;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.handlers.TransformationsUtil;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacType;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
/**
 * Handles the {@code lombok.FluentSetter} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleFluentSetter extends JavacAnnotationHandler<FluentSetter> {
	private static final Pattern SETTER_PATTERN = Pattern.compile("^(?:setter|fluentsetter|boundsetter)$", Pattern.CASE_INSENSITIVE);
	
	@Override public void handle(final AnnotationValues<FluentSetter> annotation, final JCAnnotation ast, final JavacNode annotationNode) {
		JavacNode mayBeField = annotationNode.up();
		if (mayBeField == null) return;
		JavacType type = JavacType.typeOf(annotationNode, ast);
		List<JavacNode> fields = new ArrayList<JavacNode>();
		if (mayBeField.getKind() == Kind.FIELD) {
			fields.addAll(annotationNode.upFromAnnotationToFields());
		} else if (mayBeField.getKind() == Kind.TYPE) {
			for (JavacNode field : type.node().down()) {
				if (field.getKind() != Kind.FIELD) continue;
				JCVariableDecl fieldDecl = (JCVariableDecl) field.get();
				if (!findAnnotations(fieldDecl, SETTER_PATTERN).isEmpty()) continue;
				if (fieldDecl.name.toString().startsWith("$")) continue;
				if ((fieldDecl.mods.flags & STATIC) != 0) continue;
				if ((fieldDecl.mods.flags & FINAL) != 0) continue;
				fields.add(field);
			}
		} else {
			deleteAnnotationIfNeccessary(annotationNode, FluentSetter.class);
			deleteImport(annotationNode, AccessLevel.class);
			annotationNode.addError(canBeUsedOnClassAndFieldOnly(FluentSetter.class));
			return;
		}
		deleteAnnotationIfNeccessary(annotationNode, FluentSetter.class);
		deleteImport(annotationNode, AccessLevel.class);
		generateSetter(fields, annotation.getInstance(), type);
	}
	
	private void generateSetter(final List<JavacNode> fields, final FluentSetter setter, final JavacType type) {
		for (JavacNode fieldNode : fields) {
			generateSetter(setter, fieldNode, type);
		}
	}

	private void generateSetter(final FluentSetter setter, final JavacNode fieldNode, final JavacType type) {
		JCVariableDecl field = (JCVariableDecl) fieldNode.get();
		String fieldName = fieldNode.getName();
		JCExpression fieldType = field.vartype;
		if (type.hasMethod(fieldName)) return;
		List<lombok.ast.Annotation> nonNulls = findAnnotations(field, TransformationsUtil.NON_NULL_PATTERN);
		List<lombok.ast.Annotation> nullables = findAnnotations(field, TransformationsUtil.NULLABLE_PATTERN);
		MethodDecl methodDecl = MethodDecl(Type(type.name()).withTypeArguments(type.typeParameters()), fieldNode.getName()).withAccessLevel(setter.value()) //
			.withArgument(Arg(Type(fieldType), fieldName).withAnnotations(nonNulls).withAnnotations(nullables));
		if (!nonNulls.isEmpty() && !isPrimitive(fieldType)) {
			methodDecl.withStatement(If(Equal(Name(fieldName), Null())).Then(Throw(New(Type("java.lang.NullPointerException")).withArgument(String(fieldName)))));
		}
		methodDecl.withStatement(Assign(Field(fieldName), Name(fieldName))) //
			.withStatement(Return(This()));
		type.injectMethod(methodDecl);
	}

	public static List<lombok.ast.Annotation> findAnnotations(final JCVariableDecl variable, final Pattern namePattern) {
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
}
