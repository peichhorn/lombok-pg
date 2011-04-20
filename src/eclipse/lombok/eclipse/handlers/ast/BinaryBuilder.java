package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class BinaryBuilder implements ExpressionBuilder<BinaryExpression> {
	private final String operator;
	private final ExpressionBuilder<? extends Expression> left;
	private final ExpressionBuilder<? extends Expression> right;
	
	@Override
	public BinaryExpression build(EclipseNode node, ASTNode source) {
		final int opCode;
		if ("+".equals(operator)) {
			opCode = OperatorIds.PLUS;
		} else if ("-".equals(operator)) {
			opCode = OperatorIds.MINUS;
		} else if ("*".equals(operator)) {
			opCode = OperatorIds.MULTIPLY;
		} else if ("/".equals(operator)) {
			opCode = OperatorIds.DIVIDE;
		} else {
			opCode = 0;
		}
		BinaryExpression binaryExpression = new BinaryExpression(left.build(node, source), right.build(node, source), opCode);
		setGeneratedByAndCopyPos(binaryExpression, source);
		return binaryExpression;
	}
}
