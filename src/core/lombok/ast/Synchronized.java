package lombok.ast;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class Synchronized extends Statement {
	private final Expression lock;
	private final List<Statement> statements = new ArrayList<Statement>();

	public Synchronized(final Expression lock) {
		this.lock = child(lock);
	}

	public Synchronized withStatement(final Statement statement) {
		statements.add(child(statement));
		return this;
	}

	public Synchronized withStatements(final List<Statement> statements) {
		for (Statement statement : statements) withStatement(statement);
		return this;
	}

	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitSynchronized(this, p);
	}
}
