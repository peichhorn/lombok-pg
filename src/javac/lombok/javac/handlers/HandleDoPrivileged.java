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
package lombok.javac.handlers;

import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.javac.handlers.Javac.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.util.ArrayList;
import java.util.List;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;

import lombok.DoPrivileged;
import lombok.RequiredArgsConstructor;
import lombok.DoPrivileged.SanitizeWith;
import lombok.ast.TypeRef;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.Javac;
import lombok.javac.handlers.ast.JavacMethod;

import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.DoPrivileged} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleDoPrivileged extends JavacAnnotationHandler<DoPrivileged> {

	@Override public void handle(AnnotationValues<DoPrivileged> annotation, JCAnnotation source, JavacNode annotationNode) {
		final Class<? extends java.lang.annotation.Annotation> annotationType = DoPrivileged.class;
		deleteAnnotationIfNeccessary(annotationNode, annotationType);

		final JavacMethod method = JavacMethod.methodOf(annotationNode, source);

		if (method == null) {
			annotationNode.addError(canBeUsedOnMethodOnly(annotationType));
			return;
		}
		if (method.isAbstract() || method.isEmpty()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return;
		}

		replaceWithQualifiedThisReference(method, source);

		final TypeRef innerReturnType = method.boxedReturns();
		if (method.returns("void")) {
			replaceReturns(method);
			method.body(Block() //
				.withStatements(sanitizeParameter(method)) //
				.withStatement(Try(Block() //
					.withStatement(Call(Name("java.security.AccessController"), "doPrivileged").withArgument( //
						New(Type("java.security.PrivilegedExceptionAction").withTypeArgument(innerReturnType)).withTypeDeclaration(ClassDecl("").makeAnonymous().makeLocal() //
						.withMethod(MethodDecl(innerReturnType, "run").makePublic().withThrownExceptions(method.thrownExceptions()) //
							.withStatements(method.statements()) //
							.withStatement(Return(Null()))))))) //
				.Catch(Arg(Type("java.security.PrivilegedActionException"), "$ex"), Block() //
					.withStatement(LocalDecl(Type("java.lang.Throwable"), "$cause").makeFinal().withInitialization(Call(Name("$ex"), "getCause"))) //
					.withStatements(rethrowStatements(method)) //
					.withStatement(Throw(New(Type("java.lang.RuntimeException")).withArgument(Name("$cause")))))));
		} else {
			method.body(Block() //
				.withStatements(sanitizeParameter(method)) //
				.withStatement(Try(Block() //
					.withStatement(Return(Call(Name("java.security.AccessController"), "doPrivileged").withArgument( //
						New(Type("java.security.PrivilegedExceptionAction").withTypeArgument(innerReturnType)).withTypeDeclaration(ClassDecl("").makeAnonymous().makeLocal() //
						.withMethod(MethodDecl(innerReturnType, "run").makePublic().withThrownExceptions(method.thrownExceptions()) //
							.withStatements(method.statements()))))))) //
				.Catch(Arg(Type("java.security.PrivilegedActionException"), "$ex"), Block() //
					.withStatement(LocalDecl(Type("java.lang.Throwable"), "$cause").makeFinal().withInitialization(Call(Name("$ex"), "getCause"))) //
					.withStatements(rethrowStatements(method)) //
					.withStatement(Throw(New(Type("java.lang.RuntimeException")).withArgument(Name("$cause")))))));
		}

		method.rebuild();
	}

	private List<lombok.ast.Statement> sanitizeParameter(final JavacMethod method) {
		final List<lombok.ast.Statement> sanitizeStatements = new ArrayList<lombok.ast.Statement>();
		for (JCVariableDecl param : method.get().params) {
			final JCAnnotation ann = getAnnotation(SanitizeWith.class, param);
			if (ann != null) {
				final JavacNode annotationNode = method.node().getNodeFor(ann);
				String sanatizeMethodName = Javac.createAnnotation(SanitizeWith.class, annotationNode).getInstance().value();
				final String argumentName = param.name.toString();
				final String newArgumentName = "$" + argumentName;
				sanitizeStatements.add(LocalDecl(Type(param.vartype), argumentName).withInitialization(Call(sanatizeMethodName).withArgument(Name(newArgumentName))));
				param.name = method.node().toName(newArgumentName);
				param.mods.flags |= Flags.FINAL;
				param.mods.annotations = remove(param.mods.annotations, ann);
			}
		}
		return sanitizeStatements;
	}

	private List<lombok.ast.Statement> rethrowStatements(final JavacMethod method) {
		final List<lombok.ast.Statement> rethrowStatements = new ArrayList<lombok.ast.Statement>();
		for (lombok.ast.TypeRef thrownException : method.thrownExceptions()) {
			rethrowStatements.add(If(InstanceOf(Name("$cause"), thrownException)) //
				.Then(Throw(Cast(thrownException, Name("$cause")))));
		}
		return rethrowStatements;
	}

	private void replaceReturns(final JavacMethod method) {
		final IReplacementProvider<JCStatement> replacement = new ReturnNullReplacementProvider(method);
		new ReturnStatementReplaceVisitor(replacement).visit(method.get());
	}

	private void replaceWithQualifiedThisReference(final JavacMethod method, final JCTree source) {
		final IReplacementProvider<JCExpression> replacement = new QualifiedThisReplacementProvider(method.surroundingType().name(), method.node(), source);
		new ThisReferenceReplaceVisitor(replacement).visit(method.get());
	}

	@RequiredArgsConstructor
	private static class ReturnNullReplacementProvider implements IReplacementProvider<JCStatement> {
		private final JavacMethod method;

		@Override public JCStatement getReplacement() {
			return method.build(Return(Null()));
		}
	}
}
