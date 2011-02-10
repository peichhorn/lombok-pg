package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

@RequiredArgsConstructor
public class CastExpressionBuilder implements ExpressionBuilder<CastExpression> {
	private final ExpressionBuilder<? extends TypeReference> type;
	private final ExpressionBuilder<? extends Expression> expression;
	
	@Override
	public CastExpression build(EclipseNode node, ASTNode source) {
		CastExpression castExpression = new CastExpression(expression.build(node, source), type.build(node, source));
		setGeneratedByAndCopyPos(castExpression, source);
		return castExpression;
	}
}
