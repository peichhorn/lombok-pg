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
package lombok.javac.handlers;

import static com.sun.tools.javac.code.Flags.FINAL;
import static com.sun.tools.javac.code.Flags.STATIC;
import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.javac.handlers.Javac.deleteImport;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.*;
import lombok.ast.MethodDecl;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.handlers.TransformationsUtil;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacType;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

/**
 * Handles the {@code lombok.BoundSetter} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleBoundSetter extends JavacAnnotationHandler<BoundSetter> {
	private static final String PROPERTY_SUPPORT_FIELD_NAME = "propertySupport";
	private static final String FIRE_PROPERTY_CHANGE_METHOD_NAME = "firePropertyChange";
	private static final String OLD_VALUE_VARIABLE_NAME = "old";
	private static final Pattern SETTER_PATTERN = Pattern.compile("^(?:setter|fluentsetter|boundsetter)$", Pattern.CASE_INSENSITIVE);

	@Override
	public void handle(final AnnotationValues<BoundSetter> annotation, final JCAnnotation ast, final JavacNode annotationNode) {
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
			deleteAnnotationIfNeccessary(annotationNode, BoundSetter.class);
			deleteImport(annotationNode, AccessLevel.class);
			annotationNode.addError(canBeUsedOnClassAndFieldOnly(BoundSetter.class));
			return;
		}
		deleteAnnotationIfNeccessary(annotationNode, BoundSetter.class);
		deleteImport(annotationNode, AccessLevel.class);
		generateSetter(fields, annotation.getInstance(), type);
	}

	private void generateSetter(final List<JavacNode> fields, final BoundSetter setter, final JavacType type) {
		for (JavacNode fieldNode : fields) {
			String propertyNameFieldName = "PROP_" + camelCaseToConstant(fieldNode.getName());
			generatePropertyNameConstant(propertyNameFieldName, fieldNode, type);
			generateSetter(propertyNameFieldName, setter, fieldNode, type);
		}
	}

	private void generatePropertyNameConstant(final String propertyNameFieldName, final JavacNode fieldNode, final JavacType type) {
		String propertyName = fieldNode.getName();
		if (type.hasField(propertyNameFieldName)) return;
		type.injectField(FieldDecl(Type(String.class), propertyNameFieldName).makePublic().makeStatic().makeFinal() //
			.withInitialization(New(Type(String.class)).withArgument(String(propertyName))));
	}

	private void generateSetter(final String propertyNameFieldName, final BoundSetter setter, final JavacNode fieldNode, final JavacType type) {
		JCVariableDecl field = (JCVariableDecl) fieldNode.get();
		String fieldName = fieldNode.getName();
		JCExpression fieldType = field.vartype;
		String setterName = toSetterName(field);
		if (type.hasMethod(setterName)) return;
		String oldValueName = OLD_VALUE_VARIABLE_NAME;
		List<lombok.ast.Annotation> nonNulls = findAnnotations(field, TransformationsUtil.NON_NULL_PATTERN);
		MethodDecl methodDecl = MethodDecl(Type("void"), setterName).withAccessLevel(setter.value()).withArgument(Arg(Type(fieldType), fieldName).withAnnotations(nonNulls));
		if (!nonNulls.isEmpty() && !isPrimitive(fieldType)) {
			methodDecl.withStatement(If(Equal(Name(fieldName), Null())).Then(Throw(New(Type(NullPointerException.class)).withArgument(String(fieldName)))));
		}
		methodDecl.withStatement(LocalDecl(Type(fieldType), oldValueName).makeFinal().withInitialization(Field(fieldName))) //
			.withStatement(Assign(Field(fieldName), Name(fieldName))) //
			.withStatement(Call(Field(PROPERTY_SUPPORT_FIELD_NAME), FIRE_PROPERTY_CHANGE_METHOD_NAME) //
				.withArgument(Name(propertyNameFieldName)).withArgument(Name(oldValueName)).withArgument(Field(fieldName)));
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
