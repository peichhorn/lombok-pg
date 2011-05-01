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
package lombok.eclipse.handlers;

import static lombok.core.util.ErrorMessages.canBeUsedOnConcreteMethodOnly;
import static lombok.core.util.ErrorMessages.canBeUsedOnMethodOnly;
import static lombok.core.util.Names.capitalize;
import static lombok.eclipse.handlers.Eclipse.typeNodeOf;
import static lombok.eclipse.handlers.ast.ASTBuilder.*;
import lombok.DoPrivileged;
import lombok.RequiredArgsConstructor;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.ExpressionBuilder;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.DoPrivileged} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleDoPrivileged implements EclipseAnnotationHandler<DoPrivileged> {
	@Override public boolean handle(AnnotationValues<DoPrivileged> annotation, Annotation ast, EclipseNode annotationNode) {
		final Class<? extends java.lang.annotation.Annotation> annotationType = DoPrivileged.class;
		
		final EclipseMethod method = EclipseMethod.methodOf(annotationNode);

		if (method == null) {
			annotationNode.addError(canBeUsedOnMethodOnly(annotationType));
			return true;
		}
		if (!method.wasCompletelyParsed()) {
			return false;
		}
		if (method.isAbstract() || method.isEmpty()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return true;
		}

		replaceWithQualifiedThisReference(method, ast);
		
		TypeReference returnType = method.returnType();
		ExpressionBuilder<? extends TypeReference> innerReturnType = boxedReturnType(returnType);
		
		if (method.returns("void")) {
			replaceReturns(method.get(), ast);
			method.body(ast, Block() //
				.withStatement(Call(Name("java.security.AccessController"), "doPrivileged").withArgument(
					New(Type("java.security.PrivilegedAction").withTypeArgument(innerReturnType), ClassDef("").makeAnonymous().makeLocal() //
					.withMethod(MethodDef(innerReturnType, "run").makePublic() //
						.withStatements(method.get().statements)
						.withStatement(Return(Null())))))));
		} else {
			method.body(ast, Block() //
				.withStatement(Return(Call(Name("java.security.AccessController"), "doPrivileged").withArgument(
					New(Type("java.security.PrivilegedAction").withTypeArgument(innerReturnType), ClassDef("").makeAnonymous().makeLocal() //
					.withMethod(MethodDef(innerReturnType, "run").makePublic() //
						.withStatements(method.get().statements)))))));
		}
		
		return true;
	}
	
	private ExpressionBuilder<? extends TypeReference> boxedReturnType(TypeReference type) {
		ExpressionBuilder<? extends TypeReference> objectReturnType = Type(type);
		if (type instanceof SingleTypeReference) {
			final String name = new String(type.getLastToken());
			if ("int".equals(name)) {
				objectReturnType = Type("java.lang.Integer");
			} else if ("char".equals(name)) {
				objectReturnType = Type("java.lang.Character");
			} else {
				objectReturnType = Type("java.lang." + capitalize(name));
			}
		}
		return objectReturnType;
	}
	
	private void replaceReturns(AbstractMethodDeclaration method, final ASTNode source) {
		final IReplacementProvider<Statement> replacement = new ReturnNullReplacementProvider(source);
		new ReturnStatementReplaceVisitor(replacement).visit(method);
	}

	private void replaceWithQualifiedThisReference(final EclipseMethod method, final ASTNode source) {
		final EclipseNode parent = typeNodeOf(method.node());
		final TypeDeclaration typeDec = (TypeDeclaration)parent.get();
		final IReplacementProvider<Expression> replacement = new QualifiedThisReplacementProvider(new String(typeDec.name), source);
		new ThisReferenceReplaceVisitor(replacement).visit(method.get());
	}
	

	@RequiredArgsConstructor
	private static class ReturnNullReplacementProvider implements IReplacementProvider<Statement> {
		private final ASTNode source;

		@Override public Statement getReplacement() {
			return Return(Null()).build(null, source);
		}
	}
}
