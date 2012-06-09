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
package lombok.eclipse.handlers.replace;

import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

public abstract class StatementReplaceVisitor extends ReplaceVisitor<Statement> {

	protected StatementReplaceVisitor(final EclipseMethod method, final lombok.ast.Statement<?> replacement) {
		super(method, replacement);
	}

	@Override
	public boolean visit(final ConstructorDeclaration constructorDeclaration, final ClassScope scope) {
		replace(constructorDeclaration.statements);
		return true;
	}

	@Override
	public boolean visit(final MethodDeclaration methodDeclaration, final ClassScope scope) {
		replace(methodDeclaration.statements);
		return true;
	}

	@Override
	public boolean visit(final Block block, final BlockScope scope) {
		replace(block.statements);
		return true;
	}

	@Override
	public boolean visit(final DoStatement doStatement, final BlockScope scope) {
		doStatement.action = replace(doStatement.action);
		return true;
	}

	@Override
	public boolean visit(final ForeachStatement forStatement, final BlockScope scope) {
		forStatement.action = replace(forStatement.action);
		return true;
	}

	@Override
	public boolean visit(final ForStatement forStatement, final BlockScope scope) {
		forStatement.action = replace(forStatement.action);
		return true;
	}

	@Override
	public boolean visit(final IfStatement ifStatement, final BlockScope scope) {
		ifStatement.thenStatement = replace(ifStatement.thenStatement);
		ifStatement.elseStatement = replace(ifStatement.elseStatement);
		return true;
	}

	@Override
	public boolean visit(final SwitchStatement switchStatement, final BlockScope scope) {
		replace(switchStatement.statements);
		return true;
	}

	@Override
	public boolean visit(final WhileStatement whileStatement, final BlockScope scope) {
		whileStatement.action = replace(whileStatement.action);
		return true;
	}
}
