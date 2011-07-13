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

import static lombok.ast.AST.*;
import static lombok.core.util.Arrays.*;
import static lombok.core.util.ErrorMessages.*;

import java.util.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.util.Lists;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.InitializableEclipseNode;
import lombok.eclipse.Eclipse;
import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.mangosdk.spi.ProviderFor;

public class HandleRethrowAndRethrows {

	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleRethrow extends EclipseAnnotationHandler<Rethrow> {
		@Override
		public void handle(AnnotationValues<Rethrow> annotation, Annotation ast, EclipseNode annotationNode) {
			Rethrow ann = annotation.getInstance();
			new HandleRethrowAndRethrows() //
				.withRethrow(new RethrowData(classNames(ann.value()), ann.as(), ann.message())) //
				.handle(Rethrow.class, ast, annotationNode);
		}

		@Override
		public boolean deferUntilPostDiet() {
			return true;
		}
	}

	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleRethrows extends EclipseAnnotationHandler<Rethrows> {
		@Override
		public void handle(AnnotationValues<Rethrows> annotation, Annotation ast, EclipseNode annotationNode) {
			HandleRethrowAndRethrows handle = new HandleRethrowAndRethrows();
			for (Object rethrow: annotation.getActualExpressions("value")) {
				EclipseNode rethrowNode = new InitializableEclipseNode(annotationNode.getAst(), (ASTNode)rethrow, new ArrayList<EclipseNode>(), Kind.ANNOTATION);
				Rethrow ann = Eclipse.createAnnotation(Rethrow.class, rethrowNode).getInstance();
				handle.withRethrow(new RethrowData(classNames(ann.value()), ann.as(), ann.message()));
			}
			handle.handle(Rethrow.class, ast, annotationNode);
		}

		@Override
		public boolean deferUntilPostDiet() {
			return true;
		}
	}

	private List<RethrowData> rethrows = new ArrayList<RethrowData>();

	public HandleRethrowAndRethrows withRethrow(final RethrowData rethrowData) {
		rethrows.add(rethrowData);
		return this;
	}

	public void handle(Class<? extends java.lang.annotation.Annotation> annotationType, Annotation source, EclipseNode annotationNode) {

		if (rethrows.isEmpty()) {
			return;
		}

		EclipseMethod method = EclipseMethod.methodOf(annotationNode, source);

		if (method == null) {
			annotationNode.addError(canBeUsedOnMethodOnly(annotationType));
			return;
		}

		if (method.isAbstract() || method.isEmpty()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return;
		}

		Try tryBuilder = Try(Block().withStatements(method.statements()));
		int counter = 1;
		for (RethrowData rethrow : rethrows) {
			for (Class<?> thrown : rethrow.thrown) {
				String varname = "$e" + counter++;
				if (RethrowData.class == thrown) {
					tryBuilder.Catch(Arg(Type("java.lang.RuntimeException"), varname), Block().withStatement(Throw(Name(varname))));
				} else if (rethrow.message.isEmpty()) {
					tryBuilder.Catch(Arg(Type(thrown.getName()), varname), Block().withStatement(Throw(New(Type(rethrow.as.getName())).withArgument(Name(varname)))));
				} else {
					tryBuilder.Catch(Arg(Type(thrown.getName()), varname), Block().withStatement(Throw(New(Type(rethrow.as.getName())).withArgument(String(rethrow.message)).withArgument(Name(varname)))));
				}
			}
		}
		method.body(Block().withStatement(tryBuilder));

		method.rebuild();
	}

	private static List<Class<?>> classNames(final Class<?>[] classes) {
		if (isEmpty(classes)) {
			return Lists.<Class<?>>list(RethrowData.class, Exception.class);
		}
		return Lists.list(classes);
	}

	@RequiredArgsConstructor
	private static class RethrowData {
		public final List<Class<?>> thrown;
		public final Class<?> as;
		public final String message;
	}
}
