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

import static lombok.ast.AST.*;
import static lombok.eclipse.Eclipse.toQualifiedName;
import static lombok.eclipse.handlers.Eclipse.ensureAllClassScopeMethodWereBuild;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import lombok.ast.IType;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.util.As;
import lombok.core.util.Cast;
import lombok.core.util.Each;
import lombok.core.util.Is;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.Eclipse;
import lombok.eclipse.handlers.EclipseHandlerUtil.MemberExistsResult;

public final class EclipseType implements lombok.ast.IType<EclipseMethod, EclipseField, EclipseNode, ASTNode, TypeDeclaration, AbstractMethodDeclaration> {
	private final EclipseNode typeNode;
	private final ASTNode source;
	private final EclipseTypeEditor editor;

	private EclipseType(final EclipseNode typeNode, final ASTNode source) {
		if (!(typeNode.get() instanceof TypeDeclaration)) {
			throw new IllegalArgumentException();
		}
		this.typeNode = typeNode;
		this.source = source;
		editor = new EclipseTypeEditor(this, source);
	}

	public EclipseTypeEditor editor() {
		return editor;
	}

	public boolean isInterface() {
		return (get().modifiers & AccInterface) != 0;
	}

	public boolean isEnum() {
		return (get().modifiers & AccEnum) != 0;
	}

	public boolean isAnnotation() {
		return (get().modifiers & AccAnnotation) != 0;
	}

	public boolean isClass() {
		return !isInterface() && !isEnum() && !isAnnotation();
	}

	public boolean hasSuperClass() {
		return get().superclass != null;
	}

	public <T extends lombok.ast.IType<?, ?, ?, ?, ?, ?>> T memberType(final String typeName) {
		for (EclipseNode child : node().down()) {
			if (child.getKind() != Kind.TYPE) continue;
			if (child.getName().equals(typeName)) {
				return Cast.<T> uncheckedCast(EclipseType.typeOf(child, source));
			}
		}
		throw new IllegalArgumentException();
	}

	public <T extends IType<?, ?, ?, ?, ?, ?>> T surroundingType() {
		final EclipseNode parent = node().directUp();
		if (parent == null) return null;
		return Cast.<T> uncheckedCast(EclipseType.typeOf(parent, source));
	}

	public List<EclipseMethod> methods() {
		List<EclipseMethod> methods = new ArrayList<EclipseMethod>();
		for (EclipseNode child : node().down()) {
			if (child.getKind() != Kind.METHOD) continue;
			methods.add(EclipseMethod.methodOf(child, source));
		}
		return methods;
	}

	public List<EclipseField> fields() {
		List<EclipseField> fields = new ArrayList<EclipseField>();
		for (EclipseNode child : node().down()) {
			if (child.getKind() != Kind.FIELD) continue;
			final EclipseField field = EclipseField.fieldOf(child, source);
			if (field.ignore()) continue;
			fields.add(field);
		}
		return fields;
	}

	public boolean hasMultiArgumentConstructor() {
		for (AbstractMethodDeclaration def : Each.elementIn(get().methods)) {
			if ((def instanceof ConstructorDeclaration) && Is.notEmpty(def.arguments)) return true;
		}
		return false;
	}

	public TypeDeclaration get() {
		return (TypeDeclaration) typeNode.get();
	}

