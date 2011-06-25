package lombok.ast;

import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;

import lombok.Getter;

@Getter
public class WrappedMethodDecl extends MethodDecl {
	private final Object wrappedObject;

	public WrappedMethodDecl(final Object wrappedObject) {
		super(null, methodName(wrappedObject));
		this.wrappedObject = wrappedObject;
	}
	
	private static String methodName(final Object object) {
		if (object instanceof MethodBinding) {
			return new String(((MethodBinding)object).selector);
		}
		return null;
	}

	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitWrappedMethodDecl(this, p);
	}
}
