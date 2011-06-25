package lombok.ast;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public final class Switch extends Statement {
	private final List<Case> cases = new ArrayList<Case>();
	private final Expression expression;

	public Switch(final Expression expression) {
		this.expression = child(expression);
	}

	public Switch withCase(final Case caze) {
		cases.add(child(caze));
		return this;
	}
	
	public Switch withCases(final List<Case> cases) {
		for (Case caze : cases) withCase(caze);
		return this;
	}
	
	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitSwitch(this, p);
	}
}
