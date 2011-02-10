package lombok.eclipse.handlers.ast;

import static lombok.eclipse.Eclipse.copyType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class TypeReferenceWrapper implements ExpressionBuilder<TypeReference> {
	private final TypeReference expression;
	
	@Override
	public TypeReference build(EclipseNode node, ASTNode source) {
		return copyType(expression, source);
	}
}