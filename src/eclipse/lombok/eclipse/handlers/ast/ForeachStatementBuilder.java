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
