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
public final class IfStatementBuilder implements StatementBuilder<IfStatement> {
	private final ExpressionBuilder<? extends Expression> condition;
	private StatementBuilder<? extends Statement> thenStatement = new EmptyStatementBuilder();
	private StatementBuilder<? extends Statement> elseStatement;
	
	public IfStatementBuilder Then(StatementBuilder<? extends Statement> thenStatement) {
		this.thenStatement = thenStatement;
		return this;
	}
	
	public IfStatementBuilder Else(StatementBuilder<? extends Statement> elseStatement) {
		this.elseStatement = elseStatement;
		return this;
	}
	
	@Override
	public IfStatement build(final EclipseNode node, final ASTNode source) {
		IfStatement ifStatement = new IfStatement(condition.build(node, source), thenStatement.build(node, source), 0, 0);
		if (elseStatement != null) {
			ifStatement.elseStatement = elseStatement.build(node, source);
		}
		setGeneratedByAndCopyPos(ifStatement, source);
		return ifStatement;
	}
}
