package lombok.ast;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Annotation extends Expression {
	private final Map<String, Expression> values = new HashMap<String, Expression>();
	private final TypeRef type;

	public Annotation withValue(final Expression value) {
		return withValue("value", value);
	}
	
	public Annotation withValue(final String name, final Expression value) {
		this.values.put(name, child(value));
		return this;
	}
	
	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitAnnotation(this, p);
	}
}
