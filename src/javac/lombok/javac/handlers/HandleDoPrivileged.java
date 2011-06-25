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

import static lombok.core.util.ErrorMessages.canBeUsedOnConcreteMethodOnly;
import static lombok.core.util.ErrorMessages.canBeUsedOnMethodOnly;
import static lombok.core.util.Names.capitalize;
import static lombok.javac.handlers.Javac.*;
import static lombok.javac.handlers.JavacTreeBuilder.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.lang.annotation.Annotation;
import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCStatement;

import lombok.DoPrivileged;
import lombok.RequiredArgsConstructor;
import lombok.DoPrivileged.SanitizeWith;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.Javac;

/**
 * Handles the {@code lombok.DoPrivileged} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleDoPrivileged extends JavacAnnotationHandler<DoPrivileged> {
	private final static String METHOD_BODY = "%s try { %s java.security.AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<%s>() {" +
			"public %s run() %s { %s %s }}); } catch (java.security.PrivilegedActionException $ex) {" +
			"final java.lang.Throwable $cause = $ex.getCause(); %s throw new java.lang.RuntimeException($cause); }";
	private final static String RETHROW_STATEMENT = "if ($cause instanceof %s) throw (%s) $cause; ";
	private final static String SANITIZE_STATEMENT = "%s %s = %s(%s); ";

	@Override
	public void handle(AnnotationValues<DoPrivileged> annotation, JCAnnotation source, JavacNode annotationNode) {
		final Class<? extends Annotation> annotationType = DoPrivileged.class;
		deleteAnnotationIfNeccessary(annotationNode, annotationType);
		JavacMethod method = JavacMethod.methodOf(annotationNode);

		if (method == null) {
			annotationNode.addError(canBeUsedOnMethodOnly(annotationType));
			return;
		}

		if (method.isAbstract() || method.isEmpty()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return;
		}

		replaceWithQualifiedThisReference(method);

		JCExpression returnType = method.returnType();
		JCExpression innerReturnType = boxedReturnType(method.node(), returnType);
		if (method.returns("void")) {
			replaceReturns(method);
			method.body(statements(method.node(), METHOD_BODY, sanitizeParameter(method), "", innerReturnType, innerReturnType, thrownExceptions(method), method.get().body, "return null;", rethrowStatements(method)));
		} else {
			method.body(statements(method.node(), METHOD_BODY, sanitizeParameter(method), "return", innerReturnType, innerReturnType, thrownExceptions(method), method.get().body, "", rethrowStatements(method)));
		}

		method.rebuild(source);
	}

	private JCExpression boxedReturnType(JavacNode node, JCExpression type) {
		JCExpression objectReturnType = type;
		if (!(type instanceof JCFieldAccess)) {
			final String name = type.toString();
			if ("int".equals(name)) {
				objectReturnType = chainDotsString(node.getTreeMaker(), node, "java.lang.Integer");
			} else if ("char".equals(name)) {
				objectReturnType = chainDotsString(node.getTreeMaker(), node, "java.lang.Character");
			} else {
				objectReturnType = chainDotsString(node.getTreeMaker(), node, "java.lang." + capitalize(name));
			}
		}
		return objectReturnType;
	}

	private String sanitizeParameter(final JavacMethod method) {
		final StringBuilder sanitizeStatements = new StringBuilder();
		for (JCVariableDecl param : method.get().params) {
			final JCAnnotation ann = getAnnotation(SanitizeWith.class, param);
			if (ann != null) {
				final JavacNode annotationNode = method.node().getNodeFor(ann);
				String sanatizeMethodName = Javac.createAnnotation(SanitizeWith.class, annotationNode).getInstance().value();
				final String argumentName = param.name.toString();
				final String newArgumentName = "$" + argumentName;
				sanitizeStatements.append(String.format(SANITIZE_STATEMENT, param.vartype, argumentName, sanatizeMethodName, newArgumentName));
				param.name = method.node().toName(newArgumentName);
				param.mods.flags |= Flags.FINAL;
				param.mods.annotations = remove(param.mods.annotations, ann);
			}
		}
		return sanitizeStatements.toString();
	}

	private String rethrowStatements(final JavacMethod method) {
		final StringBuilder rethrowStatements = new StringBuilder();
		for (JCExpression thrownException : method.get().thrown) {
			rethrowStatements.append(String.format(RETHROW_STATEMENT, thrownException, thrownException));
		}
		return rethrowStatements.toString();
	}

	private String thrownExceptions(final JavacMethod method) {
		final StringBuilder thrownExceptions = new StringBuilder();
		if (!method.get().thrown.isEmpty()) thrownExceptions.append("throws ").append(method.get().thrown);
		return thrownExceptions.toString();
	}

	private void replaceReturns(final JavacMethod method) {
		final IReplacementProvider<JCStatement> replacement = new ReturnNullReplacementProvider(method.node());
		new ReturnStatementReplaceVisitor(replacement).visit(method.get());
	}

	private void replaceWithQualifiedThisReference(final JavacMethod method) {
		final IReplacementProvider<JCExpression> replacement = new QualifiedThisReplacementProvider(typeNodeOf(method.node()).getName(), method.node());
		new ThisReferenceReplaceVisitor(replacement).visit(method.get());
	}

	@RequiredArgsConstructor
	private static class ReturnNullReplacementProvider implements IReplacementProvider<JCStatement> {
		private final JavacNode node;

		@Override
		public JCStatement getReplacement() {
			final TreeMaker maker = node.getTreeMaker();
			return maker.Return(chainDotsString(maker, node, "null"));
		}
	}
}
