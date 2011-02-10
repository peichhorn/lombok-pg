package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class AnnotationBuilder implements ExpressionBuilder<Annotation> {
	private final ExpressionBuilder<? extends TypeReference> type;
	private ExpressionBuilder<? extends Expression> value;
	
	public AnnotationBuilder withValue(final ExpressionBuilder<? extends Expression> value) {
		this.value = value;
		return this;
	}
	
	@Override
	public Annotation build(final EclipseNode node, final ASTNode source) {
		Annotation ann;
		if (value == null) {
			ann = new MarkerAnnotation(type.build(node, source), 0);
		} else {
			ann = new SingleMemberAnnotation(type.build(node, source), 0);
			((SingleMemberAnnotation)ann).memberValue = value.build(node, source);
		}
		setGeneratedByAndCopyPos(ann, source);
		return ann;
	} 
}
