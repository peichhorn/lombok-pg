package lombok.ast;

public final class ReturnDefault extends Expression {
	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitReturnDefault(this, p);
	}
}
