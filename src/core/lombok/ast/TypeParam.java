package lombok.ast;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public final class TypeParam extends Statement {
	protected final List<TypeRef> bounds = new ArrayList<TypeRef>();
	protected final String name;
	protected TypeRef type;
	
	public TypeParam withBound(final TypeRef bound) {
		bounds.add(child(bound));
		return this;
	}

	public TypeParam withType(final TypeRef type) {
		this.type = child(type);
		return this;
	}
	
	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitTypeParam(this, p);
	}
}
