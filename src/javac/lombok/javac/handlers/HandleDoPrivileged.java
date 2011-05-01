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
import static lombok.javac.handlers.Javac.typeNodeOf;
import static lombok.javac.handlers.JavacTreeBuilder.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.lang.annotation.Annotation;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCStatement;

import lombok.DoPrivileged;
import lombok.RequiredArgsConstructor;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;

@ProviderFor(JavacAnnotationHandler.class)
public class HandleDoPrivileged extends JavacNonResolutionBasedHandler implements JavacAnnotationHandler<DoPrivileged> {
	private final static String METHOD_BODY = "%s java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<%s>() {" +
			"public %s run() { %s %s }});";
	
	@Override
	public boolean handle(AnnotationValues<DoPrivileged> annotation, JCAnnotation ast, JavacNode annotationNode) {
		final Class<? extends Annotation> annotationType = DoPrivileged.class;
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

		JCExpression returnType = method.returnType();
		JCExpression innerReturnType = boxedReturnType(method.node(), returnType);

		if (method.returns("void")) {
			replaceReturns(method);
			method.body(statements(method.node(), METHOD_BODY, "", innerReturnType, innerReturnType, method.get().body, "return null;"));
		} else {
			method.body(statements(method.node(), METHOD_BODY, "return", innerReturnType, innerReturnType, method.get().body, ""));
		}

		method.rebuild();

		return true;
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
