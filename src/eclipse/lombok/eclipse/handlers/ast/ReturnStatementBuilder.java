package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ReturnStatementBuilder implements StatementBuilder<ReturnStatement>{
	private ExpressionBuilder<? extends Expression> expression = null;
	
	@Override
	public ReturnStatement build(final EclipseNode node, final ASTNode source) {
		ReturnStatement returnStatement = new ReturnStatement((expression == null) ? null : expression.build(node, source), 0, 0);
		setGeneratedByAndCopyPos(returnStatement, source);
		return returnStatement;
	}
}
