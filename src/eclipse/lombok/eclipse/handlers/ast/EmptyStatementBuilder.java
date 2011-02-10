package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.EmptyStatement;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class EmptyStatementBuilder implements StatementBuilder<EmptyStatement> {
	@Override
	public EmptyStatement build(final EclipseNode node, final ASTNode source) {
		final EmptyStatement emptyStatement = new EmptyStatement(0, 0);
		setGeneratedByAndCopyPos(emptyStatement, source);
		return emptyStatement;
	}
}
