package lombok.ast;

public class Argument extends AbstractVariableDecl<Argument> {

	public Argument(TypeRef type, String name) {
		super(type, name);
	}

	public TypeRef getType() {
		return type;
	}

	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitArgument(this, p);
	}
}
