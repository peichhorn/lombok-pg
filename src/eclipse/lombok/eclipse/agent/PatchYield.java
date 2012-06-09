/*
 * Copyright © 2012 Philipp Eichhorn
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
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;

import lombok.*;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.HandleYield;
import lombok.patcher.*;
import lombok.patcher.scripts.ScriptBuilder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PatchYield {

	static void addPatches(final ScriptManager sm, final boolean ecj) {
		final String HOOK_NAME = PatchYield.class.getName();
		sm.addScript(ScriptBuilder.exitEarly()
			.target(new MethodTarget(ABSTRACTMETHODDECLARATION, "resolveStatements", "void"))
			.request(StackRequest.THIS)
			.decisionMethod(new Hook(HOOK_NAME, "onAbstractMethodDeclaration_resolveStatements", "boolean", ABSTRACTMETHODDECLARATION))
			.build());
	}

	public static boolean onAbstractMethodDeclaration_resolveStatements(final AbstractMethodDeclaration decl) {
		if (decl.statements != null) {
			final EclipseNode methodNode = Patches.getMethodNode(decl);
			methodNode.traverse(new HandleYield());
		}
		return false;
	}
}
