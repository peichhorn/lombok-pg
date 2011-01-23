package lombok.eclipse.agent;

import lombok.eclipse.EclipseAST;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.TransformEclipseAST;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public final class PatchUtils {
	private PatchUtils() {
	}
	
	public static boolean hasAnnotations(TypeDeclaration decl) {
		return (decl != null) && (decl.annotations != null);
	}
	
	public static boolean matchesType(Annotation ann, Class<?> expectedType, TypeDeclaration decl) {
		if (ann.type != null) return false;
		TypeBinding tb = getResolvedType(ann, decl);
		return new String(tb.readableName()).equals(expectedType.getName());
	}
	
	public static TypeBinding getResolvedType(Annotation ann, TypeDeclaration decl) {
		TypeBinding tb = ann.resolvedType;
		return (tb == null) ? ann.type.resolveType(decl.initializerScope) : tb;
	}
	
	public static EclipseNode getTypeNode(TypeDeclaration decl) {
		CompilationUnitDeclaration cud = decl.scope.compilationUnitScope().referenceContext;
		EclipseAST astNode = TransformEclipseAST.getAST(cud, true);
		return astNode.get(decl);
	}
}
