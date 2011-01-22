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

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

/**
 * Replaces all implicit and explicit occurrences of 'this' with a specified expression.
 */
//TODO incomplete, but works for the current use-case
public class ThisReferenceReplaceVisitor extends TreeScanner<Void, Void> {
	private final JCExpression replacement;
	
	public ThisReferenceReplaceVisitor(final JCExpression replacement) {
		super();
		this.replacement = replacement;
	}
	
	public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
		if (tree instanceof JCMethodInvocation) {
			JCMethodInvocation methodInvocation = (JCMethodInvocation)tree;
			methodInvocation.args = replaceArgs(methodInvocation.args);
		}
		return super.visitMethodInvocation(tree, p);
	}
	
	public Void visitNewClass(NewClassTree tree, Void p) {
		if (tree instanceof JCNewClass) {
			JCNewClass newClass = (JCNewClass)tree;
			newClass.args = replaceArgs(newClass.args);
		}
		return super.visitNewClass(tree, p);
	}
	
	private List<JCExpression> replaceArgs(List<JCExpression> args) {
		ListBuffer<JCExpression> newArgs = ListBuffer.lb();
		for (JCExpression arg : args) {
			if ((arg instanceof JCIdent) && ("this".equals(arg.toString()))) {
				arg = replacement;
			}
			newArgs.append(arg);
		}
		return newArgs.toList();
	}
}

