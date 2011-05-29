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
package lombok.eclipse.handlers.ast;

import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import static lombok.eclipse.handlers.ast.Arrays.buildArray;

import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

public final class LocalDefBuilder extends AbstractVariableDefBuilder<LocalDefBuilder, LocalDeclaration> {
	
	LocalDefBuilder(ExpressionBuilder<? extends TypeReference> type, String name) {
		super(type, name);
	}

	@Override
	public LocalDeclaration build(final EclipseNode node, final ASTNode source) {
		final LocalDeclaration proto = new LocalDeclaration(name.toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(proto, source);
		proto.modifiers = modifiers;
		proto.annotations = buildArray(annotations, new Annotation[0], node, source);
		proto.bits |= bits | ECLIPSE_DO_NOT_TOUCH_FLAG;
		proto.type = type.build(node, source);
		if (initialization != null) {
			proto.initialization = initialization.build(node, source);
		}
		return proto;
	}
}
