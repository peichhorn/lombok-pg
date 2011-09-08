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
package lombok.javac.handlers.ast;

import static com.sun.tools.javac.code.Flags.*;
import static lombok.ast.AST.*;
import static lombok.javac.handlers.JavacHandlerUtil.fieldExists;
import static lombok.javac.handlers.JavacHandlerUtil.methodExists;

import java.util.ArrayList;
import java.util.List;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.ListBuffer;

import lombok.ast.IType;
import lombok.ast.WrappedMethodDecl;
import lombok.core.AST.Kind;
import lombok.core.util.Cast;
import lombok.javac.JavacNode;
import lombok.javac.handlers.Javac;
import lombok.javac.handlers.JavacHandlerUtil;
import lombok.javac.handlers.JavacHandlerUtil.MemberExistsResult;

public final class JavacType implements IType<JavacMethod, JavacNode, JCTree, JCClassDecl, JCMethodDecl> {
	private final JavacNode typeNode;
	private final JCTree source;
	private final JavacASTMaker builder;

	private JavacType(final JavacNode typeNode, final JCTree source) {
		if (!(typeNode.get() instanceof JCClassDecl)) {
			throw new IllegalArgumentException();
		}
		this.typeNode = typeNode;
		this.source = source;
		builder = new JavacASTMaker(typeNode, source);
	}

	public <T extends JCTree> T build(final lombok.ast.Node node) {
		return builder.<T>build(node);
	}

	public <T extends JCTree> T build(final lombok.ast.Node node, final Class<T> extectedType) {
		return builder.build(node,extectedType);
	}

	public <T extends JCTree> List<T> build(final List<? extends lombok.ast.Node> nodes) {
		return builder.build(nodes);
	}

	public <T extends JCTree> List<T> build(final List<? extends lombok.ast.Node> nodes, final Class<T> extectedType) {
		return builder.build(nodes, extectedType);
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

	public <T extends IType<?, ?, ?, ?, ?>> T memberType(final String typeName) {
		for (JavacNode child : node().down()) {
			if (child.getKind() != Kind.TYPE) continue;
			if (child.getName().equals(typeName)) {
				return Cast.<T>uncheckedCast(JavacType.typeOf(child, source));
			}
		}
		throw new IllegalArgumentException();
	}

	public List<JavacMethod> methods() {
		List<JavacMethod> methods = new ArrayList<JavacMethod>();
		for (JavacNode child : node().down()) {
			if (child.getKind() != Kind.METHOD) continue;
			methods.add(JavacMethod.methodOf(child, source));
		}
		return methods;
	}

	public boolean hasMultiArgumentConstructor() {
		for (JCTree def : get().defs) {
			if ((def instanceof JCMethodDecl) && !((JCMethodDecl)def).params.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public JCClassDecl get() {
		return (JCClassDecl)typeNode.get();
	}

	public JavacNode node() {
		return typeNode;
	}

	public void injectInitializer(final lombok.ast.Initializer initializer) {
		final JCBlock initializerBlock = builder.build(initializer);
		Javac.injectInitializer(typeNode, initializerBlock);
	}

	public void injectField(final lombok.ast.FieldDecl fieldDecl) {
		final JCVariableDecl field = builder.build(fieldDecl);
		JavacHandlerUtil.injectField(typeNode, field);
	}

	public void injectField(final lombok.ast.EnumConstant enumConstant) {
		final JCVariableDecl field = builder.build(enumConstant);
		JavacHandlerUtil.injectField(typeNode, field);
	}

	public JCMethodDecl injectMethod(final lombok.ast.MethodDecl methodDecl) {
		return injectMethodImpl(methodDecl);
	}

	public JCMethodDecl injectConstructor(final lombok.ast.ConstructorDecl constructorDecl) {
		return injectMethodImpl(constructorDecl);
	}

	private JCMethodDecl injectMethodImpl(final lombok.ast.AbstractMethodDecl<?> methodDecl) {
		final JCMethodDecl method = builder.build(methodDecl, JCMethodDecl.class);
		JavacHandlerUtil.injectMethod(typeNode, method);
		if (methodDecl instanceof WrappedMethodDecl) {
			WrappedMethodDecl node = (WrappedMethodDecl)methodDecl;
			MethodSymbol methodSymbol = (MethodSymbol) node.getWrappedObject();
			JCClassDecl tree = get();
			ClassSymbol c = tree.sym;
			c.members_field.enter(methodSymbol, c.members_field, methodSymbol.enclClass().members_field);
			method.sym = methodSymbol;
		}
		return method;
	}

	public void injectType(final lombok.ast.ClassDecl typeDecl) {
		final JCClassDecl type = builder.build(typeDecl);
		Javac.injectType(typeNode, type);
	}

	public void removeMethod(final JavacMethod method) {
		JCClassDecl type = (JCClassDecl) typeNode.get();
		ListBuffer<JCTree> defs = ListBuffer.lb();
		for (JCTree def : type.defs) {
			if (!def.equals(method.get())) {
				defs.append(def);
			}
		}
		type.defs = defs.toList();
		typeNode.removeChild(method.node());
	}

	public String name() {
		return get().name.toString();
	}

	public List<lombok.ast.TypeRef> typeParameters() {
		final List<lombok.ast.TypeRef> typeParameters = new ArrayList<lombok.ast.TypeRef>();
		for (JCTypeParameter param : get().typarams) {
			typeParameters.add(Type(param.name.toString()));
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

	public boolean hasMethod(final String methodName) {
		return (methodExists(methodName, typeNode, false) != MemberExistsResult.NOT_EXISTS);
	}

	public void makeEnum() {
		get().mods.flags |= ENUM;
	}

	public void makePrivate() {
		makePackagePrivate();
		get().mods.flags |= PRIVATE;
	}

	public void makePackagePrivate() {
		get().mods.flags &= ~(PRIVATE |PROTECTED | PUBLIC);
	}

	public void makeProtected() {
		makePackagePrivate();
		get().mods.flags |= PROTECTED;
	}

	public void makePublic() {
		makePackagePrivate();
		get().mods.flags |= PUBLIC;
	}

	public void rebuild() {
		node().rebuild();
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
