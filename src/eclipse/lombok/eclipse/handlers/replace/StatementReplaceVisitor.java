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
package lombok.eclipse.handlers.replace;

import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

public abstract class StatementReplaceVisitor extends ReplaceVisitor<Statement> {

	protected StatementReplaceVisitor(EclipseMethod method, lombok.ast.Statement replacement) {
		super(method, replacement);
	}

	@Override public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
		replace(constructorDeclaration.statements);
		return true;
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

	@Override public boolean visit(SwitchStatement switchStatement, BlockScope scope) {
		replace(switchStatement.statements);
		return true;
	}

	@Override public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		whileStatement.action = replace(whileStatement.action);
		return true;
	}
}
