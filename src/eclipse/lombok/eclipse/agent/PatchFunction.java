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
import static lombok.eclipse.handlers.Eclipse.getAnnotation;
import static lombok.patcher.scripts.ScriptBuilder.*;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;

import lombok.*;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.HandleFunction;
import lombok.eclipse.handlers.ast.EclipseMethod;
import lombok.eclipse.handlers.ast.EclipseType;
import lombok.patcher.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PatchFunction {
	static void addPatches(final ScriptManager sm, final boolean ecj) {
		final String HOOK_NAME = PatchFunction.class.getName();
		sm.addScript(exitEarly()
				.target(new MethodTarget(CLASSSCOPE, "buildFieldsAndMethods", "void"))
				.request(StackRequest.THIS)
				.decisionMethod(new Hook(HOOK_NAME, "onClassScope_buildFieldsAndMethods", "boolean", CLASSSCOPE))
				.build());
	}

	public static boolean onClassScope_buildFieldsAndMethods(final ClassScope scope) {
		TypeDeclaration decl = scope.referenceContext;
		final EclipseNode typeNode = getTypeNode(decl);
		if (typeNode != null) {
			final EclipseType type = EclipseType.typeOf(typeNode, decl);
			for (EclipseMethod method : type.methods()) {
				final Annotation ann = getAnnotation(Function.class, method.get().annotations);
				if (ann != null) {
					completeNode(typeNode);
					EclipseNode annotationNode = typeNode.getNodeFor(ann);
					new HandleFunction().handle(Eclipse.createAnnotation(Function.class, annotationNode), ann, annotationNode);
				}
			}
		}
		return false;
	}
}
