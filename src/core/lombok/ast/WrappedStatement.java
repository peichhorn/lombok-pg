package lombok.ast;

import lombok.Getter;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class WrappedStatement extends Statement {
	private final Object wrappedObject;
	
	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitWrappedStatement(this, p);
	}
}
