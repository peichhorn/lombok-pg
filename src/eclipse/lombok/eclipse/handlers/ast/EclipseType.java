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
import static lombok.core.util.Names.string;
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
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;

import lombok.core.AST.Kind;
import lombok.core.util.Cast;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.Eclipse;
import lombok.eclipse.handlers.EclipseHandlerUtil;
import lombok.eclipse.handlers.EclipseHandlerUtil.MemberExistsResult;

public final class EclipseType implements lombok.ast.IType<EclipseMethod, EclipseField, EclipseNode, ASTNode, TypeDeclaration, AbstractMethodDeclaration> {
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

	public <T extends ASTNode> T build(final lombok.ast.Node node) {
		return builder.<T>build(node);
	}

	public <T extends ASTNode> T build(final lombok.ast.Node node, final Class<T> extectedType) {
		return builder.build(node,extectedType);
	}

	public <T extends ASTNode> List<T> build(final List<? extends lombok.ast.Node> nodes) {
		return builder.build(nodes);
	}

	public <T extends ASTNode> List<T> build(final List<? extends lombok.ast.Node> nodes, final Class<T> extectedType) {
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

	public List<EclipseField> fields() {
		List<EclipseField> fields = new ArrayList<EclipseField>();
		for (EclipseNode child : node().down()) {
			if (child.getKind() != Kind.FIELD) continue;
			fields.add(EclipseField.fieldOf(child, source));
		}
		return fields;
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

	public void injectInitializer(final lombok.ast.Initializer initializer) {
		final Initializer initializerBlock = builder.build(initializer);
		Eclipse.injectInitializer(typeNode, initializerBlock);
	}

	public void injectField(final lombok.ast.FieldDecl fieldDecl) {
		final FieldDeclaration field = builder.build(fieldDecl);
		EclipseHandlerUtil.injectField(typeNode, field);
	}

	public void injectField(final lombok.ast.EnumConstant enumConstant) {
		final FieldDeclaration field = builder.build(enumConstant);
		EclipseHandlerUtil.injectField(typeNode, field);
	}

	public AbstractMethodDeclaration injectMethod(final lombok.ast.MethodDecl methodDecl) {
		return (MethodDeclaration) injectMethodImpl(methodDecl);
	}

	public AbstractMethodDeclaration injectConstructor(final lombok.ast.ConstructorDecl constructorDecl) {
		return (ConstructorDeclaration) injectMethodImpl(constructorDecl);
	}

	private AbstractMethodDeclaration injectMethodImpl(final lombok.ast.AbstractMethodDecl<?> methodDecl) {
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
		Eclipse.injectType(typeNode, type);
	}

	public void removeMethod(final EclipseMethod method) {
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
		return node().getName();
	}

	public List<lombok.ast.TypeRef> typeArguments() {
		final List<lombok.ast.TypeRef> typeArguments = new ArrayList<lombok.ast.TypeRef>();
		if (isNotEmpty(get().typeParameters)) for (TypeParameter typaram : get().typeParameters) {
			typeArguments.add(Type(string(typaram.name)));
		}
		return typeArguments;
	}

	public List<lombok.ast.TypeParam> typeParameters() {
		final List<lombok.ast.TypeParam> typeParameters = new ArrayList<lombok.ast.TypeParam>();
		if (isNotEmpty(get().typeParameters)) for (TypeParameter typaram : get().typeParameters) {
			lombok.ast.TypeParam typeParameter = TypeParam(string(typaram.name));
			if (typaram.type != null) typeParameter.withBound(Type(typaram.type));
			if (isNotEmpty(typaram.bounds)) for (TypeReference bound : typaram.bounds) {
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
		if (isNotEmpty(anns)) for (Annotation annotation : anns) {
			lombok.ast.Annotation ann = Annotation(Type(annotation.type));
			if (annotation instanceof SingleMemberAnnotation) {
				ann.withValue(Expr(((SingleMemberAnnotation)annotation).memberValue));
			} else if (annotation instanceof NormalAnnotation) {
				for (MemberValuePair pair : ((NormalAnnotation)annotation).memberValuePairs) {
					ann.withValue(new String(pair.name), Expr(pair.value));
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
			} catch (final Exception e) {
				// well can't do anything about it then
			}
			methodScopeCreateMethodMethod = m;
		}
	}
}
