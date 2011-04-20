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
package lombok.eclipse.handlers;

import static org.eclipse.jdt.core.dom.Modifier.PRIVATE;
import static org.eclipse.jdt.core.dom.Modifier.PROTECTED;
import static org.eclipse.jdt.core.dom.Modifier.PUBLIC;
import static org.eclipse.jdt.internal.compiler.ast.ASTNode.IsSynchronized;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccFinal;
import static lombok.core.util.Arrays.*;
import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import static lombok.eclipse.handlers.ast.ASTBuilder.Annotation;
import static lombok.eclipse.handlers.ast.ASTBuilder.String;
import static lombok.eclipse.handlers.ast.ASTBuilder.Type;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

import lombok.core.util.Arrays;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.ExpressionBuilder;
import lombok.eclipse.handlers.ast.StatementBuilder;

public class EclipseMethod {
	private final EclipseNode methodNode;

	private EclipseMethod(final EclipseNode methodNode) {
		if (!(methodNode.get() instanceof AbstractMethodDeclaration)) {
			throw new IllegalArgumentException();
		}
		this.methodNode = methodNode;
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
	
	public TypeReference returnType() {
		if (isConstructor()) return null;
		MethodDeclaration methodDecl = (MethodDeclaration)get();
		return methodDecl.returnType;
	}

	public boolean isSynchronized() {
		return !isConstructor() && (get().bits & IsSynchronized) != 0;
	}

	public boolean isConstructor() {
		return get() instanceof ConstructorDeclaration;
	}

	public AbstractMethodDeclaration get() {
		return (AbstractMethodDeclaration)methodNode.get();
	}

	public EclipseNode node() {
		return methodNode;
	}

	public boolean hasNonFinalParameter() {
		if (get().arguments != null) for (Argument arg : get().arguments) {
			if ((arg.modifiers & AccFinal) == 0) {
				return true;
			}
		}
		return false;
	}

	public boolean isAbstract() {
		return get().isAbstract();
	}

	public boolean isEmpty() {
		return Arrays.isEmpty(get().statements);
	}

	public String name() {
		return new String(get().selector);
	}

	public boolean wasCompletelyParsed() {
		return node().getAst().isCompleteParse();
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

	public void body(ASTNode source, Statement... statements) {
		setGeneratedByAndCopyPos(get(), source);
		get().bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		get().statements = statements;
		Annotation[] originalAnnotationArray = get().annotations;
		Annotation ann = Annotation(Type("java.lang.SuppressWarnings")).withValue(String("all")).build(node(), source);
		if (originalAnnotationArray == null) {
			get().annotations = array(ann);
			return;
		}
		get().annotations = resize(originalAnnotationArray, originalAnnotationArray.length + 1);
		get().annotations [originalAnnotationArray.length] = ann;
	}
	
	public void body(ASTNode source, final StatementBuilder<? extends Block> body) {
		body(source, body.build(node(), source).statements);
	}
	
	public void withException(final ExpressionBuilder<? extends TypeReference> exception) {
		TypeReference[] originalThrownExceptionsArray = get().thrownExceptions;
		TypeReference thrown = exception.build(node(), get());
		if (originalThrownExceptionsArray == null) {
			get().thrownExceptions = array(thrown);
			return;
		}
		get().thrownExceptions = resize(originalThrownExceptionsArray, originalThrownExceptionsArray.length + 1);
		get().thrownExceptions[originalThrownExceptionsArray.length] = thrown;
	}

	public void rebuild() {
		node().rebuild();
	}

	@Override
	public String toString() {
		return get().toString();
	}

	public static EclipseMethod methodOf(final EclipseNode node) {
		EclipseNode methodNode = node;
		while ((methodNode != null) && !(methodNode.get() instanceof AbstractMethodDeclaration)) {
			methodNode = methodNode.up();
		}
		return methodNode == null ? null : new EclipseMethod(methodNode);
	}
}
