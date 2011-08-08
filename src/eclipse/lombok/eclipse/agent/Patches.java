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

import lombok.*;
import lombok.eclipse.EclipseAST;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.TransformEclipseAST;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Patches {
	public static final String AST_PACKAGE = "org.eclipse.jdt.internal.compiler.ast";
	public static final String LOOKUP_PACKAGE = "org.eclipse.jdt.internal.compiler.lookup";
	public static final String PROBLEM_PACKAGE = "org.eclipse.jdt.internal.compiler.problem";
	public static final String TEXT_JAVA_PACKAGE = "org.eclipse.jdt.ui.text.java";
	public static final String BINDING = LOOKUP_PACKAGE + ".Binding";
	public static final String BINDINGS = BINDING + "[]";
	public static final String BLOCKSCOPE = LOOKUP_PACKAGE + ".BlockScope";
	public static final String CLASSSCOPE = LOOKUP_PACKAGE + ".ClassScope";
	public static final String COMPLETIONPROPOSALCOLLECTOR = TEXT_JAVA_PACKAGE + ".CompletionProposalCollector";
	public static final String IJAVACOMPLETIONPROPOSALS = TEXT_JAVA_PACKAGE + ".IJavaCompletionProposal[]";
	public static final String INVOCATIONSITE = LOOKUP_PACKAGE + ".InvocationSite";
	public static final String MESSAGESEND = AST_PACKAGE + ".MessageSend";
	public static final String METHODBINDING = LOOKUP_PACKAGE + ".MethodBinding";
	public static final String METHODBINDINGS = METHODBINDING + "[]";
	public static final String METHODDECLARATION = AST_PACKAGE + ".MethodDeclaration";
	public static final String METHODVERIFIER = LOOKUP_PACKAGE + ".MethodVerifier";
	public static final String PROBLEMREPORTER = PROBLEM_PACKAGE + ".ProblemReporter";
	public static final String REFERENCEBINDING = LOOKUP_PACKAGE + ".ReferenceBinding";
	public static final String SCOPE = LOOKUP_PACKAGE + ".Scope";
	public static final String SOURCETYPEBINDING = LOOKUP_PACKAGE + ".SourceTypeBinding";
	public static final String TYPEBINDING = LOOKUP_PACKAGE + ".TypeBinding";
	public static final String TYPEBINDINGS = TYPEBINDING + "[]";
	public static final String TYPEDECLARATION = AST_PACKAGE + ".TypeDeclaration";

	public static Annotation getAnnotation(final Class<? extends java.lang.annotation.Annotation> expectedType, final TypeDeclaration decl) {
		if (hasAnnotations(decl)) for (Annotation ann : decl.annotations) {
			if (matchesType(ann, expectedType, decl)) {
				return ann;
			}
		}
		return null;
	}

	private static boolean matchesType(final Annotation ann, final Class<?> expectedType, final TypeDeclaration decl) {
		if (ann.type == null) return false;
		TypeBinding tb = ann.resolvedType;
		if ((tb == null) && (ann.type != null)) {
			try {
				tb = ann.type.resolveType(decl.initializerScope);
			} catch (final Exception ignore) {
				// completion nodes may throw an exception here
			}
		}
		if (tb == null) return false;
		return new String(tb.readableName()).equals(expectedType.getName());
	}

	public static EclipseNode getTypeNode(final TypeDeclaration decl) {
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
