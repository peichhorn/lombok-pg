package lombok.ast;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class TypeRef extends Expression {
	private final List<TypeRef> typeArgs = new ArrayList<TypeRef>();
	private final String typeName;
	private boolean superType;
	private int dims;
	
	public TypeRef(Class<?> clazz) {
		this(clazz.getName());
	}

	public TypeRef makeSuperType() {
		superType = true;
		return this;
	}

	public TypeRef withDimensions(final int dims) {
		this.dims = dims;
		return this;
	}

	public TypeRef withTypeArgument(final TypeRef typeArg) {
		typeArgs.add(child(typeArg));
		return this;
	}

	public TypeRef withTypeArguments(final List<TypeRef> typeArgs) {
		for (TypeRef typeArg : typeArgs) withTypeArgument(typeArg);
		return this;
	}

	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitTypeRef(this, p);
	}
}
