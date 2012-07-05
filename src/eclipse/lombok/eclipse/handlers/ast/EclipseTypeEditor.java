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

import static lombok.core.util.Arrays.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;

import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.Eclipse;
import lombok.eclipse.handlers.EclipseHandlerUtil;

public final class EclipseTypeEditor implements lombok.ast.ITypeEditor<EclipseMethod, ASTNode, TypeDeclaration, AbstractMethodDeclaration> {
	private final EclipseType type;
	private final EclipseASTMaker builder;

	EclipseTypeEditor(final EclipseType type, final ASTNode source) {
		this.type = type;
		builder = new EclipseASTMaker(type.node(), source);
	}

	public TypeDeclaration get() {
		return type.get();
	}

	public EclipseNode node() {
		return type.node();
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

	public void injectInitializer(final lombok.ast.Initializer initializer) {
		final Initializer initializerBlock = builder.build(initializer);
		Eclipse.injectInitializer(node(), initializerBlock);
	}

	public void injectField(final lombok.ast.FieldDecl fieldDecl) {
		final FieldDeclaration field = builder.build(fieldDecl);
		EclipseHandlerUtil.injectField(node(), field);
	}

	public void injectField(final lombok.ast.EnumConstant enumConstant) {
		final FieldDeclaration field = builder.build(enumConstant);
		EclipseHandlerUtil.injectField(node(), field);
	}

	public AbstractMethodDeclaration injectMethod(final lombok.ast.MethodDecl methodDecl) {
		return (MethodDeclaration) injectMethodImpl(methodDecl);
	}

	public AbstractMethodDeclaration injectConstructor(final lombok.ast.ConstructorDecl constructorDecl) {
		return (ConstructorDeclaration) injectMethodImpl(constructorDecl);
	}

	private AbstractMethodDeclaration injectMethodImpl(final lombok.ast.AbstractMethodDecl<?> methodDecl) {
		final AbstractMethodDeclaration method = builder.build(methodDecl, MethodDeclaration.class);
		EclipseHandlerUtil.injectMethod(node(), method);

		TypeDeclaration type = get();
		if (type.scope != null && method.scope == null) {
			boolean aboutToBeResolved = false;
			for (StackTraceElement elem : Thread.currentThread().getStackTrace()) {
				if ("org.eclipse.jdt.internal.compiler.lookup.ClassScope".equals(elem.getClassName()) && "buildFieldsAndMethods".equals(elem.getMethodName())) {
					aboutToBeResolved = true;
					break;
				}
			}
			if (!aboutToBeResolved) {
				MethodScope scope = new MethodScope(type.scope, method, methodDecl.getModifiers().contains(lombok.ast.Modifier.STATIC));
				MethodBinding methodBinding = null;
				try {
					methodBinding = (MethodBinding) Reflection.methodScopeCreateMethodMethod.invoke(scope, method);
				} catch (final Exception e) {
					// See 'Reflection' class for why we ignore this exception.
				}
				if (methodBinding != null) {
					SourceTypeBinding sourceType = type.scope.referenceContext.binding;
					MethodBinding[] methods = sourceType.methods();
					methods = resize(methods, methods.length + 1);
					methods[methods.length - 1] = methodBinding;
					sourceType.setMethods(methods);
					sourceType.resolveTypesFor(methodBinding);
				}
			}
		}
		return method;
	}

	public void injectType(final lombok.ast.ClassDecl typeDecl) {
		final TypeDeclaration type = builder.build(typeDecl);
		Eclipse.injectType(node(), type);
	}

	public void removeMethod(final EclipseMethod method) {
		TypeDeclaration type = get();
		List<AbstractMethodDeclaration> methods = new ArrayList<AbstractMethodDeclaration>();
		for (AbstractMethodDeclaration decl : type.methods) {
			if (!decl.equals(method.get())) {
				methods.add(decl);
			}
		}
		type.methods = methods.toArray(new AbstractMethodDeclaration[0]);
		node().removeChild(method.node());
	}

	public void makeEnum() {
		get().modifiers |= AccEnum;
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

	public void makeStatic() {
		get().modifiers |= AccStatic;
	}

	public void rebuild() {
		node().rebuild();
	}

	@Override
	public String toString() {
		return get().toString();
	}

	private static final class Reflection {
		public static final Method methodScopeCreateMethodMethod;
		static {
			Method m = null;
			try {
				m = MethodScope.class.getDeclaredMethod("createMethod", AbstractMethodDeclaration.class);
				m.setAccessible(true);
			} catch (final Exception e) {
				// well can't do anything about it then
			}
			methodScopeCreateMethodMethod = m;
		}
	}
}
