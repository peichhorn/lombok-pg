package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.ContinueStatement;

@NoArgsConstructor
@AllArgsConstructor
public class ContinueStatementBuilder implements StatementBuilder<ContinueStatement> {
	public String label;
	
	@Override
	public ContinueStatement build(EclipseNode node, ASTNode source) {
		ContinueStatement continueStatement = new ContinueStatement(label == null ? null : label.toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(continueStatement, source);
		return continueStatement;
	}
}
