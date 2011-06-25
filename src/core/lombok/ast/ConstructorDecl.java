package lombok.ast;

public class ConstructorDecl extends AbstractMethodDecl<ConstructorDecl> {
	private boolean implicitSuper;

	public ConstructorDecl(String name) {
		super(name);
	}
	
	public ConstructorDecl withImplicitSuper() {
		implicitSuper = true;
		return this;
	}
	
	public boolean implicitSuper() {
		return implicitSuper;
	}
	
	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitConstructorDecl(this, p);
	}
}
