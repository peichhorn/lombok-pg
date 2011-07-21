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
package lombok.javac.handlers;

import lombok.javac.handlers.ast.JavacMethod;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;

public class ReturnStatementReplaceVisitor extends ReplaceVisitor<JCStatement> {

	public ReturnStatementReplaceVisitor(JavacMethod method, lombok.ast.Statement replacement) {
		super(method, replacement);
	}

	@Override public Void visitBlock(BlockTree tree, Void p) {
		JCBlock block = (JCBlock) tree;
		block.stats = replace(block.stats);
		return super.visitBlock(block, p);
	}

	@Override public Void visitDoWhileLoop(DoWhileLoopTree tree, Void p) {
		JCDoWhileLoop doWhileLoop = (JCDoWhileLoop) tree;
		doWhileLoop.body = replace(doWhileLoop.body);
		return super.visitDoWhileLoop(doWhileLoop, p);
	}

	@Override public Void visitWhileLoop(WhileLoopTree tree, Void p) {
		JCWhileLoop whileLoop = (JCWhileLoop) tree;
		whileLoop.body = replace(whileLoop.body);
		return super.visitWhileLoop(whileLoop, p);
	}

	@Override public Void visitForLoop(ForLoopTree tree, Void p) {
		JCForLoop forLoop = (JCForLoop) tree;
		forLoop.body = replace(forLoop.body);
		return super.visitForLoop(forLoop, p);
	}

	@Override public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
		JCEnhancedForLoop enhancedForLoop = (JCEnhancedForLoop) tree;
		enhancedForLoop.body = replace(enhancedForLoop.body);
		return super.visitEnhancedForLoop(enhancedForLoop, p);
	}

	@Override public Void visitCase(CaseTree tree, Void p) {
		JCCase caseTree = (JCCase) tree;
		caseTree.stats = replace(caseTree.stats);
		return super.visitCase(caseTree, p);
	}

	@Override public Void visitIf(IfTree tree, Void p) {
		JCIf ifTree = (JCIf) tree;
		ifTree.thenpart = replace(ifTree.thenpart);
		ifTree.elsepart = replace(ifTree.elsepart);
		return super.visitIf(ifTree, p);
	}

	@Override protected boolean needsReplacing(JCStatement node) {
		return node instanceof JCReturn;
	}
}
