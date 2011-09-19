package lombok.javac.handlers.ast;

import static lombok.ast.AST.*;
import static lombok.core.util.Names.*;

import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;

import lombok.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavacASTUtil {

	public static lombok.ast.TypeRef boxedType(final JCExpression type) {
		if (type == null) return null;
		lombok.ast.TypeRef boxedType = Type(type);
		if (type instanceof JCPrimitiveTypeTree) {
			final String name = type.toString();
			if ("int".equals(name)) {
				boxedType = Type(Integer.class);
			} else if ("char".equals(name)) {
				boxedType = Type(Character.class);
			} else if (isOneOf(name, "void", "boolean", "float", "double", "byte", "short", "long")) {
				boxedType = Type("java.lang." + capitalize(name));
			}
		}
		return boxedType;
	}
}
