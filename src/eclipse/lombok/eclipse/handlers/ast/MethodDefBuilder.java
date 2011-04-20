/*
 * Copyright Â© 2011 Philipp Eichhorn
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
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccAbstract;
import static org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers.AccSemicolonBody;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

public class MethodDefBuilder extends AbstractMethodDefBuilder<MethodDefBuilder, MethodDeclaration> {
	protected ExpressionBuilder<? extends TypeReference> returnType;
	protected boolean noBody;
	
	public MethodDefBuilder(final ExpressionBuilder<? extends TypeReference> returnType, final String name) {
		super(name);
		this.returnType = returnType;
	}
	
	public MethodDefBuilder withReturnType(final ExpressionBuilder<? extends TypeReference> returnType) {
		this.returnType = returnType;
		return self();
	}
	
	public MethodDefBuilder withNoBody() {
		noBody = true;
		return self();
	}

	@Override
	public MethodDeclaration build(final EclipseNode node, final ASTNode source) {
		MethodDeclaration proto = new MethodDeclaration(((CompilationUnitDeclaration) node.top().get()).compilationResult);
		setGeneratedByAndCopyPos(proto, source);
		for (StatementBuilder<? extends Statement> statement : statements) {
			if (statement instanceof DefaultReturnBuilder) {
				((DefaultReturnBuilder) statement).withReturnType(returnType);
			}
		}
		proto.modifiers = modifiers;
		proto.returnType = returnType.build(node, source);
		proto.annotations = buildAnnotations(node, source);
		proto.selector = name.toCharArray();
		proto.thrownExceptions = buildThrownExceptions(node, source);
		proto.typeParameters = buildTypeParameters(node, source);
		proto.bits |=  bits | ECLIPSE_DO_NOT_TOUCH_FLAG;
		proto.arguments = buildArguments(node, source);
		if (noBody || ((modifiers & AccAbstract) != 0)) {
			proto.modifiers |= AccSemicolonBody;
		} else {
			proto.statements = buildStatements(node, source);
		}
		return proto;
	}
}
