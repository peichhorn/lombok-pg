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

import static lombok.eclipse.agent.Patches.SCOPE;
import static lombok.eclipse.agent.Patches.METHODBINDING;
import static lombok.eclipse.agent.Patches.REFERENCEBINDING;
import static lombok.eclipse.agent.Patches.CHARS;
import static lombok.eclipse.agent.Patches.TYPEBINDING;
import static lombok.eclipse.agent.Patches.TYPEBINDINGS;
import static lombok.eclipse.agent.Patches.INVOCATIONSITE;

import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.patcher.Hook;
import lombok.patcher.MethodTarget;
import lombok.patcher.ScriptManager;
import lombok.patcher.scripts.ScriptBuilder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PatchVisibleForTesting {
	static void addPatches(ScriptManager sm, boolean ecj) {
		sm.addScript(ScriptBuilder.replaceMethodCall()
				.target(new MethodTarget(SCOPE, "getMethod", METHODBINDING, TYPEBINDING, CHARS, TYPEBINDINGS, INVOCATIONSITE))
				.methodToReplace(new Hook(SCOPE, "findMethod", METHODBINDING, REFERENCEBINDING, CHARS, TYPEBINDINGS, INVOCATIONSITE))
				.replacementMethod(new Hook("lombok.eclipse.agent.PatchVisibleForTesting", "onFindMethod", METHODBINDING, SCOPE, REFERENCEBINDING, CHARS, TYPEBINDINGS, INVOCATIONSITE))
				.build());
		sm.addScript(ScriptBuilder.replaceMethodCall()
				.target(new MethodTarget(SCOPE, "getMethod", METHODBINDING, TYPEBINDING, CHARS, TYPEBINDINGS, INVOCATIONSITE))
				.methodToReplace(new Hook(SCOPE, "findExactMethod", METHODBINDING, REFERENCEBINDING, CHARS, TYPEBINDINGS, INVOCATIONSITE))
				.replacementMethod(new Hook("lombok.eclipse.agent.PatchVisibleForTesting", "onFindExactMethod", METHODBINDING, SCOPE, REFERENCEBINDING, CHARS, TYPEBINDINGS, INVOCATIONSITE))
				.build());
	}

	public static MethodBinding onFindMethod(Scope scope, ReferenceBinding receiverType, char[] selector, TypeBinding[] argumentTypes, InvocationSite invocationSite) {
		return handleVisibleForTesting(scope, scope.findMethod(receiverType, selector, argumentTypes, invocationSite));
	}

	public static MethodBinding onFindExactMethod(Scope scope, ReferenceBinding receiverType, char[] selector, TypeBinding[] argumentTypes, InvocationSite invocationSite) {
		return handleVisibleForTesting(scope, scope.findExactMethod(receiverType, selector, argumentTypes, invocationSite));
	}

	private static MethodBinding handleVisibleForTesting(Scope scope, MethodBinding methodBinding) {
		if (methodBinding == null) {
			return null;
		}
		final AnnotationBinding[] annotations = methodBinding.getAnnotations();
		if (annotations != null) for (AnnotationBinding annotation : annotations) {
			if ("@VisibleForTesting".equals(annotation.toString())) {
				ClassScope classScope = scope.classScope();
				if (classScope != null) {
					TypeDeclaration decl = classScope.referenceContext;
					if ((methodBinding.declaringClass != decl.binding) && !new String(decl.name).contains("Test")) {
						return new ProblemMethodBinding(methodBinding, methodBinding.selector, methodBinding.parameters, ProblemReasons.NotVisible);
					}
				}
			}
		}
		return methodBinding;
	}
}
