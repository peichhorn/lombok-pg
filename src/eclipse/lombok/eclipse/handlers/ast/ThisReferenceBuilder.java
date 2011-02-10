package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ThisReferenceBuilder implements ExpressionBuilder<ThisReference> {
	private ExpressionBuilder<? extends TypeReference> type;
	private boolean isImplicitThis;
	
	ThisReferenceBuilder(final ExpressionBuilder<? extends TypeReference> type) {
		this.type = type;
	}
	
	ThisReferenceBuilder(final boolean isImplicitThis) {
		this.isImplicitThis =isImplicitThis;
	}
	
	@Override
	public ThisReference build(final EclipseNode node, final ASTNode source) {
		ThisReference thisReference = new ThisReference(0, 0);
		if (type != null) {
			thisReference = new QualifiedThisReference(type.build(node, source), 0, 0);
		} else {
			thisReference = new ThisReference(0, 0);
			if (isImplicitThis) {
				thisReference.bits |= ASTNode.IsImplicitThis;
			}
		}
		setGeneratedByAndCopyPos(thisReference, source);
		return thisReference;
	}
}
