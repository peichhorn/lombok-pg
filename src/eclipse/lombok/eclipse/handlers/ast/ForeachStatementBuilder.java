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
package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;

@RequiredArgsConstructor
public class ForeachStatementBuilder implements StatementBuilder<ForeachStatement> {
	private final StatementBuilder<? extends LocalDeclaration> elementVariable;
	private ExpressionBuilder<? extends Expression> collection;
	private StatementBuilder<? extends Statement> action;

	public ForeachStatementBuilder In(final ExpressionBuilder<? extends Expression> collection) {
		this.collection = collection;
		return this;
	}
	
	public ForeachStatementBuilder Do(final StatementBuilder<? extends Statement> action) {
		this.action = action;
		return this;
	}
	
	@Override
	public ForeachStatement build(EclipseNode node, ASTNode source) {
		ForeachStatement forEach = new ForeachStatement(elementVariable.build(node, source), 0);
		setGeneratedByAndCopyPos(forEach, source);
		forEach.collection = collection.build(node, source);
		forEach.action = action.build(node, source);
		return forEach;
	}
}
