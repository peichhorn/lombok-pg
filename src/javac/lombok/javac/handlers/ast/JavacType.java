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
package lombok.javac.handlers.ast;

import static com.sun.tools.javac.code.Flags.*;
import static lombok.ast.AST.*;
import static lombok.javac.handlers.JavacHandlerUtil.createAnnotation;
import static lombok.javac.handlers.JavacHandlerUtil.fieldExists;
import static lombok.javac.handlers.JavacHandlerUtil.methodExists;

import java.util.ArrayList;
import java.util.List;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;

import lombok.ast.IType;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.util.As;
import lombok.core.util.Cast;
import lombok.javac.JavacNode;
import lombok.javac.handlers.Javac;
import lombok.javac.handlers.JavacHandlerUtil.MemberExistsResult;

public final class JavacType implements lombok.ast.IType<JavacMethod, JavacField, JavacNode, JCTree, JCClassDecl, JCMethodDecl> {
	private final JavacNode typeNode;
	private final JCTree source;
	private final JavacTypeEditor editor;

	private JavacType(final JavacNode typeNode, final JCTree source) {
		if (!(typeNode.get() instanceof JCClassDecl)) {
			throw new IllegalArgumentException();
		}
		this.typeNode = typeNode;
		this.source = source;
		editor = new JavacTypeEditor(this, source);
	}

	public JavacTypeEditor editor() {
		return editor;
	}

	public boolean isInterface() {
		return (get().mods.flags & INTERFACE) != 0;
	}

	public boolean isEnum() {
		return (get().mods.flags & ENUM) != 0;
	}

	public boolean isAnnotation() {
		return (get().mods.flags & ANNOTATION) != 0;
	}

	public boolean isClass() {
		return !isInterface() && !isEnum() && !isAnnotation();
	}

	public boolean hasSuperClass() {
		return get().getExtendsClause() != null;
	}

	public <A extends java.lang.annotation.Annotation> AnnotationValues<A> getAnnotationValue(final Class<A> expectedType) {
		final JavacNode node = getAnnotation(expectedType);
		return node == null ? null : createAnnotation(expectedType, node);
	}

	public JavacNode getAnnotation(final Class<? extends java.lang.annotation.Annotation> expectedType) {
		return getAnnotation(expectedType.getName());
	}

	public JavacNode getAnnotation(final String typeName) {
		JavacNode annotationNode = null;
		for (JavacNode child : node().down()) {
			if (child.getKind() != Kind.ANNOTATION) continue;
			if (Javac.matchesType((JCAnnotation) child.get(), typeName)) {
				annotationNode = child;
			}
		}
		return annotationNode;
	}

	public <T extends lombok.ast.IType<?, ?, ?, ?, ?, ?>> T memberType(final String typeName) {
		for (JavacNode child : node().down()) {
			if (child.getKind() != Kind.TYPE) continue;
			if (child.getName().equals(typeName)) {
				return Cast.<T> uncheckedCast(JavacType.typeOf(child, source));
			}
		}
		throw new IllegalArgumentException();
	}

	public <T extends IType<?, ?, ?, ?, ?, ?>> T surroundingType() {
		final JavacNode parent = node().directUp();
		if (parent == null) return null;
		return Cast.<T> uncheckedCast(JavacType.typeOf(parent, source));
	}

	public List<JavacMethod> methods() {
		List<JavacMethod> methods = new ArrayList<JavacMethod>();
		for (JavacNode child : node().down()) {
			if (child.getKind() != Kind.METHOD) continue;
			methods.add(JavacMethod.methodOf(child, source));
		}
		return methods;
	}

	public List<JavacField> fields() {
		List<JavacField> fields = new ArrayList<JavacField>();
		for (JavacNode child : node().down()) {
			if (child.getKind() != Kind.FIELD) continue;
			fields.add(JavacField.fieldOf(child, source));
		}
		return fields;
	}

	public boolean hasMultiArgumentConstructor() {
		for (JCTree def : get().defs) {
			if ((def instanceof JCMethodDecl) && !((JCMethodDecl) def).params.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public JCClassDecl get() {
		return (JCClassDecl) typeNode.get();
	}

	public JavacNode node() {
		return typeNode;
	}

	public String name() {
		return node().getName();
	}

	public String qualifiedName() {
		StringBuilder qualifiedName = new StringBuilder(name());
		for (IType<?, ?, ?, ?, ?, ?> surroundingType = surroundingType(); surroundingType != null; surroundingType = surroundingType.surroundingType()) {
			qualifiedName.insert(0, surroundingType.name()  + "$");
		}
		JCCompilationUnit cu = (JCCompilationUnit) node().top().get();
		if (cu.getPackageName() != null) qualifiedName.insert(0, cu.getPackageName() + ".");
		return qualifiedName.toString();
	}

	public List<lombok.ast.TypeRef> typeArguments() {
		final List<lombok.ast.TypeRef> typeArguments = new ArrayList<lombok.ast.TypeRef>();
		for (JCTypeParameter typaram : get().typarams) {
			typeArguments.add(Type(As.string(typaram.name)));
		}
		return typeArguments;
	}

	public List<lombok.ast.TypeParam> typeParameters() {
		final List<lombok.ast.TypeParam> typeParameters = new ArrayList<lombok.ast.TypeParam>();
		for (JCTypeParameter typaram : get().typarams) {
			final lombok.ast.TypeParam typeParam = TypeParam(As.string(typaram.name));
			for (JCExpression expr : typaram.bounds) {
				typeParam.withBound(Type(expr));
			}
			typeParameters.add(typeParam);
		}
		return typeParameters;
	}

	public java.util.List<lombok.ast.Annotation> annotations() {
		return annotations(get().mods);
	}

	private java.util.List<lombok.ast.Annotation> annotations(final JCModifiers mods) {
		final java.util.List<lombok.ast.Annotation> annotations = new java.util.ArrayList<lombok.ast.Annotation>();
		for (JCAnnotation annotation : mods.annotations) {
			lombok.ast.Annotation ann = Annotation(Type(annotation.annotationType));
			for (JCExpression arg : annotation.args) {
				if (arg instanceof JCAssign) {
					JCAssign assign = (JCAssign) arg;
					ann.withValue(assign.lhs.toString(), Expr(assign.rhs));
				} else {
					ann.withValue(Expr(arg));
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
		return hasMethod(get().sym, methodName, editor().build(As.list(argumentTypes)));
	}

	private boolean hasMethod(final TypeSymbol type, final String methodName, final List<JCTree> argumentTypes) {
		if (type == null) return false;
		for (Symbol enclosedElement : type.getEnclosedElements()) {
			if (enclosedElement instanceof MethodSymbol) {
				if ((enclosedElement.flags() & (Flags.ABSTRACT)) != 0) continue;
				if ((enclosedElement.flags() & (Flags.PUBLIC)) == 0) continue;
				MethodSymbol method = (MethodSymbol) enclosedElement;
				if (!methodName.equals(As.string(method.name))) continue;
				MethodType methodType = (MethodType) method.type;
				if (argumentTypes.size() != methodType.argtypes.size()) continue;
				// TODO check actual types..
				return true;
			}
		}
		Type supertype = ((ClassSymbol)type).getSuperclass();
		return hasMethod(supertype.tsym, methodName, argumentTypes);
	}

	@Override
	public String toString() {
		return get().toString();
	}

	public static JavacType typeOf(final JavacNode node, final JCTree source) {
		JavacNode typeNode = Javac.typeNodeOf(node);
		return typeNode == null ? null : new JavacType(typeNode, source);
	}
}
