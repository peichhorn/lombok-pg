/*
 * Copyright © 2012 Philipp Eichhorn
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
import static lombok.javac.handlers.Javac.*;

import java.util.List;

import lombok.core.util.As;
import lombok.core.util.Is;
import lombok.javac.JavacNode;
import lombok.javac.handlers.replace.*;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCStatement;

public final class JavacMethodEditor implements lombok.ast.IMethodEditor<JCTree> {
	private final JavacMethod method;
	private final JavacASTMaker builder;

	JavacMethodEditor(final JavacMethod method, final JCTree source) {
		this.method = method;
		builder = new JavacASTMaker(method.node(), source);
	}

	public JCMethodDecl get() {
		return method.get();
	}

	public JavacNode node() {
		return method.node();
	}

	public <T extends JCTree> T build(final lombok.ast.Node<?> node) {
		return builder.<T> build(node);
	}

	public <T extends JCTree> T build(final lombok.ast.Node<?> node, final Class<T> extectedType) {
		return builder.build(node, extectedType);
	}

	public <T extends JCTree> List<T> build(final List<? extends lombok.ast.Node<?>> nodes) {
		return builder.build(nodes);
	}

	public <T extends JCTree> List<T> build(final List<? extends lombok.ast.Node<?>> nodes, final Class<T> extectedType) {
		return builder.build(nodes, extectedType);
	}

	public void replaceReturnType(final lombok.ast.TypeRef returnType) {
		if (method.isConstructor()) return;
		get().restype = build(returnType);
	}

	public void replaceReturns(final lombok.ast.Statement<?> replacement) {
		new ReturnStatementReplaceVisitor(method, replacement).visit(get());
	}

	public void replaceVariableName(final String oldName, final String newName) {
		new VariableNameReplaceVisitor(method, oldName, newName).visit(get());
	}

	public void forceQualifiedThis() {
		new ThisReferenceReplaceVisitor(method, This(Type(method.surroundingType().name()))).visit(get());
	}

	public void makePrivate() {
		makePackagePrivate();
		get().mods.flags |= PRIVATE;
	}

	public void makePackagePrivate() {
		get().mods.flags &= ~(PRIVATE | PROTECTED | PUBLIC);
	}

	public void makeProtected() {
		makePackagePrivate();
		get().mods.flags |= PROTECTED;
	}

	public void makePublic() {
		makePackagePrivate();
		get().mods.flags |= PUBLIC;
	}

	public void replaceBody(final lombok.ast.Statement<?>... statements) {
		replaceBody(As.list(statements));
	}

	public void replaceBody(final List<lombok.ast.Statement<?>> statements) {
		replaceBody(Block().withStatements(statements));
	}

	public void replaceBody(final lombok.ast.Block body) {
		final lombok.ast.Block bodyWithConstructorCall = new lombok.ast.Block();
		if (!method.isEmpty()) {
			final JCStatement suspect = get().body.stats.get(0);
			if (isConstructorCall(suspect)) bodyWithConstructorCall.withStatement(Stat(suspect));
		}
		bodyWithConstructorCall.withStatements(body.getStatements());
		get().body = builder.build(bodyWithConstructorCall);
		addSuppressWarningsAll(get().mods, node(), get().pos);
	}

	private boolean isConstructorCall(final JCStatement supect) {
		if (!(supect instanceof JCExpressionStatement)) return false;
		final JCExpression supectExpression = ((JCExpressionStatement) supect).expr;
		if (!(supectExpression instanceof JCMethodInvocation)) return false;
		return Is.oneOf(((JCMethodInvocation) supectExpression).meth.toString(), "super", "this");
	}

	public void rebuild() {
		node().rebuild();
	}

	@Override
	public String toString() {
		return get().toString();
	}
}
