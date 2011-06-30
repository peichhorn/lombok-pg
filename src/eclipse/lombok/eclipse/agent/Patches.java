/*
 * Copyright © 2010-2011 Philipp Eichhorn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.eclipse.agent;

import static lombok.eclipse.handlers.Eclipse.hasAnnotations;
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

	public static Annotation getAnnotation(Class<? extends java.lang.annotation.Annotation> expectedType, TypeDeclaration decl) {
		if (hasAnnotations(decl)) for (Annotation ann : decl.annotations) {
			if (matchesType(ann, expectedType, decl)) {
				return ann;
			}
		}
		return null;
	}

	private static boolean matchesType(Annotation ann, Class<?> expectedType, TypeDeclaration decl) {
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
		EclipseNode node = astNode.get(decl);
		if (node == null) {
			astNode = TransformEclipseAST.getAST(cud, true);
			node = astNode.get(decl);
		}
		return node;
	}
}
