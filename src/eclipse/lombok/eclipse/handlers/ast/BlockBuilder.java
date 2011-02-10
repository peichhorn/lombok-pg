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
	
	public BlockBuilder withStatement(StatementBuilder<? extends Statement> statement) {
		this.statements.add(statement);
		return this;
	}
	
	public BlockBuilder withStatements(List<StatementBuilder<? extends Statement>> statements) {
		this.statements.addAll(statements);
		return this;
	}
	
	public BlockBuilder withStatements(Statement... statements) {
		if (statements != null) for (Statement statement : statements) {
			this.statements.add(new StatementWrapper<Statement>(statement));
		}
		return this;
	}
	
	@Override
	public Block build(EclipseNode node, ASTNode source) {
		Block block = new Block(0);
		setGeneratedByAndCopyPos(block, source);
		block.statements = buildArray(statements, new Statement[0], node, source);
		return block;
	}
}
