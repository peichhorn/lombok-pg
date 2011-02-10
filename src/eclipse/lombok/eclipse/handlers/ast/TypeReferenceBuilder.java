package lombok.eclipse.handlers.ast;

import static lombok.core.util.Arrays.isNotEmpty;
import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.Eclipse.poss;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class TypeReferenceBuilder extends GenericTypeArgumentBuilder<TypeReferenceBuilder, TypeReference> {
	private final String typeName;
	private int dims;
	
	public TypeReferenceBuilder withDimensions(final int dims) {
		this.dims = dims;
		return this;
	}
	
	@Override
	public TypeReference build(final EclipseNode node, final ASTNode source) {
		TypeReference[] paramTypes = buildTypeArguments(node, source);
		TypeReference typeReference;
		if (typeName.equals("void")) {
			typeReference = new SingleTypeReference(TypeBinding.VOID.simpleName, 0);
		} else if (typeName.contains(".")) {
			char[][] typeNameTokens = fromQualifiedName(typeName);
			if (isNotEmpty(paramTypes)) {
				TypeReference[][] typeArguments = new TypeReference[typeNameTokens.length][];
				typeArguments[typeNameTokens.length - 1] = paramTypes;
				typeReference = new ParameterizedQualifiedTypeReference(typeNameTokens, typeArguments, 0, poss(source, typeNameTokens.length));
			} else {
				if (dims > 0) {
					typeReference = new ArrayQualifiedTypeReference(typeNameTokens, dims, poss(source, typeNameTokens.length));
				} else {
					typeReference = new QualifiedTypeReference(typeNameTokens, poss(source, typeNameTokens.length));
				}
			}
		} else {
			char[] typeNameToken = typeName.toCharArray();
			if (isNotEmpty(paramTypes)) {
				typeReference = new ParameterizedSingleTypeReference(typeNameToken, paramTypes, 0, 0);
			} else {
				if (dims > 0) {
					typeReference = new ArrayTypeReference(typeNameToken, dims, 0);
				} else {
					typeReference = new SingleTypeReference(typeNameToken, 0);
				}
			}
		}
		setGeneratedByAndCopyPos(typeReference, source);
		return typeReference;
	}
}
