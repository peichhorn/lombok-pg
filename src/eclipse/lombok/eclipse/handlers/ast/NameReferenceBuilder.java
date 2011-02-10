package lombok.eclipse.handlers.ast;

import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.Eclipse.poss;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class NameReferenceBuilder implements ExpressionBuilder<NameReference> {
	private final String name;
	
	@Override
	public NameReference build(final EclipseNode node, final ASTNode source) {
		NameReference nameReference;
		if (name.contains(".")) {
			char[][] nameTokens = fromQualifiedName(name);
			nameReference = new QualifiedNameReference(nameTokens, poss(source, nameTokens.length), 0, 0);
		} else {
			nameReference = new SingleNameReference(name.toCharArray(), 0);
		}
		setGeneratedByAndCopyPos(nameReference, source);
		return nameReference;
	}
}
