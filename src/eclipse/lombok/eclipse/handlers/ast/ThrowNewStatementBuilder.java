package lombok.eclipse.handlers.ast;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class ThrowNewStatementBuilder implements StatementBuilder<ThrowStatement> {
	private final AllocationExpressionBuilder init;
	
	public ThrowNewStatementBuilder withArgument(final ExpressionBuilder<? extends Expression> arg) {
		init.withArgument(arg);
		return this;
	}
	
	public ThrowNewStatementBuilder withTypeArgument(final TypeReferenceBuilder typeArg) {
		init.withTypeArgument(typeArg);
		return this;
	}
	
	@Override
	public ThrowStatement build(final EclipseNode node, final ASTNode source) {
		return new ThrowStatementBuilder(init).build(node, source);
	}
}