	public EclipseNode node() {
		return typeNode;
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
			if (Eclipse.matchesType((Annotation) child.get(), typeName)) {
				annotationNode = child;
			}
		}
		return annotationNode;
	}

	public String name() {
		return node().getName();
	}

	public String qualifiedName() {
		StringBuilder qualifiedName = new StringBuilder(name());
		for (IType<?, ?, ?, ?, ?, ?> surroundingType = surroundingType(); surroundingType != null; surroundingType = surroundingType.surroundingType()) {
			qualifiedName.insert(0, surroundingType.name()  + "$");
		}
		CompilationUnitDeclaration cud = (CompilationUnitDeclaration) node().top().get();
		if (cud.currentPackage != null) qualifiedName.insert(0, toQualifiedName(cud.currentPackage.tokens) + ".");
		return qualifiedName.toString();
	}

	public List<lombok.ast.TypeRef> typeArguments() {
		final List<lombok.ast.TypeRef> typeArguments = new ArrayList<lombok.ast.TypeRef>();
		for (TypeParameter typaram : Each.elementIn(get().typeParameters)) {
			typeArguments.add(Type(As.string(typaram.name)));
		}
		return typeArguments;
	}

	public List<lombok.ast.TypeParam> typeParameters() {
		final List<lombok.ast.TypeParam> typeParameters = new ArrayList<lombok.ast.TypeParam>();
		for (TypeParameter typaram : Each.elementIn(get().typeParameters)) {
			lombok.ast.TypeParam typeParameter = TypeParam(As.string(typaram.name)).posHint(typaram);
			if (typaram.type != null) typeParameter.withBound(Type(typaram.type));
			for (TypeReference bound : Each.elementIn(typaram.bounds)) {
				typeParameter.withBound(Type(bound));
			}
			typeParameters.add(typeParameter);
		}
		return typeParameters;
	}

	public List<lombok.ast.Annotation> annotations() {
		return annotations(get().annotations);
	}

	private List<lombok.ast.Annotation> annotations(final Annotation[] anns) {
		final List<lombok.ast.Annotation> annotations = new ArrayList<lombok.ast.Annotation>();
		for (Annotation annotation : Each.elementIn(anns)) {
			lombok.ast.Annotation ann = Annotation(Type(annotation.type)).posHint(annotation);
			if (annotation instanceof SingleMemberAnnotation) {
				ann.withValue(Expr(((SingleMemberAnnotation) annotation).memberValue));
			} else if (annotation instanceof NormalAnnotation) {
				for (MemberValuePair pair : Each.elementIn(((NormalAnnotation) annotation).memberValuePairs)) {
					ann.withValue(As.string(pair.name), Expr(pair.value)).posHint(pair);
				}
			}
			annotations.add(ann);
		}
		return annotations;
	}

	public boolean hasField(final String fieldName) {
		return (fieldExists(fieldName, typeNode) != MemberExistsResult.NOT_EXISTS);
	}

	public boolean hasMethod(final String methodName, final lombok.ast.TypeRef... argumentTypes) {
		// TODO check actual types..
		return (methodExists(methodName, typeNode, false, argumentTypes == null ? 0 : argumentTypes.length) != MemberExistsResult.NOT_EXISTS);
	}

	public boolean hasMethodIncludingSupertypes(final String methodName, final lombok.ast.TypeRef... argumentTypes) {
		return hasMethod(get().binding, methodName, editor().build(As.list(argumentTypes)));
	}

	private boolean hasMethod(final TypeBinding binding, final String methodName, List<ASTNode> argumentTypes) {
		if (binding instanceof ReferenceBinding) {
			ReferenceBinding rb = (ReferenceBinding) binding;
			MethodBinding[] availableMethods = rb.availableMethods();
			for (MethodBinding method : Each.elementIn(availableMethods)) {
				if (method.isAbstract()) continue;
				if (!method.isPublic()) continue;
				if (!methodName.equals(As.string(method.selector))) continue;
				if (argumentTypes.size() != As.list(method.parameters).size()) continue;
				// TODO check actual types..
				return true;
			}
			ReferenceBinding superclass = rb.superclass();
			ensureAllClassScopeMethodWereBuild(superclass);
			return hasMethod(superclass, methodName, argumentTypes);
		}
		return false;
	}

	@Override
	public String toString() {
		return get().toString();
	}

	public static EclipseType typeOf(final EclipseNode node, final ASTNode source) {
		EclipseNode typeNode = Eclipse.typeNodeOf(node);
		return typeNode == null ? null : new EclipseType(typeNode, source);
	}
}
