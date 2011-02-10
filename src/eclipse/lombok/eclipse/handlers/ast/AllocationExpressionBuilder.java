package lombok.eclipse.handlers.ast;

import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.handlers.ast.Arrays.buildArray;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class AllocationExpressionBuilder extends GenericTypeArgumentBuilder<AllocationExpressionBuilder, AllocationExpression> {
	private final ExpressionBuilder<? extends TypeReference> type;
	private final List<ExpressionBuilder<? extends Expression>> args = new ArrayList<ExpressionBuilder<? extends Expression>>();
	
	public AllocationExpressionBuilder withArgument(final ExpressionBuilder<? extends Expression> arg) {
		args.add(arg);
		return self();
	}
	
	@Override
	public final AllocationExpression build(final EclipseNode node, final ASTNode source) {
		final AllocationExpression initException = new AllocationExpression();
		setGeneratedByAndCopyPos(initException, source);
		initException.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		initException.type = type.build(node, source);
		initException.typeArguments = buildTypeArguments(node, source);
		initException.arguments = buildArray(args, new Expression[0], node, source);
		return initException;
	}
}
