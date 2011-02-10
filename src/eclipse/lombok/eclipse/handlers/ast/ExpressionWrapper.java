package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;

@RequiredArgsConstructor
public class ExpressionWrapper<NODE_TYPE extends Expression>  implements ExpressionBuilder<NODE_TYPE> {
	private final NODE_TYPE expression;
	
	@Override
	public NODE_TYPE build(EclipseNode node, ASTNode source) {
		setGeneratedByAndCopyPos(expression, source);
		return expression;
	}
}