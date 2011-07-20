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

import static org.eclipse.jdt.core.dom.Modifier.PRIVATE;
import static org.eclipse.jdt.core.dom.Modifier.PROTECTED;
import static org.eclipse.jdt.core.dom.Modifier.PUBLIC;
import static org.eclipse.jdt.internal.compiler.ast.ASTNode.IsSynchronized;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccFinal;
import static lombok.core.util.Arrays.*;
import static lombok.core.util.Lists.list;
import static lombok.core.util.Names.capitalize;
import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import static lombok.ast.AST.*;

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
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

import lombok.ast.IMethod;
import lombok.ast.TypeRef;
import lombok.core.util.Arrays;
import lombok.eclipse.EclipseNode;

public class EclipseMethod implements IMethod<EclipseType, EclipseNode, ASTNode, AbstractMethodDeclaration> {
	private final EclipseNode methodNode;
	private final ASTNode source;
	private final EclipseASTMaker builder;

	private EclipseMethod(final EclipseNode methodNode, final ASTNode source) {
		if (!(methodNode.get() instanceof AbstractMethodDeclaration)) {
			throw new IllegalArgumentException();
		}
		this.methodNode = methodNode;
		this.source = source;
		builder = new EclipseASTMaker(methodNode, source);
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

	public TypeRef returns() {
		if (isConstructor()) return null;
		return Type(returnType());
	}

	public TypeRef boxedReturns() {
		if (isConstructor()) return null;
		TypeReference type = returnType();
		lombok.ast.TypeRef objectReturnType = Type(type);
		if (type instanceof SingleTypeReference) {
			final String name = new String(type.getLastToken());
			if ("int".equals(name)) {
				objectReturnType = Type("java.lang.Integer");
			} else if ("char".equals(name)) {
				objectReturnType = Type("java.lang.Character");
			} else {
				objectReturnType = Type("java.lang." + capitalize(name));
			}
		}
		return objectReturnType;
	}

	public boolean returns(Class<?> clazz) {
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
		MethodDeclaration methodDecl = (MethodDeclaration)get();
		return methodDecl.returnType;
	}

	public boolean isSynchronized() {
		return !isConstructor() && (get().bits & IsSynchronized) != 0;
	}
	
	public boolean isStatic() {
		return !isConstructor() && (get().modifiers & ClassFileConstants.AccStatic) != 0;
	}

	public boolean isConstructor() {
		return get() instanceof ConstructorDeclaration;
	}

	public boolean isAbstract() {
		return get().isAbstract();
	}

	public boolean isEmpty() {
		return Arrays.isEmpty(get().statements);
	}

	public AbstractMethodDeclaration get() {
		return (AbstractMethodDeclaration)methodNode.get();
	}

	public EclipseNode node() {
		return methodNode;
	}

	public boolean hasNonFinalArgument() {
		if (hasArguments()) for (Argument arg : get().arguments) {
			if ((arg.modifiers & AccFinal) == 0) {
				return true;
			}
		}
		return false;
	}

	public boolean hasArguments() {
		return isNotEmpty(get().arguments);
	}

	public String name() {
		return new String(get().selector);
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

	public void body(lombok.ast.Statement... statements) {
		body(list(statements));
	}

	public void body(List<lombok.ast.Statement> statements) {
		setGeneratedByAndCopyPos(get(), source);
		get().bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		get().statements = builder.build(statements).toArray(new Statement[0]);
		final List<Annotation> annotations = new ArrayList<Annotation>();
		Annotation[] originalAnnotations = get().annotations;
		if (originalAnnotations != null) for (Annotation originalAnnotation : originalAnnotations) {
			if (!originalAnnotation.type.toString().endsWith("SuppressWarnings")) {
				annotations.add(originalAnnotation);
			}
		}
		annotations.add(builder.build(Annotation(Type("java.lang.SuppressWarnings")).withValue(String("all")), Annotation.class));
		get().annotations = annotations.toArray(new Annotation[0]);
	}

	public void body(final lombok.ast.Block body) {
		body(body.getStatements());
	}

	public void rebuild() {
		node().rebuild();
	}

	public EclipseType surroundingType() {
		return EclipseType.typeOf(node(), source);
	}

	public List<lombok.ast.Statement> statements() {
		final List<lombok.ast.Statement> methodStatements = new ArrayList<lombok.ast.Statement>();
		if (isNotEmpty(get().statements)) for (Object statement : get().statements) {
			methodStatements.add(Stat(statement));
		}
		return methodStatements;
	}

	public List<lombok.ast.Annotation> annotations() {
		final List<lombok.ast.Annotation> annotations = new ArrayList<lombok.ast.Annotation>();
		if (isNotEmpty(get().annotations)) for (Annotation annotation : get().annotations) {
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

	public List<lombok.ast.Argument> arguments() {
		final List<lombok.ast.Argument> methodArguments = new ArrayList<lombok.ast.Argument>();
		if (isNotEmpty(get().arguments)) for (Argument argument : get().arguments) {
			methodArguments.add(Arg(Type(argument.type), new String(argument.name)));
		}
		return methodArguments;
	}

	public List<lombok.ast.TypeRef> thrownExceptions() {
		final List<lombok.ast.TypeRef> thrownExceptions = new ArrayList<lombok.ast.TypeRef>();
		if (isNotEmpty(get().thrownExceptions)) for (Object thrownException : get().thrownExceptions) {
			thrownExceptions.add(Type(thrownException));
		}
		return thrownExceptions;
	}

	@Override
	public String toString() {
		return get().toString();
	}

	public static EclipseMethod methodOf(final EclipseNode node, final ASTNode source) {
		EclipseNode methodNode = node;
		while ((methodNode != null) && !(methodNode.get() instanceof AbstractMethodDeclaration)) {
			methodNode = methodNode.up();
		}
		return methodNode == null ? null : new EclipseMethod(methodNode, source);
	}
}
