package lombok.eclipse.handlers.ast;


import static lombok.eclipse.Eclipse.makeType;
import static org.eclipse.jdt.internal.compiler.ast.ASTNode.IsSuperType;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class TypeBindingTypeReferenceBuilder implements ExpressionBuilder<TypeReference> {
	private final TypeBinding typeBinding;
	protected int bits;
	
	public TypeBindingTypeReferenceBuilder makeSuperType() {
		return withBits(IsSuperType);
	}
	
	public TypeBindingTypeReferenceBuilder withBits(int bits) {
		this.bits |= bits;
		return this;
	}
	
	@Override
	public TypeReference build(final EclipseNode node, final ASTNode source) {
		TypeReference typeReference = makeType(typeBinding, source, false);
		return typeReference;
	}
}
