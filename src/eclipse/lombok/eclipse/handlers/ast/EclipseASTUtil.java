package lombok.eclipse.handlers.ast;

import static lombok.ast.AST.*;
import static lombok.core.util.Names.*;

import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

import lombok.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EclipseASTUtil {

	public static lombok.ast.TypeRef boxedType(final TypeReference type) {
		if (type == null) return null;
		lombok.ast.TypeRef boxedType = Type(type);
		if (type instanceof SingleTypeReference) {
			final String name = new String(type.getLastToken());
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
