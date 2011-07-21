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

import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.DoStatement;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;

public class ReturnStatementReplaceVisitor extends ReplaceVisitor<Statement> {

	public ReturnStatementReplaceVisitor(EclipseMethod method, lombok.ast.Statement replacement) {
		super(method, replacement);
	}

	@Override public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		replace(methodDeclaration.statements);
		return true;
	}

	@Override public boolean visit(Block block, BlockScope scope) {
		replace(block.statements);
		return true;
	}

	@Override public boolean visit(DoStatement doStatement, BlockScope scope) {
		doStatement.action = replace(doStatement.action);
		return true;
	}

	@Override public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		whileStatement.action = replace(whileStatement.action);
		return true;
	}

	@Override public boolean visit(ForeachStatement forStatement, BlockScope scope) {
		forStatement.action = replace(forStatement.action);
		return true;
	}

	@Override public boolean visit(ForStatement forStatement, BlockScope scope) {
		forStatement.action = replace(forStatement.action);
		return true;
	}

	@Override public boolean visit(IfStatement ifStatement, BlockScope scope) {
		ifStatement.thenStatement = replace(ifStatement.thenStatement);
		ifStatement.elseStatement = replace(ifStatement.elseStatement);
		return true;
	}

	@Override protected boolean needsReplacing(Statement node) {
		return node instanceof ReturnStatement;
	}
}
