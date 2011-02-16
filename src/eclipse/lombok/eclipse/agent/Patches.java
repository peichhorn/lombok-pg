package lombok.eclipse.agent;

import static lombok.core.util.Arrays.isNotEmpty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.eclipse.EclipseAST;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.TransformEclipseAST;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Patches {
	public static final String CLASSSCOPE = "org.eclipse.jdt.internal.compiler.lookup.ClassScope";
	public static final String METHODVERIFIER = "org.eclipse.jdt.internal.compiler.lookup.MethodVerifier";
	public static final String METHODBINDING= "org.eclipse.jdt.internal.compiler.lookup.MethodBinding";
	public static final String METHODBINDINGS = "org.eclipse.jdt.internal.compiler.lookup.MethodBinding[]";
	public static final String SOURCETYPEBINDING = "org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding";
	public static final String TYPEDECLARATION = "org.eclipse.jdt.internal.compiler.ast.TypeDeclaration";
	public static final String METHODDECLARATION = "org.eclipse.jdt.internal.compiler.ast.MethodDeclaration";
	public static final String PROBLEMREPORTER = "org.eclipse.jdt.internal.compiler.problem.ProblemReporter";
	public static final String SCOPE = "org.eclipse.jdt.internal.compiler.lookup.Scope";
	public static final String REFERENCEBINDING = "org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding";
	public static final String CHARS = "char[]";
	public static final String TYPEBINDING = "org.eclipse.jdt.internal.compiler.lookup.TypeBinding";
	public static final String TYPEBINDINGS = "org.eclipse.jdt.internal.compiler.lookup.TypeBinding[]";
	public static final String INVOCATIONSITE = "org.eclipse.jdt.internal.compiler.lookup.InvocationSite";
	
	public static boolean hasAnnotations(TypeDeclaration decl) {
		return (decl != null) && isNotEmpty(decl.annotations);
	}

	public static Annotation getAnnotation(Class<?> expectedType, TypeDeclaration decl) {
		if (hasAnnotations(decl)) for (Annotation ann : decl.annotations) {
			if (matchesType(ann, expectedType, decl)) {
				return ann;
			}
		}
		return null;
	}

	public static boolean matchesType(Annotation ann, Class<?> expectedType, TypeDeclaration decl) {
		if (ann.type == null) return false;
		TypeBinding tb = ann.resolvedType;
		if ((tb == null) && (ann.type != null)) {
			tb = ann.type.resolveType(decl.initializerScope);
		}
		if (tb == null) return false;
		return new String(tb.readableName()).equals(expectedType.getName());
	}

	public static EclipseNode getTypeNode(TypeDeclaration decl) {
		CompilationUnitDeclaration cud = decl.scope.compilationUnitScope().referenceContext;
		EclipseAST astNode = TransformEclipseAST.getAST(cud, false);
		return astNode.get(decl);
	}
}
