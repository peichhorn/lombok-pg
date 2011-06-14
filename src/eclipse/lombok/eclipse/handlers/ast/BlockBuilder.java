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
import static lombok.eclipse.handlers.ast.Arrays.buildArray;

import java.util.ArrayList;
import java.util.List;

import lombok.NoArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.Statement;

@NoArgsConstructor
public class BlockBuilder implements StatementBuilder<Block> {
	private final List<StatementBuilder<? extends Statement>> statements = new ArrayList<StatementBuilder<? extends Statement>>();

	public BlockBuilder withStatement(final StatementBuilder<? extends Statement> statement) {
		this.statements.add(statement);
		return this;
	}

	public BlockBuilder withStatements(final List<StatementBuilder<? extends Statement>> statements) {
		this.statements.addAll(statements);
		return this;
	}

	public BlockBuilder withStatements(final Statement... statements) {
		if (statements != null) for (final Statement statement : statements) {
			this.statements.add(new StatementWrapper<Statement>(statement));
		}
		return this;
	}

	@Override
	public Block build(final EclipseNode node, final ASTNode source) {
		final Block block = new Block(0);
		setGeneratedByAndCopyPos(block, source);
		block.statements = buildArray(statements, new Statement[0], node, source);
		return block;
	}
}
