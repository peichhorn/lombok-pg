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
package lombok.javac.handlers;

import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.util.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacMethod;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.SwingInvokeLater} and {@code lombok.SwingInvokeAndWait} annotation for javac.
 */
public class HandleSwingInvoke {

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleSwingInvokeLater extends JavacAnnotationHandler<SwingInvokeLater> {
		@Override public void handle(AnnotationValues<SwingInvokeLater> annotation, JCAnnotation source, JavacNode annotationNode) {
			new HandleSwingInvoke().generateSwingInvoke("invokeLater", SwingInvokeLater.class, source, annotationNode);
		}
	}

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleSwingInvokeAndWait extends JavacAnnotationHandler<SwingInvokeAndWait> {
		@Override public void handle(AnnotationValues<SwingInvokeAndWait> annotation, JCAnnotation source, JavacNode annotationNode) {
			new HandleSwingInvoke().generateSwingInvoke("invokeAndWait", SwingInvokeAndWait.class, source, annotationNode);
		}
	}

	public void generateSwingInvoke(String methodName, Class<? extends java.lang.annotation.Annotation> annotationType, JCTree source, JavacNode annotationNode) {
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

		String field = "$" + camelCase(method.name(), "runnable");

		Call elseStatementRun = Call(Name("java.awt.EventQueue"), methodName).withArgument(Name(field));

		Statement elseStatement;
		if ("invokeAndWait".equals(methodName)) {
			elseStatement =  Block().withStatement(generateTryCatchBlock(elseStatementRun, method));
		} else {
			elseStatement = Block().withStatement(elseStatementRun);
		}

		method.body(Block() //
			.withStatement(LocalDecl(Type("java.lang.Runnable"), field).makeFinal().withInitialization(New(Type("java.lang.Runnable")) //
				.withTypeDeclaration(ClassDecl("").makeAnonymous().makeLocal() //
					.withMethod(MethodDecl(Type("void"), "run").makePublic().withAnnotation(Annotation(Type("java.lang.Override"))) //
						.withStatements(method.statements()))))) //
			.withStatement(If(Call(Name("java.awt.EventQueue"), "isDispatchThread")) //
				.Then(Block().withStatement(Call(Name(field), "run"))) //
				.Else(elseStatement)));

		method.rebuild();
	}

	private Try generateTryCatchBlock(Call elseStatementRun, final JavacMethod method) {
		return Try(Block() //
				.withStatement(elseStatementRun)) //
			.Catch(Arg(Type("java.lang.InterruptedException"), "$ex1"), Block()) //
			.Catch(Arg(Type("java.lang.reflect.InvocationTargetException"), "$ex2"), Block() //
				.withStatement(LocalDecl(Type("java.lang.Throwable"), "$cause").makeFinal().withInitialization(Call(Name("$ex2"), "getCause")))
				.withStatements(rethrowStatements(method)) //
				.withStatement(Throw(New(Type("java.lang.RuntimeException")).withArgument(Name("$cause")))));
	}

	private List<Statement> rethrowStatements(final JavacMethod method) {
		final List<Statement> rethrowStatements = new ArrayList<Statement>();
		for (TypeRef thrownException : method.thrownExceptions()) {
			rethrowStatements.add(If(InstanceOf(Name("$cause"), thrownException)) //
				.Then(Throw(Cast(thrownException, Name("$cause")))));
		}
		return rethrowStatements;
	}

	private void replaceWithQualifiedThisReference(final JavacMethod method, final JCTree source) {
		final IReplacementProvider<JCExpression> replacement = new QualifiedThisReplacementProvider(method.surroundingType().name(), method.node(), source);
		new ThisReferenceReplaceVisitor(replacement).visit(method.get());
	}
}
