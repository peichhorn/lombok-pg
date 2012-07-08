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
import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.ast.AST.*;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;

import lombok.core.util.As;
import lombok.core.util.Each;
import lombok.eclipse.handlers.replace.ReturnStatementReplaceVisitor;
import lombok.eclipse.handlers.replace.ThisReferenceReplaceVisitor;
import lombok.eclipse.handlers.replace.VariableNameReplaceVisitor;

public final class EclipseMethodEditor implements lombok.ast.IMethodEditor<ASTNode> {
	private final EclipseMethod method;
	private final EclipseASTMaker builder;

	EclipseMethodEditor(final EclipseMethod method, final ASTNode source) {
		this.method = method;
		builder = new EclipseASTMaker(method.node(), source);
	}

	AbstractMethodDeclaration get() {
		return method.get();
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

	public void replaceReturnType(final lombok.ast.TypeRef returnType) {
		if (method.isConstructor()) return;
		MethodDeclaration methodDecl = (MethodDeclaration) get();
		methodDecl.returnType = build(returnType);
	}

	public void replaceReturns(final lombok.ast.Statement<?> replacement) {
		new ReturnStatementReplaceVisitor(method, replacement).visit(get());
	}

	@Override
	public void replaceVariableName(final String oldName, final String newName) {
		new VariableNameReplaceVisitor(method, oldName, newName).visit(get());
	}

	public void forceQualifiedThis() {
		new ThisReferenceReplaceVisitor(method, This(Type(method.surroundingType().name()))).visit(get());
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

	public void replaceArguments(final lombok.ast.Argument... arguments) {
		replaceArguments(As.list(arguments));
	}

	public void replaceArguments(final List<lombok.ast.Argument> arguments) {
		get().arguments = build(arguments).toArray(new Argument[0]);
	}

	public void replaceBody(final lombok.ast.Statement<?>... statements) {
		replaceBody(As.list(statements));
	}

	public void replaceBody(final List<lombok.ast.Statement<?>> statements) {
		get().bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		get().statements = build(statements).toArray(new Statement[0]);
		final List<Annotation> annotations = new ArrayList<Annotation>();
		Annotation[] originalAnnotations = get().annotations;
		for (Annotation originalAnnotation : Each.elementIn(originalAnnotations)) {
			if (!originalAnnotation.type.toString().endsWith("SuppressWarnings")) {
				annotations.add(originalAnnotation);
			}
		}
		annotations.add(build(Annotation(Type("java.lang.SuppressWarnings")).withValue(String("all")), Annotation.class));
		get().annotations = annotations.toArray(new Annotation[0]);
	}

	public void replaceBody(final lombok.ast.Block body) {
		replaceBody(body.getStatements());
	}

	public void rebuild() {
		method.node().rebuild();
	}

	@Override
	public String toString() {
		return get().toString();
	}
}
