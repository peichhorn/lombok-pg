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

import static lombok.eclipse.agent.Patches.*;
import static lombok.patcher.scripts.ScriptBuilder.*;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;

import lombok.*;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.HandleAutoGenMethodStub;
import lombok.patcher.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PatchAutoGenMethodStub {

	static void addPatches(final ScriptManager sm, final boolean ecj) {
		final String HOOK_NAME = PatchAutoGenMethodStub.class.getName();
		sm.addScript(replaceMethodCall()
			.target(new MethodTarget(METHODVERIFIER, "checkAbstractMethod", "void", METHODBINDING))
			.target(new MethodTarget(METHODVERIFIER, "checkInheritedMethods", "void", METHODBINDINGS, "int"))
			.methodToReplace(new Hook(TYPEDECLARATION, "addMissingAbstractMethodFor", METHODDECLARATION, METHODBINDING))
			.replacementMethod(new Hook(HOOK_NAME, "addMissingAbstractMethodFor", METHODDECLARATION, TYPEDECLARATION, METHODBINDING))
			.build());

		sm.addScript(replaceMethodCall()
			.target(new MethodTarget(METHODVERIFIER, "checkAbstractMethod", "void", METHODBINDING))
			.target(new MethodTarget(METHODVERIFIER, "checkInheritedMethods", "void", METHODBINDINGS, "int"))
			.methodToReplace(new Hook(PROBLEMREPORTER, "abstractMethodMustBeImplemented", "void", SOURCETYPEBINDING, METHODBINDING))
			.replacementMethod(new Hook(HOOK_NAME, "abstractMethodMustBeImplemented", "void", PROBLEMREPORTER, SOURCETYPEBINDING, METHODBINDING))
			.build());
	}

	private static final ThreadLocal<Boolean> ISSUE_WAS_FIXED = new ThreadLocal<Boolean>() {
		@Override protected Boolean initialValue() {
			return Boolean.FALSE;
		}
	};

	public static MethodDeclaration addMissingAbstractMethodFor(final TypeDeclaration decl, final MethodBinding abstractMethod) {
		Annotation ann = getAnnotation(AutoGenMethodStub.class, decl);
		EclipseNode typeNode = getTypeNode(decl);
		if ((ann != null) && (typeNode != null)) {
			EclipseNode annotationNode = typeNode.getNodeFor(ann);
			MethodDeclaration method = new HandleAutoGenMethodStub().handle(abstractMethod, Eclipse.createAnnotation(AutoGenMethodStub.class, annotationNode), ann, annotationNode);
			ISSUE_WAS_FIXED.set(true);
			return method;
		}
		return decl.addMissingAbstractMethodFor(abstractMethod);
	}

	public static void abstractMethodMustBeImplemented(final ProblemReporter problemReporter, final SourceTypeBinding type, final MethodBinding abstractMethod) {
		if (ISSUE_WAS_FIXED.get()) {
			ISSUE_WAS_FIXED.set(false);
		} else {
			problemReporter.abstractMethodMustBeImplemented(type, abstractMethod);
		}
	}
}