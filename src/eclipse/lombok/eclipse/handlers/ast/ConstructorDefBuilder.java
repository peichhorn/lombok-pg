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
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;

public class ConstructorDefBuilder extends AbstractMethodDefBuilder<ConstructorDefBuilder, ConstructorDeclaration> {
	private ExplicitConstructorCall constructorCall;
	
	public ConstructorDefBuilder(final String typeName) {
		super(typeName);
	}
	
	public ConstructorDefBuilder withImplicitSuper() {
		this.constructorCall = new ExplicitConstructorCall(ExplicitConstructorCall.ImplicitSuper);
		return this;
	}

	@Override
	public ConstructorDeclaration build(final EclipseNode node, final ASTNode source) {
		final ConstructorDeclaration proto = new ConstructorDeclaration(((CompilationUnitDeclaration) node.top().get()).compilationResult);
		setGeneratedByAndCopyPos(proto, source);
		proto.modifiers = modifiers;
		proto.annotations = buildAnnotations(node, source);
		proto.constructorCall = constructorCall;
		proto.selector = name.toCharArray();
		proto.thrownExceptions = buildThrownExceptions(node, source);
		proto.typeParameters = buildTypeParameters(node, source);
		proto.bits |=  bits | ECLIPSE_DO_NOT_TOUCH_FLAG;
		proto.arguments = buildArguments(node, source);
		if (!statements.isEmpty()) {
			proto.statements = buildStatements(node, source);
		}
		return proto;
	}
}
