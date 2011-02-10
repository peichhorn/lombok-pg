package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.ast.ASTBuilder.Char;
import static lombok.eclipse.handlers.ast.ASTBuilder.False;
import static lombok.eclipse.handlers.ast.ASTBuilder.Null;
import static lombok.eclipse.handlers.ast.ASTBuilder.Number;
import static lombok.eclipse.handlers.ast.ASTBuilder.Return;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DefaultReturnStatementBuilder implements StatementBuilder<ReturnStatement> {
	protected ExpressionBuilder<? extends TypeReference> returnType;
	
	public DefaultReturnStatementBuilder withReturnType(final ExpressionBuilder<? extends TypeReference> returnType) {
		this.returnType = returnType;
		return this;
	}
	
	@Override
	public ReturnStatement build(EclipseNode node, ASTNode source) {
		ReturnStatementBuilder builder = Return(Null());
		if (returnType != null) {
			TypeReference type = returnType.build(node, source);
			if (type instanceof SingleTypeReference) {
				final String name = new String(type.getLastToken());
				if ("int".equals(name)) {
					builder = Return(Number(Integer.valueOf(0)));
				} else if ("byte".equals(name)) {
					builder = Return(Number(Integer.valueOf(0)));
				} else if ("short".equals(name)) {
					builder = Return(Number(Integer.valueOf(0)));
				} else if ("char".equals(name)) {
					builder = Return(Char(""));
				} else if ("long".equals(name)) {
					builder = Return(Number(Long.valueOf(0)));
				} else if ("float".equals(name)) {
					builder = Return(Number(Float.valueOf(0)));
				} else if ("double".equals(name)) {
					builder = Return(Number(Double.valueOf(0)));
				} else if ("boolean".equals(name)) {
					builder = Return(False());
				}
			}
		}
		return builder.build(node, source);
	}
}
