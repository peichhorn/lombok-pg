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
import static lombok.patcher.scripts.ScriptBuilder.replaceMethodCall;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;

import lombok.AccessLevel;
import lombok.AutoGenMethodStub;
import lombok.NoArgsConstructor;
import lombok.eclipse.handlers.HandleAutoGenMethodStub;
import lombok.patcher.*;

// TODO scan for lombok annotations that come after @AutoGenMethodStub and print a warning that @AutoGenMethodStub
// should be the last annotation to avoid major issues, once again.. curve ball
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PatchAutoGenMethodStub {

	static void addPatches(ScriptManager sm, boolean ecj) {
		sm.addScript(replaceMethodCall()
				.target(new MethodTarget(METHODVERIFIER, "checkAbstractMethod", "void", METHODBINDING))
				.methodToReplace(new Hook(TYPEDECLARATION, "addMissingAbstractMethodFor", METHODDECLARATION, METHODBINDING))
				.replacementMethod(new Hook(PatchAutoGenMethodStub.class.getName(), "addMissingAbstractMethodFor", METHODDECLARATION, TYPEDECLARATION, METHODBINDING))
				.build());

		sm.addScript(replaceMethodCall()
				.target(new MethodTarget(METHODVERIFIER, "checkAbstractMethod", "void", METHODBINDING))
				.methodToReplace(new Hook(PROBLEMREPORTER, "abstractMethodMustBeImplemented", "void", SOURCETYPEBINDING, METHODBINDING))
				.replacementMethod(new Hook(PatchAutoGenMethodStub.class.getName(), "abstractMethodMustBeImplemented", "void", PROBLEMREPORTER, SOURCETYPEBINDING, METHODBINDING))
				.build());

		sm.addScript(replaceMethodCall()
				.target(new MethodTarget(METHODVERIFIER, "checkInheritedMethods", "void", METHODBINDINGS, "int"))
				.methodToReplace(new Hook(TYPEDECLARATION, "addMissingAbstractMethodFor", METHODDECLARATION, METHODBINDING))
				.replacementMethod(new Hook(PatchAutoGenMethodStub.class.getName(), "addMissingAbstractMethodFor", METHODDECLARATION, TYPEDECLARATION, METHODBINDING))
				.build());

		sm.addScript(replaceMethodCall()
				.target(new MethodTarget(METHODVERIFIER, "checkInheritedMethods", "void", METHODBINDINGS, "int"))
				.methodToReplace(new Hook(PROBLEMREPORTER, "abstractMethodMustBeImplemented", "void", SOURCETYPEBINDING, METHODBINDING))
				.replacementMethod(new Hook(PatchAutoGenMethodStub.class.getName(), "abstractMethodMustBeImplemented", "void", PROBLEMREPORTER, SOURCETYPEBINDING, METHODBINDING))
				.build());
	}

	private static boolean issueWasFixed = false;

	public static MethodDeclaration addMissingAbstractMethodFor(TypeDeclaration decl, MethodBinding abstractMethod) {
		Annotation ann = getAnnotation(AutoGenMethodStub.class, decl);
		if (ann != null) {
			MethodDeclaration method = new HandleAutoGenMethodStub().handle(abstractMethod, ann, getTypeNode(decl));
			issueWasFixed = true;
			return method;
		}
		return decl.addMissingAbstractMethodFor(abstractMethod);
	}

	public static void abstractMethodMustBeImplemented(ProblemReporter problemReporter, SourceTypeBinding type, MethodBinding abstractMethod) {
		if (issueWasFixed) {
			issueWasFixed = false;
		} else {
			problemReporter.abstractMethodMustBeImplemented(type, abstractMethod);
		}
	}
}