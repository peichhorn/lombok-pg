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
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;

import lombok.*;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.HandleListenerSupport;
import lombok.patcher.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PatchListenerSupport {
	static void addPatches(final ScriptManager sm, final boolean ecj) {
		sm.addScript(exitEarly()
			.target(new MethodTarget(CLASSSCOPE, "buildFieldsAndMethods", "void"))
			.request(StackRequest.THIS)
			.decisionMethod(new Hook(PatchListenerSupport.class.getName(), "onClassScope_buildFieldsAndMethods", "boolean", CLASSSCOPE))
			.build());
	}

	public static boolean onClassScope_buildFieldsAndMethods(final ClassScope scope) {
		TypeDeclaration decl = scope.referenceContext;
		Annotation ann = getAnnotation(ListenerSupport.class, decl);
		EclipseNode typeNode = getTypeNode(decl);
		if ((ann != null) && (typeNode != null)) {
			EclipseNode annotationNode = typeNode.getNodeFor(ann);
			new HandleListenerSupport().handle(Eclipse.createAnnotation(ListenerSupport.class, annotationNode), ann, annotationNode);
		}
		return false;
	}
}
