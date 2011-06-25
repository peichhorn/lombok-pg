package lombok.ast;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class Case extends Statement {
	private final List<Statement> statements = new ArrayList<Statement>();
	private Expression pattern;
	
	public Case(Expression pattern) {
		this.pattern = child(pattern);
	}
	
	public Case withStatement(final Statement statement) {
		statements.add(child(statement));
		return this;
	}
	
	public Case withStatements(final List<Statement> statements) {
		for (Statement statement : statements) withStatement(statement);
		return this;
	}

	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitCase(this, p);
	}
}
