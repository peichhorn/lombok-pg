package lombok.ast;

import lombok.Getter;

public class MethodDecl extends AbstractMethodDecl<MethodDecl> {
	@Getter
	private TypeRef returnType;
	@Getter
	private boolean implementing;
	private boolean noBody;
	
	public MethodDecl(TypeRef returnType, String name) {
		super(name);
		this.returnType = child(returnType);
	}

	public MethodDecl withReturnType(TypeRef returnType) {
		this.returnType = child(returnType);
		return self();
	}

	public MethodDecl withNoBody() {
		noBody = true;
		return self();
	}

	public boolean noBody() {
		return noBody;
	}

	public MethodDecl implementing() {
		implementing = true;
		return self();
	}

	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitMethodDecl(this, p);
	}
}
