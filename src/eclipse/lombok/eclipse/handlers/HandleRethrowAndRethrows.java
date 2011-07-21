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

import static lombok.core.handlers.RethrowAndRethrowsHandler.*;

import java.util.*;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.handlers.RethrowAndRethrowsHandler;
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
			prepareRethrowAndRethrowsHandler(annotationNode, ast) //
				.withRethrow(new RethrowData(classNames(ann.value()), ann.as(), ann.message())) //
				.handle(Rethrow.class);
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
			RethrowAndRethrowsHandler<EclipseMethod> handler = prepareRethrowAndRethrowsHandler(annotationNode, ast);
			for (Object rethrow: annotation.getActualExpressions("value")) {
				EclipseNode rethrowNode = new InitializableEclipseNode(annotationNode.getAst(), (ASTNode)rethrow, new ArrayList<EclipseNode>(), Kind.ANNOTATION);
				Rethrow ann = Eclipse.createAnnotation(Rethrow.class, rethrowNode).getInstance();
				handler.withRethrow(new RethrowData(classNames(ann.value()), ann.as(), ann.message()));
			}
			handler.handle(Rethrow.class);
		}

		@Override
		public boolean deferUntilPostDiet() {
			return true;
		}
	}
	
	private static RethrowAndRethrowsHandler<EclipseMethod> prepareRethrowAndRethrowsHandler(EclipseNode node, Annotation source) {
		return new RethrowAndRethrowsHandler<EclipseMethod>(EclipseMethod.methodOf(node, source), node);
	}
}
