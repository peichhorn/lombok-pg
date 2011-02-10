package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import static lombok.eclipse.handlers.ast.Arrays.buildArray;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class TypeParameterBuilder implements StatementBuilder<TypeParameter>{
	protected final List<ExpressionBuilder<? extends TypeReference>> bounds = new ArrayList<ExpressionBuilder<? extends TypeReference>>();
	protected final String name;
	protected ExpressionBuilder<? extends TypeReference> type;
	
	public TypeParameterBuilder withBound(final ExpressionBuilder<? extends TypeReference> bound) {
		bounds.add(bound);
		return this;
	}
	
	public TypeParameterBuilder withType(final ExpressionBuilder<? extends TypeReference> type) {
		this.type = type;
		return this;
	}
	
	@Override
	public TypeParameter build(EclipseNode node, ASTNode source) {
		TypeParameter typeParameter = new TypeParameter();
		typeParameter.type = type.build(node, source);
		typeParameter.name = name.toCharArray();
		typeParameter.bounds = buildArray(bounds, new TypeReference[0], node, source);
		setGeneratedByAndCopyPos(typeParameter, source);
		return typeParameter;
	}
}
