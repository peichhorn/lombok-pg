package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import static lombok.eclipse.handlers.Eclipse.typeNodeOf;
import static lombok.eclipse.handlers.EclipseHandlerUtil.injectField;
import static lombok.eclipse.handlers.ast.Arrays.buildArray;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;

@RequiredArgsConstructor
public class EnumConstantBuilder implements StatementBuilder<FieldDeclaration> {
	protected final String name;
	private final List<ExpressionBuilder<? extends Expression>> args = new ArrayList<ExpressionBuilder<? extends Expression>>();
	
	public EnumConstantBuilder withArgument(final ExpressionBuilder<? extends Expression> arg) {
		args.add(arg);
		return this;
	}
	
	public void injectInto(final EclipseNode node, final ASTNode source) {
		injectField(typeNodeOf(node), build(node, source));
	}
	
	@Override
	public FieldDeclaration build(EclipseNode node, ASTNode source) {
		AllocationExpression initialization = new AllocationExpression();
		setGeneratedByAndCopyPos(initialization, source);
		initialization.arguments = buildArray(args, new Expression[0], node, source);
		initialization.enumConstant = new FieldDeclaration(name.toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(initialization.enumConstant, source);
		initialization.enumConstant.initialization = initialization;
		return initialization.enumConstant;
	}
}
