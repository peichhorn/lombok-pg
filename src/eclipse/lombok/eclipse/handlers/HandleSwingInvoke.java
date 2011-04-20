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
package lombok.eclipse.handlers;

import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.camelCase;
import static lombok.eclipse.handlers.Eclipse.*;
import static lombok.eclipse.handlers.ast.ASTBuilder.*;
import lombok.RequiredArgsConstructor;
import lombok.SwingInvokeAndWait;
import lombok.SwingInvokeLater;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ThisReferenceReplaceVisitor;
import lombok.eclipse.handlers.ThisReferenceReplaceVisitor.IReplacementProvider;
import lombok.eclipse.handlers.ast.CallBuilder;
import lombok.eclipse.handlers.ast.StatementBuilder;
import lombok.eclipse.handlers.ast.TryBuilder;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.SwingInvokeLater} and {@code lombok.SwingInvokeAndWait} annotation for eclipse.
 */
public class HandleSwingInvoke {
	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleSwingInvokeLater implements EclipseAnnotationHandler<SwingInvokeLater> {
		@Override public boolean handle(AnnotationValues<SwingInvokeLater> annotation, Annotation ast, EclipseNode annotationNode) {
			return new HandleSwingInvoke().generateSwingInvoke("invokeLater", SwingInvokeLater.class, ast, annotationNode);
		}
	}

	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleSwingInvokeAndWait implements EclipseAnnotationHandler<SwingInvokeAndWait> {
		@Override public boolean handle(AnnotationValues<SwingInvokeAndWait> annotation, Annotation ast, EclipseNode annotationNode) {
			return new HandleSwingInvoke().generateSwingInvoke("invokeAndWait", SwingInvokeAndWait.class, ast, annotationNode);
		}
	}

	public boolean generateSwingInvoke(String methodName, Class<? extends java.lang.annotation.Annotation> annotationType, ASTNode source, EclipseNode annotationNode) {
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

		replaceWithQualifiedThisReference(method.node(), source);

		String field = "$" + camelCase(method.name(), "runnable");

		CallBuilder elseStatementRun = Call(Name("java.awt.EventQueue"), methodName).withArgument(Name(field));

		StatementBuilder<? extends Statement> elseStatement;
		if ("invokeAndWait".equals(methodName)) {
			elseStatement =  Block().withStatement(generateTryCatchBlock(elseStatementRun));
		} else {
			elseStatement = Block().withStatement(elseStatementRun);
		}
				
		method.body(source, Block() //
				.withStatement(LocalDef(Type("java.lang.Runnable"), field).makeFinal().withInitialization(New(Type("java.lang.Runnable"), //
						ClassDef("").makeAnonymous().makeLocal() //
							.withMethod(MethodDef(Type("void"), "run").makePublic().withAnnotation(Annotation(Type("java.lang.Override"))) //
								.withStatements(method.get().statements))))) //
				.withStatement(If(Call(Name("java.awt.EventQueue"), "isDispatchThread")) //
						.Then(Block().withStatement(Call(Name(field), "run"))) //
						.Else(elseStatement)));

		method.rebuild();

		return true;
	}

	private TryBuilder generateTryCatchBlock(CallBuilder elseStatementRun) {
		return Try(Block() //
				.withStatement(elseStatementRun)) //
			.Catch(Arg(Type("java.lang.InterruptedException"), "$ex1"), Block()) //
			.Catch(Arg(Type("java.lang.reflect.InvocationTargetException"), "$ex2"), Block() //
				.withStatement(If(NotEqual(Call(Name("$ex2"), "getCause"), Null())) //
						.Then(Throw(New(Type("java.lang.RuntimeException")).withArgument(Call(Name("$ex2"), "getCause"))))));
	}

	private static void replaceWithQualifiedThisReference(final EclipseNode node, final ASTNode source) {
		final EclipseNode parent = typeNodeOf(node);
		final TypeDeclaration typeDec = (TypeDeclaration)parent.get();
		final IReplacementProvider replacement = new HandleSwingInvokeReplacementProvider(new String(typeDec.name), source);
		new ThisReferenceReplaceVisitor(replacement).visit(node.get());
	}

	@RequiredArgsConstructor
	private static class HandleSwingInvokeReplacementProvider implements IReplacementProvider {
		private final String typeName;
		private final ASTNode source;

		@Override public Expression getReplacement() {
			return This(Type(typeName)).build(null, source);
		}
	}
}