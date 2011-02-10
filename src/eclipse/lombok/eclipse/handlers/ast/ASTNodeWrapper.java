package lombok.eclipse.handlers.ast;

import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;

@RequiredArgsConstructor
public class ASTNodeWrapper<NODE_TYPE extends ASTNode> implements ASTNodeBuilder<NODE_TYPE> {
	private final NODE_TYPE astNode;
	
	@Override
	public NODE_TYPE build(EclipseNode node, ASTNode source) {
		return astNode;
	}
}