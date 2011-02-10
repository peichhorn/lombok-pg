package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.EqualExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class EqualExpressionBuilder implements ExpressionBuilder<EqualExpression> {
	private final ExpressionBuilder<? extends Expression> left;
	private final ExpressionBuilder<? extends Expression> right;
	private final int operator;

	@Override
	public EqualExpression build(final EclipseNode node, final ASTNode source) {
		EqualExpression equalExpression = new EqualExpression(left.build(node, source), right.build(node, source), operator);
		setGeneratedByAndCopyPos(equalExpression, source);
		return equalExpression;
	}
}
