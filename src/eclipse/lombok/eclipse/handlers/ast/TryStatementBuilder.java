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

import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;

@RequiredArgsConstructor
public class TryStatementBuilder implements StatementBuilder<TryStatement> {
	private final StatementBuilder<? extends Block> tryBlock;
	private StatementBuilder<? extends Block> finallyBlock;
	private final List<StatementBuilder<? extends Argument>> catchArguments = new ArrayList<StatementBuilder<? extends Argument>>();
	private final List<StatementBuilder<? extends Block>> catchBlocks = new ArrayList<StatementBuilder<? extends Block>>();
	
	public TryStatementBuilder Catch(final StatementBuilder<? extends Argument> catchArgument, final StatementBuilder<? extends Block> catchBlock) {
		catchArguments.add(catchArgument);
		catchBlocks.add(catchBlock);
		return this;
	}
	
	public TryStatementBuilder Finally(final StatementBuilder<? extends Block> finallyBlock) {
		this.finallyBlock = finallyBlock;
		return this;
	}
	
	@Override
	public TryStatement build(EclipseNode node, ASTNode source) {
		final TryStatement tryStatement = new TryStatement();
		setGeneratedByAndCopyPos(tryStatement, source);
		tryStatement.tryBlock = tryBlock.build(node, source);
		tryStatement.catchArguments = buildArray(catchArguments, new Argument[0], node, source);
		tryStatement.catchBlocks = buildArray(catchBlocks, new Block[0], node, source);
		if (finallyBlock != null) {
			tryStatement.finallyBlock = finallyBlock.build(node, source);
		}
		return tryStatement;
	}
}
