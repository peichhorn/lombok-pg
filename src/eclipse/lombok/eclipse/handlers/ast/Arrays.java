package lombok.eclipse.handlers.ast;

import static lombok.core.util.Arrays.resize;

import java.util.List;

import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;

public final class Arrays {
	public static <ELEMENT_TYPE extends ASTNode> ELEMENT_TYPE[] buildArray(final List<? extends ASTNodeBuilder<? extends ELEMENT_TYPE>> list, final ELEMENT_TYPE[] array, final EclipseNode node, final ASTNode source) {
		if ((list != null) && !list.isEmpty()) {
			final int size = list.size();
			ELEMENT_TYPE[] newArray = resize(array, size);
			for (int i = 0; i < size; i++) {
				newArray[i] = list.get(i).build(node, source);
			}
			return newArray;
		}
		return null;
	}
}
