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
package lombok.eclipse.handlers.ast;

import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;
import static lombok.ast.AST.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

import lombok.core.util.As;
import lombok.core.util.Each;
import lombok.core.util.Is;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseNode;

public final class EclipseField implements lombok.ast.IField<EclipseNode, ASTNode, FieldDeclaration> {
	private final EclipseNode fieldNode;
	private final EclipseASTMaker builder;

	private EclipseField(final EclipseNode fieldNode, final ASTNode source) {
		if (!(fieldNode.get() instanceof FieldDeclaration)) {
			throw new IllegalArgumentException();
		}
		this.fieldNode = fieldNode;
		builder = new EclipseASTMaker(fieldNode, source);
	}

	public <T extends ASTNode> T build(final lombok.ast.Node<?> node) {
		return builder.<T> build(node);
	}

	public <T extends ASTNode> T build(final lombok.ast.Node<?> node, final Class<T> extectedType) {
		return builder.build(node, extectedType);
	}

	public <T extends ASTNode> List<T> build(final List<? extends lombok.ast.Node<?>> nodes) {
		return builder.build(nodes);
	}

	public <T extends ASTNode> List<T> build(final List<? extends lombok.ast.Node<?>> nodes, final Class<T> extectedType) {
		return builder.build(nodes, extectedType);
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

	public FieldDeclaration get() {
		return (FieldDeclaration) fieldNode.get();
	}

	public EclipseNode node() {
		return fieldNode;
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

	public lombok.ast.Expression<?> initialization() {
		return get().initialization == null ? null : Expr(get().initialization);
	}

	public void replaceInitialization(lombok.ast.Expression<?> initialization) {
		get().initialization = (initialization == null) ? null : build(initialization, Expression.class);
	}

	public void makePrivate() {
		makePackagePrivate();
		get().modifiers |= AccPrivate;
	}

	public void makePackagePrivate() {
		get().modifiers &= ~(AccPrivate | AccProtected | AccPublic);
	}

	public void makeProtected() {
		makePackagePrivate();
		get().modifiers |= AccProtected;
	}

	public void makePublic() {
		makePackagePrivate();
		get().modifiers |= AccPublic;
	}

	public void makeNonFinal() {
		get().modifiers &= ~AccFinal;
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
					for (MemberValuePair pair : ((NormalAnnotation) annotation).memberValuePairs) {
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
