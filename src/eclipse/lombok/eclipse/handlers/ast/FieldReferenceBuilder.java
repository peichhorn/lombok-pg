package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class FieldReferenceBuilder implements ExpressionBuilder<FieldReference> {
	private ExpressionBuilder<? extends Expression> receiver = new ThisReferenceBuilder(true);
	private final String name;
	
	@Override
	public FieldReference build(final EclipseNode node, final ASTNode source) {
		FieldReference fieldRef = new FieldReference(name.toCharArray(), 0);
		fieldRef.receiver = receiver.build(node, source);
		setGeneratedByAndCopyPos(fieldRef, source);
		return fieldRef;
	}
}
