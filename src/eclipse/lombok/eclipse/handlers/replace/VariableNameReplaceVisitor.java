package lombok.eclipse.handlers.replace;

import static lombok.ast.AST.*;
import static lombok.core.util.Names.string;

import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.internal.compiler.ast.*;

public class VariableNameReplaceVisitor extends ExpressionReplaceVisitor {
	private final String oldName;
	
	public VariableNameReplaceVisitor(EclipseMethod method, String oldName, String newName) {
		super(method, Name(newName));
		this.oldName = oldName;
	}

	@Override
	protected boolean needsReplacing(Expression node) {
		return (node instanceof SingleNameReference) && oldName.equals(string(((SingleNameReference) node).token));
	}
}
