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
import static lombok.eclipse.handlers.ast.EclipseASTUtil.boxedType;
import static lombok.ast.AST.*;
import static lombok.ast.IMethod.ArgumentStyle.BOXED_TYPES;
import static lombok.ast.IMethod.ArgumentStyle.INCLUDE_ANNOTATIONS;

import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

import lombok.AccessLevel;
import lombok.core.AST.Kind;
import lombok.core.util.As;
import lombok.core.util.Each;
import lombok.core.util.Is;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.Eclipse;

public final class EclipseMethod implements lombok.ast.IMethod<EclipseType, EclipseNode, ASTNode, AbstractMethodDeclaration> {
	private final EclipseNode methodNode;
	private final ASTNode source;
	private final EclipseMethodEditor editor;

	private EclipseMethod(final EclipseNode methodNode, final ASTNode source) {
		if (!(methodNode.get() instanceof AbstractMethodDeclaration)) {
			throw new IllegalArgumentException();
		}
		this.methodNode = methodNode;
		this.source = source;
		editor = new EclipseMethodEditor(this, source);
	}

	public EclipseMethodEditor editor() {
		return editor;
	}

	public lombok.ast.TypeRef returns() {
		return isConstructor() ? null : Type(returnType());
	}

	public lombok.ast.TypeRef boxedReturns() {
		return boxedType(returnType());
	}

	public boolean returns(final Class<?> clazz) {
		return returns(clazz.getSimpleName());
	}

	public boolean returns(final String typeName) {
		TypeReference returnType = returnType();
		if (returnType == null) return false;
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (char[] elem : returnType.getTypeName()) {
			if (first) first = false;
			else sb.append('.');
			sb.append(elem);
		}
		String type = sb.toString();
		return type.endsWith(typeName);
	}

	private TypeReference returnType() {
		if (isConstructor()) return null;
		MethodDeclaration methodDecl = (MethodDeclaration) get();
		return methodDecl.returnType;
	}

	public AccessLevel accessLevel() {
		if ((get().modifiers & AccPublic) != 0) return AccessLevel.PUBLIC;
		if ((get().modifiers & AccProtected) != 0) return AccessLevel.PROTECTED;
		if ((get().modifiers & AccPrivate) != 0) return AccessLevel.PRIVATE;
		return AccessLevel.PACKAGE;
	}

	public boolean isSynchronized() {
		return !isConstructor() && (get().modifiers & AccSynchronized) != 0;
	}

	public boolean isStatic() {
		return !isConstructor() && (get().modifiers & AccStatic) != 0;
	}

	public boolean isConstructor() {
		return get() instanceof ConstructorDeclaration;
	}

	public boolean isAbstract() {
		return get().isAbstract();
	}

	public boolean isEmpty() {
		if (isConstructor() && (((ConstructorDeclaration) get()).constructorCall != null)) return false;
		return Is.empty(get().statements);
	}

	public AbstractMethodDeclaration get() {
		return (AbstractMethodDeclaration) methodNode.get();
	}

	public EclipseNode node() {
		return methodNode;
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

	public boolean hasNonFinalArgument() {
		for (Argument arg : Each.elementIn(get().arguments)) {
			if ((arg.modifiers & AccFinal) == 0) {
				return true;
			}
		}
		return false;
	}

	public boolean hasArguments() {
		return Is.notEmpty(get().arguments);
	}

	public String name() {
		return node().getName();
	}

	public EclipseType surroundingType() {
		return EclipseType.typeOf(node(), source);
	}

	public List<lombok.ast.Statement<?>> statements() {
		final List<lombok.ast.Statement<?>> methodStatements = new ArrayList<lombok.ast.Statement<?>>();
		for (Object statement : Each.elementIn(get().statements)) {
			methodStatements.add(Stat(statement));
		}
		return methodStatements;
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

	public java.util.List<lombok.ast.Argument> arguments(final ArgumentStyle... style) {
		final List<ArgumentStyle> styles = As.list(style);
		final List<lombok.ast.Argument> methodArguments = new ArrayList<lombok.ast.Argument>();
		for (Argument argument : Each.elementIn(get().arguments)) {
			lombok.ast.TypeRef argType = styles.contains(BOXED_TYPES) ? boxedType(argument.type).posHint(argument.type) : Type(argument.type);
			lombok.ast.Argument arg = Arg(argType, As.string(argument.name)).posHint(argument);
			if (styles.contains(INCLUDE_ANNOTATIONS)) arg.withAnnotations(annotations(argument.annotations));
			methodArguments.add(arg);
		}
		return methodArguments;
	}

	public List<lombok.ast.TypeParam> typeParameters() {
		final List<lombok.ast.TypeParam> typeParameters = new ArrayList<lombok.ast.TypeParam>();
		if (isConstructor()) return typeParameters;
		MethodDeclaration methodDecl = (MethodDeclaration) get();
		for (TypeParameter typaram : Each.elementIn(methodDecl.typeParameters)) {
			lombok.ast.TypeParam typeParameter = TypeParam(As.string(typaram.name)).posHint(typaram);
			if (typaram.type != null) typeParameter.withBound(Type(typaram.type));
			for (TypeReference bound : Each.elementIn(typaram.bounds)) {
				typeParameter.withBound(Type(bound));
			}
			typeParameters.add(typeParameter);
		}
		return typeParameters;
	}

	public List<lombok.ast.TypeRef> thrownExceptions() {
		final List<lombok.ast.TypeRef> thrownExceptions = new ArrayList<lombok.ast.TypeRef>();
		for (Object thrownException : Each.elementIn(get().thrownExceptions)) {
			thrownExceptions.add(Type(thrownException));
		}
		return thrownExceptions;
	}

	@Override
	public String toString() {
		return get().toString();
	}

	public static EclipseMethod methodOf(final EclipseNode node, final ASTNode source) {
		EclipseNode methodNode = Eclipse.methodNodeOf(node);
		return methodNode == null ? null : new EclipseMethod(methodNode, source);
	}
}
