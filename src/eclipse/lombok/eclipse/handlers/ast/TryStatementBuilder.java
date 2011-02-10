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
