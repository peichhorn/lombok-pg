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
package lombok.javac.handlers.replace;

import lombok.javac.handlers.ast.JavacMethod;

import com.sun.source.tree.*;
import com.sun.tools.javac.tree.JCTree.*;

public abstract class StatementReplaceVisitor extends ReplaceVisitor<JCStatement> {

	protected StatementReplaceVisitor(final JavacMethod method, final lombok.ast.Statement replacement) {
		super(method, replacement);
	}

	@Override public Void visitBlock(final BlockTree tree, final Void p) {
		JCBlock block = (JCBlock) tree;
		block.stats = replace(block.stats);
		return super.visitBlock(tree, p);
	}

	@Override public Void visitCase(final CaseTree tree, final Void p) {
		JCCase caseTree = (JCCase) tree;
		caseTree.stats = replace(caseTree.stats);
		return super.visitCase(tree, p);
	}

	@Override public Void visitDoWhileLoop(final DoWhileLoopTree tree, final Void p) {
		JCDoWhileLoop doWhileLoop = (JCDoWhileLoop) tree;
		doWhileLoop.body = replace(doWhileLoop.body);
		return super.visitDoWhileLoop(tree, p);
	}

	@Override public Void visitEnhancedForLoop(final EnhancedForLoopTree tree, final Void p) {
		JCEnhancedForLoop enhancedForLoop = (JCEnhancedForLoop) tree;
		enhancedForLoop.body = replace(enhancedForLoop.body);
		return super.visitEnhancedForLoop(tree, p);
	}

	@Override public Void visitForLoop(final ForLoopTree tree, final Void p) {
		JCForLoop forLoop = (JCForLoop) tree;
		forLoop.body = replace(forLoop.body);
		return super.visitForLoop(tree, p);
	}

	@Override public Void visitIf(final IfTree tree, final Void p) {
		JCIf ifTree = (JCIf) tree;
		ifTree.thenpart = replace(ifTree.thenpart);
		ifTree.elsepart = replace(ifTree.elsepart);
		return super.visitIf(tree, p);
	}

	@Override public Void visitWhileLoop(final WhileLoopTree tree, final Void p) {
		JCWhileLoop whileLoop = (JCWhileLoop) tree;
		whileLoop.body = replace(whileLoop.body);
		return super.visitWhileLoop(tree, p);
	}
}
