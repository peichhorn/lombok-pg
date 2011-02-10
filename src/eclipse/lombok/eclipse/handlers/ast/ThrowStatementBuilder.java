package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class ThrowStatementBuilder implements StatementBuilder<ThrowStatement> {
	private final ExpressionBuilder<? extends Expression> init;
	
	@Override
	public ThrowStatement build(final EclipseNode node, final ASTNode source) {
		ThrowStatement throwStatement = new ThrowStatement(init.build(node, source), 0, 0);
		setGeneratedByAndCopyPos(throwStatement, source);
		return throwStatement;
	}
}
