/*
 * Copyright © 2010-2012 Philipp Eichhorn
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.*;
import lombok.ast.*;
import lombok.core.LombokNode;
import lombok.core.AST.Kind;
import lombok.core.TransformationsUtil;

@RequiredArgsConstructor
public abstract class FluentSetterHandler<TYPE_TYPE extends IType<?, FIELD_TYPE, ?, ?, ?, ?>, FIELD_TYPE extends IField<?, ?, ?>, LOMBOK_NODE_TYPE extends LombokNode<?, LOMBOK_NODE_TYPE, ?>, SOURCE_TYPE> {
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
			annotationNode.addError(canBeUsedOnClassAndFieldOnly(FluentSetter.class));
			return;
		}
		generateSetter(type, fields, level);
	}

	protected abstract TYPE_TYPE typeOf(final LOMBOK_NODE_TYPE node, final SOURCE_TYPE ast);

	protected abstract FIELD_TYPE fieldOf(final LOMBOK_NODE_TYPE node, final SOURCE_TYPE ast);

	private void generateSetter(final TYPE_TYPE type, final List<FIELD_TYPE> fields, final AccessLevel level) {
		for (FIELD_TYPE field : fields) {
			generateSetter(type, field, level);
		}
	}

	private void generateSetter(final TYPE_TYPE type, final FIELD_TYPE field, final AccessLevel level) {
		String fieldName = field.name();
		TypeRef fieldType = field.type();
		if (type.hasMethod(fieldName, fieldType)) return;
		List<lombok.ast.Annotation> nonNulls = field.annotations(TransformationsUtil.NON_NULL_PATTERN);
		List<lombok.ast.Annotation> nullables = field.annotations(TransformationsUtil.NULLABLE_PATTERN);
		MethodDecl methodDecl = MethodDecl(Type(type.name()).withTypeArguments(type.typeArguments()), fieldName).withAccessLevel(level) //
				.withArgument(Arg(fieldType, fieldName).withAnnotations(nonNulls).withAnnotations(nullables));
		if (!nonNulls.isEmpty() && !field.isPrimitive()) {
			methodDecl.withStatement(If(Equal(Name(fieldName), Null())).Then(Throw(New(Type(NullPointerException.class)).withArgument(String(fieldName)))));
		}
		methodDecl.withStatement(Assign(Field(fieldName), Name(fieldName))) //
				.withStatement(Return(This()));
		type.editor().injectMethod(methodDecl);
	}
}
