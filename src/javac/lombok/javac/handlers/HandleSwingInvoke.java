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

import static lombok.core.util.ErrorMessages.*;
import static lombok.javac.handlers.Javac.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static lombok.javac.handlers.JavacTreeBuilder.*;

import java.lang.annotation.Annotation;

import lombok.SwingInvokeAndWait;
import lombok.SwingInvokeLater;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;

/**
 * Handles the {@code lombok.SwingInvokeLater} and {@code lombok.SwingInvokeAndWait} annotation for javac.
 */
public class HandleSwingInvoke {
	private final static String METHOD_BODY = "final java.lang.Runnable %s = new java.lang.Runnable(){ " + //
		"@java.lang.Override public void run() %s }; if (java.awt.EventQueue.isDispatchThread()) { %s.run(); } else { %s }";
	private final static String TRY_CATCH_BLOCK = //
		"try { %s } catch (final java.lang.InterruptedException $ex1) { " + //
		"} catch (final java.lang.reflect.InvocationTargetException $ex2) { " + //
		"final java.lang.Throwable $cause = $ex2.getCause();" + //
		" %s " + //
		"throw new java.lang.RuntimeException($cause); }";
	private final static String RETHROW_EXCEPTION = "if ($cause instanceof %s) throw (%s) $cause;";
	private final static String ELSE_STATEMENT = "java.awt.EventQueue.%s(%s);";

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleSwingInvokeLater extends JavacNonResolutionBasedHandler implements JavacAnnotationHandler<SwingInvokeLater> {
		@Override public boolean handle(AnnotationValues<SwingInvokeLater> annotation, JCAnnotation ast, JavacNode annotationNode) {
			return new HandleSwingInvoke().generateSwingInvoke("invokeLater", SwingInvokeLater.class, annotationNode);
		}
	}

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleSwingInvokeAndWait extends JavacNonResolutionBasedHandler implements JavacAnnotationHandler<SwingInvokeAndWait> {
		@Override public boolean handle(AnnotationValues<SwingInvokeAndWait> annotation, JCAnnotation ast, JavacNode annotationNode) {
			return new HandleSwingInvoke().generateSwingInvoke("invokeAndWait", SwingInvokeAndWait.class, annotationNode);
		}
	}

	public boolean generateSwingInvoke(String methodName, Class<? extends Annotation> annotationType, JavacNode annotationNode) {
		markAnnotationAsProcessed(annotationNode, annotationType);
		JavacMethod method = JavacMethod.methodOf(annotationNode);

		if (method == null) {
			annotationNode.addError(canBeUsedOnMethodOnly(annotationType));
			return true;
		}

		if (method.isAbstract() || method.isEmpty()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return true;
		}

		replaceWithQualifiedThisReference(method);

		String fieldName = "$" + method.name() + "Runnable";

		String elseStatement = String.format(ELSE_STATEMENT, methodName, fieldName);
		if ("invokeAndWait".equals(methodName)) {
			StringBuilder rethrowExceptions = new StringBuilder();
			for (JCExpression thrownException : method.get().thrown) {
				rethrowExceptions.append(String.format(RETHROW_EXCEPTION, thrownException, thrownException));
			}
			elseStatement = String.format(TRY_CATCH_BLOCK, elseStatement, rethrowExceptions);
		}
		method.body(statements(method.node(), METHOD_BODY, fieldName, method.get().body, fieldName, elseStatement));

		method.rebuild();

		return true;
	}

	private void replaceWithQualifiedThisReference(final JavacMethod method) {
		final IReplacementProvider<JCExpression> replacement = new QualifiedThisReplacementProvider(typeNodeOf(method.node()).getName(), method.node());
		new ThisReferenceReplaceVisitor(replacement).visit(method.get());
	}
}
