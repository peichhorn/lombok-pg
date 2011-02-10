package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.ast.Arrays.buildArray;

import java.util.ArrayList;
import java.util.List;

import lombok.core.util.Cast;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

abstract class GenericTypeArgumentBuilder<SELF_TYPE extends GenericTypeArgumentBuilder<SELF_TYPE, NODE_TYPE>, NODE_TYPE extends Expression> implements ExpressionBuilder<NODE_TYPE> {
	protected final List<ExpressionBuilder<? extends TypeReference>> typeArgs = new ArrayList<ExpressionBuilder<? extends TypeReference>>();
	
	protected TypeReference[] buildTypeArguments(final EclipseNode node, final ASTNode source) {
		return buildArray(typeArgs, new TypeReference[0], node, source);
	}
	
	protected final SELF_TYPE self() {
		return Cast.<SELF_TYPE>uncheckedCast(this);
	}
	
	public SELF_TYPE withTypeArgument(final ExpressionBuilder<? extends TypeReference> typeArg) {
		typeArgs.add(typeArg);
		return self();
	}
	
	public SELF_TYPE withTypeArguments(List<ExpressionBuilder<? extends TypeReference>> typeArgs) {
		this.typeArgs.addAll(typeArgs);
		return self();
	}
}
