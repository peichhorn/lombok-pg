package lombok.ast;

import lombok.Getter;

@Getter
public class WrappedTypeRef extends TypeRef {
	private final Object wrappedObject;
	
	public WrappedTypeRef(final Object wrappedObject) {
		super((String) null);
		this.wrappedObject = wrappedObject;
	}
	
	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitWrappedTypeRef(this, p);
	}
}
