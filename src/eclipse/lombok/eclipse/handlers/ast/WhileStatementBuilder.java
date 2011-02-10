package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class WhileStatementBuilder implements StatementBuilder<WhileStatement> {
	private final ExpressionBuilder<? extends Expression> condition;
	private StatementBuilder<? extends Statement> action = new EmptyStatementBuilder();
	
	public WhileStatementBuilder Do(StatementBuilder<? extends Statement> action) {
		this.action = action;
		return this;
	}
	
	public WhileStatementBuilder Do(Statement action) {
		return Do(new StatementWrapper<Statement>(action));
	}
	
	@Override
	public WhileStatement build(final EclipseNode node, final ASTNode source) {
		WhileStatement whileStatement = new WhileStatement(condition.build(node, source), action.build(node, source), 0, 0);
		setGeneratedByAndCopyPos(whileStatement, source);
		return whileStatement;
	}
}
