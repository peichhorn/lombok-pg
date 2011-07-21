/*
 * Copyright © 2010-2011 Philipp Eichhorn
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
package lombok.javac.handlers;

import lombok.javac.handlers.ast.JavacMethod;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;

/**
 * Replaces all implicit and explicit occurrences of 'this' with a specified expression.
 */
//TODO incomplete, but works for the current use-case
public class ThisReferenceReplaceVisitor extends ReplaceVisitor<JCExpression> {

	public ThisReferenceReplaceVisitor(JavacMethod method, lombok.ast.Statement replacement) {
		super(method, replacement);
	}

	@Override
	public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
		JCMethodInvocation methodInvocation = (JCMethodInvocation)tree;
		methodInvocation.args = replace(methodInvocation.args);
		return super.visitMethodInvocation(tree, p);
	}

	@Override
	public Void visitNewClass(NewClassTree tree, Void p) {
		JCNewClass newClass = (JCNewClass)tree;
		newClass.args = replace(newClass.args);
		return super.visitNewClass(tree, p);
	}

	@Override
	protected boolean needsReplacing(JCExpression node) {
		return (node instanceof JCIdent) && "this".equals(node.toString());
	}
}
