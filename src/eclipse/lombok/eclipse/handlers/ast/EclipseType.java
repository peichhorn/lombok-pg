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

import static lombok.ast.AST.*;
import static lombok.core.util.Arrays.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;
import static org.eclipse.jdt.core.dom.Modifier.PRIVATE;
import static org.eclipse.jdt.core.dom.Modifier.PROTECTED;
import static org.eclipse.jdt.core.dom.Modifier.PUBLIC;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccInterface;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccEnum;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccAnnotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;

import lombok.ast.IType;
import lombok.core.AST.Kind;
import lombok.core.util.Cast;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.Eclipse;
import lombok.eclipse.handlers.EclipseHandlerUtil;
import lombok.eclipse.handlers.EclipseHandlerUtil.MemberExistsResult;

public class EclipseType implements IType<EclipseMethod, EclipseNode, ASTNode, TypeDeclaration, AbstractMethodDeclaration> {
	private final EclipseNode typeNode;
	private final ASTNode source;
	private final EclipseASTMaker builder;

	private EclipseType(final EclipseNode typeNode, final ASTNode source) {
		if (!(typeNode.get() instanceof TypeDeclaration)) {
			throw new IllegalArgumentException();
		}
		this.typeNode = typeNode;
		this.source = source;
		builder = new EclipseASTMaker(typeNode, source);
	}

	public <T extends ASTNode> T build(lombok.ast.Node node) {
		return builder.<T>build(node);
	}

	public <T extends ASTNode> T build(lombok.ast.Node node, Class<T> extectedType) {
		return builder.build(node,extectedType);
	}

	public <T extends ASTNode> List<T> build(List<? extends lombok.ast.Node> nodes) {
		return builder.build(nodes);
	}

	public <T extends ASTNode> List<T> build(List<? extends lombok.ast.Node> nodes, Class<T> extectedType) {
		return builder.build(nodes, extectedType);
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

	public boolean hasSuperClass() {
		return get().superclass != null;
	}

	public <T extends IType<?, ?, ?, ?, ?>> T memberType(String typeName) {
		for (EclipseNode child : node().down()) {
			if (child.getKind() != Kind.TYPE) continue;
			if (child.getName().equals(typeName)) {
				return Cast.<T>uncheckedCast(EclipseType.typeOf(child, source));
			}
		}
		throw new IllegalArgumentException();
	}

	public List<EclipseMethod> methods() {
		List<EclipseMethod> methods = new ArrayList<EclipseMethod>();
		for (EclipseNode child : node().down()) {
			if (child.getKind() != Kind.METHOD) continue;
			methods.add(EclipseMethod.methodOf(child, source));
		}
		return methods;
	}

	public boolean hasMultiArgumentConstructor() {
		if (isNotEmpty(get().methods)) for (AbstractMethodDeclaration def : get().methods) {
			if ((def instanceof ConstructorDeclaration) && isNotEmpty(def.arguments)) return true;
		}
		return false;
	}

	public TypeDeclaration get() {
		return (TypeDeclaration)typeNode.get();
	}

	public EclipseNode node() {
		return typeNode;
	}

	public void injectField(lombok.ast.FieldDecl fieldDecl) {
		final FieldDeclaration field = builder.build(fieldDecl);
		EclipseHandlerUtil.injectField(typeNode, field);
	}

	public void injectField(lombok.ast.EnumConstant enumConstant) {
		final FieldDeclaration field = builder.build(enumConstant);
		EclipseHandlerUtil.injectField(typeNode, field);
	}

	public AbstractMethodDeclaration injectMethod(lombok.ast.MethodDecl methodDecl) {
		return (MethodDeclaration) injectMethodImpl(methodDecl);
	}

	public AbstractMethodDeclaration injectConstructor(lombok.ast.ConstructorDecl constructorDecl) {
		return (ConstructorDeclaration) injectMethodImpl(constructorDecl);
	}

	private AbstractMethodDeclaration injectMethodImpl(lombok.ast.AbstractMethodDecl<?> methodDecl) {
		final AbstractMethodDeclaration method = builder.build(methodDecl, MethodDeclaration.class);
		EclipseHandlerUtil.injectMethod(typeNode, method);

		TypeDeclaration type = (TypeDeclaration) typeNode.get();
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
				} catch (Exception e) {
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

	public void injectType(lombok.ast.ClassDecl typeDecl) {
		final TypeDeclaration type = builder.build(typeDecl);
		Eclipse.injectType(typeNode, type);
	}

	public void removeMethod(EclipseMethod method) {
		TypeDeclaration type = (TypeDeclaration) typeNode.get();
		List<AbstractMethodDeclaration> methods = new ArrayList<AbstractMethodDeclaration>();
		for (AbstractMethodDeclaration decl : type.methods) {
			if (!decl.equals(method.get())) {
				methods.add(decl);
			}
		}
		type.methods = methods.toArray(new AbstractMethodDeclaration[0]);
		typeNode.removeChild(method.node());
	}

	public String name() {
		return new String(get().name);
	}

	public List<lombok.ast.TypeRef> typeParameters() {
		final List<lombok.ast.TypeRef> typeParameters = new ArrayList<lombok.ast.TypeRef>();
		if (isNotEmpty(get().typeParameters)) for (TypeParameter param : get().typeParameters) {
			typeParameters.add(Type(new String(param.name)));
		}
		return typeParameters;
	}

	public boolean hasField(String fieldName) {
		return (fieldExists(fieldName, typeNode) != MemberExistsResult.NOT_EXISTS);
	}

	public boolean hasMethod(String methodName) {
		return (methodExists(methodName, typeNode, false) != MemberExistsResult.NOT_EXISTS);
	}

	public void makeEnum() {
		get().modifiers |= AccEnum;
	}

	public void makePrivate() {
		makePackagePrivate();
		get().modifiers |= PRIVATE;
	}

	public void makePackagePrivate() {
		get().modifiers &= ~(PRIVATE |PROTECTED | PUBLIC);
	}

	public void makeProtected() {
		makePackagePrivate();
		get().modifiers |= PROTECTED;
	}

	public void makePublic() {
		makePackagePrivate();
		get().modifiers |= PUBLIC;
	}

	public void rebuild() {
		node().rebuild();
	}

	@Override
	public String toString() {
		return get().toString();
	}

	public static EclipseType typeOf(final EclipseNode node, final ASTNode source) {
		EclipseNode typeNode = Eclipse.typeNodeOf(node);
		return typeNode == null ? null : new EclipseType(typeNode, source);
	}

	private static final class Reflection {
		public static final Method methodScopeCreateMethodMethod;
		static {
			Method m = null;
			try {
				m = MethodScope.class.getDeclaredMethod("createMethod", AbstractMethodDeclaration.class);
				m.setAccessible(true);
			} catch (Exception e) {
				// well can't do anything about it then
			}
			methodScopeCreateMethodMethod = m;
		}
	}
}
