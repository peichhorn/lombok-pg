package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Statement;

@RequiredArgsConstructor
public class StatementWrapper<NODE_TYPE extends Statement> implements StatementBuilder<NODE_TYPE> {
	private final NODE_TYPE statement;
	
	@Override
	public NODE_TYPE build(EclipseNode node, ASTNode source) {
		setGeneratedByAndCopyPos(statement, source);
		return statement;
	}
}