package lombok.ast;

import lombok.*;

@RequiredArgsConstructor
@Getter
public class ArrayRef extends Expression<ArrayRef> {
	private final Expression<?> indexed;
	private final Expression<?> index;

	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(final ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, final PARAMETER_TYPE p) {
		return v.visitArrayRef(this, p);
	}
}
