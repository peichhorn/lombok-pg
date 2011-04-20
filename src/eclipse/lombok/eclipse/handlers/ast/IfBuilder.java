/*
 * Copyright Â© 2011 Philipp Eichhorn
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

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.Statement;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class IfBuilder implements StatementBuilder<IfStatement> {
	private StatementBuilder<? extends Statement> thenStatement = new EmptyStatementBuilder();
	private final ExpressionBuilder<? extends Expression> condition;
	private StatementBuilder<? extends Statement> elseStatement;
	
	public IfBuilder Then(final StatementBuilder<? extends Statement> thenStatement) {
		this.thenStatement = thenStatement;
		return this;
	}
	
	public IfBuilder Else(final StatementBuilder<? extends Statement> elseStatement) {
		this.elseStatement = elseStatement;
		return this;
	}
	
	@Override
	public IfStatement build(final EclipseNode node, final ASTNode source) {
		final IfStatement ifStatement = new IfStatement(condition.build(node, source), thenStatement.build(node, source), 0, 0);
		if (elseStatement != null) {
			ifStatement.elseStatement = elseStatement.build(node, source);
		}
		setGeneratedByAndCopyPos(ifStatement, source);
		return ifStatement;
	}
}
