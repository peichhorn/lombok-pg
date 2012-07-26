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
package lombok.eclipse.handlers.ast;

import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;
import static lombok.ast.AST.*;
import static lombok.eclipse.handlers.Eclipse.matchesType;
import static lombok.eclipse.handlers.EclipseHandlerUtil.createAnnotation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.util.As;
import lombok.core.util.Each;
import lombok.core.util.Is;
import lombok.core.util.Names;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseNode;
import lombok.experimental.Accessors;

public final class EclipseField implements lombok.ast.IField<EclipseType, EclipseNode, ASTNode, FieldDeclaration> {
	private final EclipseNode fieldNode;
	private final ASTNode source;
	private final EclipseFieldEditor editor;
	private final String filteredName;

	private EclipseField(final EclipseNode fieldNode, final ASTNode source) {
		if (!(fieldNode.get() instanceof FieldDeclaration)) {
			throw new IllegalArgumentException();
		}
		this.fieldNode = fieldNode;
		this.source = source;
		editor = new EclipseFieldEditor(this, source);
		final AnnotationValues<Accessors> accessorsValues;
		if (getAnnotation(Accessors.class) != null) {
			accessorsValues = getAnnotationValue(Accessors.class);
		} else {
			final EclipseType surroundingType = surroundingType();
			accessorsValues = surroundingType.getAnnotationValue(Accessors.class);
		}
		filteredName = Names.removePrefix(name(), accessorsValues.getInstance().prefix());
	}

	public EclipseFieldEditor editor() {
		return editor;
	}

	public boolean isPrivate() {
		return (get().modifiers & AccPrivate) != 0;
	}

	public boolean isFinal() {
		return (get().modifiers & AccFinal) != 0;
	}

	public boolean isStatic() {
		return (get().modifiers & AccStatic) != 0;
	}

	public boolean isInitialized() {
		return get().initialization != null;
	}

	public boolean isPrimitive() {
		return Eclipse.isPrimitive(get().type);
	}

	public boolean hasJavaDoc() {
		return get().javadoc != null;
	}

	public FieldDeclaration get() {
		return (FieldDeclaration) fieldNode.get();
	}

	public EclipseNode node() {
		return fieldNode;
	}

	public boolean ignore() {
		return filteredName == null;
	}

	public <A extends java.lang.annotation.Annotation> AnnotationValues<A> getAnnotationValue(final Class<A> expectedType) {
		final EclipseNode node = getAnnotation(expectedType);
		return node == null ? AnnotationValues.of(expectedType, node()) : createAnnotation(expectedType, node);
	}

	public EclipseNode getAnnotation(final Class<? extends java.lang.annotation.Annotation> expectedType) {
		return getAnnotation(expectedType.getName());
	}

	public EclipseNode getAnnotation(final String typeName) {
		EclipseNode annotationNode = null;
		for (EclipseNode child : node().down()) {
			if (child.getKind() != Kind.ANNOTATION) continue;
			if (matchesType((Annotation) child.get(), typeName)) {
				annotationNode = child;
			}
		}
		return annotationNode;
	}

	public lombok.ast.TypeRef type() {
		return Type(get().type);
	}

	public lombok.ast.TypeRef boxedType() {
		return EclipseASTUtil.boxedType(get().type);
	}

	public boolean isOfType(final String typeName) {
		TypeReference variableType = get().type;
		if (variableType == null) return false;
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (char[] elem : variableType.getTypeName()) {
			if (first) first = false;
			else sb.append('.');
			sb.append(elem);
		}
		String type = sb.toString();
		return type.endsWith(typeName);
	}

	public String name() {
		return node().getName();
	}

	public String filteredName() {
		return filteredName == null ? name() : filteredName;
	}

	public EclipseType surroundingType() {
		return EclipseType.typeOf(node(), source);
	}

	public lombok.ast.Expression<?> initialization() {
		return get().initialization == null ? null : Expr(get().initialization);
	}

	public List<lombok.ast.TypeRef> typeArguments() {
		final List<lombok.ast.TypeRef> typeArguments = new ArrayList<lombok.ast.TypeRef>();
		final TypeReference type = get().type;
		if (type instanceof ParameterizedQualifiedTypeReference) {
			ParameterizedQualifiedTypeReference typeRef = (ParameterizedQualifiedTypeReference) type;
			if (Is.notEmpty(typeRef.typeArguments)) for (TypeReference typeArgument : Each.elementIn(typeRef.typeArguments[typeRef.typeArguments.length - 1])) {
				typeArguments.add(Type(typeArgument));
			}
		} else if (type instanceof ParameterizedSingleTypeReference) {
			ParameterizedSingleTypeReference typeRef = (ParameterizedSingleTypeReference) type;
			for (TypeReference typeArgument : Each.elementIn(typeRef.typeArguments)) {
				typeArguments.add(Type(typeArgument));
			}
		}
		return typeArguments;
	}

	public List<lombok.ast.Annotation> annotations() {
		return annotations(null);
	}

	public List<lombok.ast.Annotation> annotations(final Pattern namePattern) {
		List<lombok.ast.Annotation> result = new ArrayList<lombok.ast.Annotation>();
		for (Annotation annotation : Each.elementIn(get().annotations)) {
			TypeReference typeRef = annotation.type;
			char[][] typeName = typeRef.getTypeName();
			String suspect = As.string(typeName[typeName.length - 1]);
			if ((namePattern == null) || namePattern.matcher(suspect).matches()) {
				lombok.ast.Annotation ann = Annotation(Type(annotation.type)).posHint(annotation);
				if (annotation instanceof SingleMemberAnnotation) {
					ann.withValue(Expr(((SingleMemberAnnotation) annotation).memberValue));
				} else if (annotation instanceof NormalAnnotation) {
					for (MemberValuePair pair : Each.elementIn(((NormalAnnotation) annotation).memberValuePairs)) {
						ann.withValue(As.string(pair.name), Expr(pair.value)).posHint(pair);
					}
				}
				result.add(ann);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return get().toString();
	}

	public static EclipseField fieldOf(final EclipseNode node, final ASTNode source) {
		EclipseNode fieldNode = node;
		while ((fieldNode != null) && !(fieldNode.get() instanceof FieldDeclaration)) {
			fieldNode = fieldNode.up();
		}
		return fieldNode == null ? null : new EclipseField(fieldNode, source);
	}
}
