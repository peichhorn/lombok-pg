package lombok.eclipse.handlers.replace;

import static lombok.ast.AST.*;

import lombok.core.util.As;
import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.internal.compiler.ast.*;

public class VariableNameReplaceVisitor extends ExpressionReplaceVisitor {
	private final String oldName;
	
	public VariableNameReplaceVisitor(final EclipseMethod method, final String oldName, final String newName) {
		super(method, Name(newName));
		this.oldName = oldName;
	}

	@Override
	protected boolean needsReplacing(final Expression node) {
		return (node instanceof SingleNameReference) && oldName.equals(As.string(((SingleNameReference) node).token));
	}
}
