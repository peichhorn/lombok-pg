package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.DoStatement;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.Statement;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class DoStatementBuilder implements StatementBuilder<DoStatement> {
	private final StatementBuilder<? extends Statement> action;
	private ExpressionBuilder<? extends Expression> condition = new MagicLiteralBuilder(true);
	
	public DoStatementBuilder While(ExpressionBuilder<? extends Expression> condition) {
		this.condition = condition;
		return this;
	}
	
	@Override
	public DoStatement build(final EclipseNode node, final ASTNode source) {
		DoStatement doStatement = new DoStatement(condition.build(node, source), action.build(node, source), 0, 0);
		setGeneratedByAndCopyPos(doStatement, source);
		return doStatement;
	}
}
