/*
 * Copyright © 2011 Philipp Eichhorn
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

import static lombok.eclipse.agent.Patches.*;
import static lombok.patcher.scripts.ScriptBuilder.*;

import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import lombok.*;
import lombok.core.util.As;
import lombok.core.util.Each;
import lombok.patcher.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PatchVisibleForTesting {
	static void addPatches(final ScriptManager sm, final boolean ecj) {
		final String HOOK_NAME = PatchVisibleForTesting.class.getName();
		sm.addScript(replaceMethodCall()
			.target(new MethodTarget(SCOPE, "getMethod", METHODBINDING, TYPEBINDING, "char[]", TYPEBINDINGS, INVOCATIONSITE))
			.methodToReplace(new Hook(SCOPE, "findMethod", METHODBINDING, REFERENCEBINDING, "char[]", TYPEBINDINGS, INVOCATIONSITE))
			.replacementMethod(new Hook(HOOK_NAME, "onFindMethod", METHODBINDING, SCOPE, REFERENCEBINDING, "char[]", TYPEBINDINGS, INVOCATIONSITE))
			.build());
		sm.addScript(replaceMethodCall()
			.target(new MethodTarget(SCOPE, "getMethod", METHODBINDING, TYPEBINDING, "char[]", TYPEBINDINGS, INVOCATIONSITE))
			.methodToReplace(new Hook(SCOPE, "findExactMethod", METHODBINDING, REFERENCEBINDING, "char[]", TYPEBINDINGS, INVOCATIONSITE))
			.replacementMethod(new Hook(HOOK_NAME, "onFindExactMethod", METHODBINDING, SCOPE, REFERENCEBINDING, "char[]", TYPEBINDINGS, INVOCATIONSITE))
			.build());
		sm.addScript(replaceMethodCall()
			.target(new MethodTarget(COMPILATIONUNITSCOPE, "findImport", BINDING, "char[][]", "int"))
			.target(new MethodTarget(COMPILATIONUNITSCOPE, "findSingleImport", BINDING, "char[][]", "int", "boolean"))
			.target(new MethodTarget(SCOPE, "getTypeOrPackage", BINDING, "char[]", "int", "boolean"))
			.methodToReplace(new Hook(SCOPE, "findType", REFERENCEBINDING, "char[]", PACKAGEBINDING, PACKAGEBINDING))
			.replacementMethod(new Hook(HOOK_NAME, "onFindType", REFERENCEBINDING, SCOPE, "char[]", PACKAGEBINDING, PACKAGEBINDING))
			.build());
	}

	public static MethodBinding onFindMethod(final Scope scope, final ReferenceBinding receiverType, final char[] selector, final TypeBinding[] argumentTypes,
			final InvocationSite invocationSite) {
		return handleVisibleForTestingOnMethod(scope, scope.findMethod(receiverType, selector, argumentTypes, invocationSite));
	}

	public static MethodBinding onFindExactMethod(final Scope scope, final ReferenceBinding receiverType, final char[] selector, final TypeBinding[] argumentTypes,
			final InvocationSite invocationSite) {
		return handleVisibleForTestingOnMethod(scope, scope.findExactMethod(receiverType, selector, argumentTypes, invocationSite));
	}

	public static ReferenceBinding onFindType(final Scope scope, final char[] typeName, final PackageBinding declarationPackage, final PackageBinding invocationPackage) {
		return handleVisibleForTestingOnType(scope, scope.findType(typeName, declarationPackage, invocationPackage));
	}

	private static MethodBinding handleVisibleForTestingOnMethod(final Scope scope, final MethodBinding methodBinding) {
		if ((methodBinding == null) || (methodBinding.declaringClass == null)) return methodBinding;
		for (AnnotationBinding annotation : Each.elementIn(methodBinding.getAnnotations())) {
			if (!As.string(annotation.getAnnotationType()).contains("VisibleForTesting")) continue;
			ClassScope classScope = scope.outerMostClassScope();
			if (classScope == null) continue;
			TypeDeclaration decl = classScope.referenceContext;
			if ((methodBinding.declaringClass == decl.binding) || As.string(decl.name).contains("Test")) continue;
			return new ProblemMethodBinding(methodBinding, methodBinding.selector, methodBinding.parameters, ProblemReasons.NotVisible);
		}
		return methodBinding;
	}

	private static ReferenceBinding handleVisibleForTestingOnType(final Scope scope, final ReferenceBinding typeBinding) {
		if (typeBinding == null) return typeBinding;
		for (AnnotationBinding annotation : Each.elementIn(typeBinding.getAnnotations())) {
			if (!As.string(annotation.getAnnotationType()).contains("VisibleForTesting")) continue;
			ClassScope classScope = scope.outerMostClassScope();
			if (classScope == null) continue;
			TypeDeclaration decl = classScope.referenceContext;
			if (As.string(decl.name).contains("Test")) continue;
			return new ProblemReferenceBinding(typeBinding.compoundName, typeBinding, ProblemReasons.NotVisible);
		}
		return typeBinding;
	}
}
