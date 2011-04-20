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

import static lombok.eclipse.handlers.Eclipse.typeNodeOf;
import static lombok.eclipse.handlers.EclipseHandlerUtil.injectMethod;
import static lombok.eclipse.handlers.ast.Arrays.buildArray;

import static org.eclipse.jdt.core.dom.Modifier.*;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.core.util.Cast;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

@RequiredArgsConstructor
abstract class AbstractMethodDefBuilder<SELF_TYPE extends AbstractMethodDefBuilder<SELF_TYPE, NODE_TYPE>, NODE_TYPE extends AbstractMethodDeclaration> implements ASTNodeBuilder<NODE_TYPE> {
	protected final List<ExpressionBuilder<? extends Annotation>> annotations = new ArrayList<ExpressionBuilder<? extends Annotation>>();
	protected final List<StatementBuilder<? extends TypeParameter>> typeParameters = new ArrayList<StatementBuilder<? extends TypeParameter>>();
	protected final List<StatementBuilder<? extends Argument>> arguments = new ArrayList<StatementBuilder<? extends Argument>>();
	protected final List<ExpressionBuilder<? extends TypeReference>> thrownExceptions = new ArrayList<ExpressionBuilder<? extends TypeReference>>();
	protected final List<StatementBuilder<? extends Statement>> statements = new ArrayList<StatementBuilder<? extends Statement>>();
	protected final String name;
	protected int modifiers;
	protected int bits;
	
	public NODE_TYPE injectInto(final EclipseNode node, final ASTNode source) {
		final NODE_TYPE method = build(node, source);
		injectMethod(typeNodeOf(node), build(node, source));
		return method;
	}
	
	protected Annotation[] buildAnnotations(final EclipseNode node, final ASTNode source) {
		return buildArray(annotations, new Annotation[0], node, source);
	}
	
	protected TypeParameter[] buildTypeParameters(final EclipseNode node, final ASTNode source) {
		return buildArray(typeParameters, new TypeParameter[0], node, source);
	}

	protected Argument[] buildArguments(final EclipseNode node, final ASTNode source) {
		return buildArray(arguments, new Argument[0], node, source);
	}
	
	protected TypeReference[] buildThrownExceptions(final EclipseNode node, final ASTNode source) {
		return buildArray(thrownExceptions, new TypeReference[0], node, source);
	}
	
	protected Statement[] buildStatements(final EclipseNode node, final ASTNode source) {
		return buildArray(statements, new Statement[0], node, source);
	}

	protected final SELF_TYPE self() {
		return Cast.<SELF_TYPE>uncheckedCast(this);
	}

	public SELF_TYPE makePrivate() {
		return withModifiers(PRIVATE);
	}
	
	public SELF_TYPE makeProtected() {
		return withModifiers(PROTECTED);
	}
	
	public SELF_TYPE makePublic() {
		return withModifiers(PUBLIC);
	}
	
	public SELF_TYPE makeStatic() {
		return withModifiers(STATIC);
	}
	
	public SELF_TYPE withAnnotation(final ExpressionBuilder<? extends Annotation> annotation) {
		annotations.add(annotation);
		return self();
	}
	
	public SELF_TYPE withAnnotations(final Annotation... annotations) {
		if (annotations != null) for (Annotation annotation : annotations) {
			this.annotations.add(new ExpressionWrapper<Annotation>(annotation));
		}
		return self();
	}

	public SELF_TYPE withArgument(final StatementBuilder<? extends Argument> argument) {
		this.arguments.add(argument);
		return self();
	}
	
	public SELF_TYPE withArguments(final List<StatementBuilder<? extends Argument>> arguments) {
		this.arguments.addAll(arguments);
		return self();
	}
	
	public SELF_TYPE withArguments(final Argument... arguments) {
		if (arguments != null) for (Argument argument : arguments) {
			this.arguments.add(new StatementWrapper<Argument>(argument));
		}
		return self();
	}
	
	public SELF_TYPE withBits(final int bits) {
		this.bits |= bits;
		return self();
	}
	
	public SELF_TYPE withModifiers(final int modifiers) {
		this.modifiers = modifiers;
		return self();
	}

	public SELF_TYPE withStatement(final StatementBuilder<? extends Statement> statement) {
		this.statements.add(statement);
		return self();
	}
	
	public SELF_TYPE withStatements(final Statement... statements) {
		if (statements != null) for (Statement statement : statements) {
			this.statements.add(new StatementWrapper<Statement>(statement));
		}
		return self();
	}
	
	public SELF_TYPE withThrownException(final ExpressionBuilder<? extends TypeReference> thrownException) {
		this.thrownExceptions.add(thrownException);
		return self();
	}
	
	public SELF_TYPE withThrownExceptions(final TypeReference... thrownExceptions) {
		if (thrownExceptions != null)  for (TypeReference thrownException : thrownExceptions) {
			this.thrownExceptions.add(new ExpressionWrapper<TypeReference>(thrownException));
		}
		return self();
	}
	
	public SELF_TYPE withTypeParameter(final StatementBuilder<? extends TypeParameter> typeParameter) {
		this.typeParameters.add(typeParameter);
		return self();
	}
}
