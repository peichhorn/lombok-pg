package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class UnaryExpressionBuilder implements ExpressionBuilder<UnaryExpression> {
	private final int operator;
	private final ExpressionBuilder<? extends Expression> expression;
	
	@Override
	public UnaryExpression build(final EclipseNode node, final ASTNode source) {
		UnaryExpression newExpression = new UnaryExpression(expression.build(node, source), operator);
		setGeneratedByAndCopyPos(newExpression, source);
		return newExpression;
	}
}
