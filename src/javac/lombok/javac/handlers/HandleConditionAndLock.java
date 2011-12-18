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

import static lombok.javac.handlers.JavacHandlerUtil.deleteAnnotationIfNeccessary;
import static lombok.javac.handlers.Javac.deleteImport;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.handlers.ConditionAndLockHandler;
import lombok.core.handlers.ConditionAndLockHandler.*;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacMethod;
import lombok.javac.handlers.ast.JavacType;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree.JCAnnotation;

public class HandleConditionAndLock {

	private static ConditionAndLockHandler<JavacType, JavacMethod> prepareConditionAndLockHandler(final JavacNode node, final JCAnnotation source,
			final Class<? extends java.lang.annotation.Annotation> annotationType) {
		deleteAnnotationIfNeccessary(node, annotationType);
		deleteImport(node, Position.class);
		return new ConditionAndLockHandler<JavacType, JavacMethod>(JavacType.typeOf(node, source), JavacMethod.methodOf(node, source), node);
	}

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleReadLock extends JavacAnnotationHandler<ReadLock> {
		@Override public void handle(final AnnotationValues<ReadLock> annotation, final JCAnnotation ast, final JavacNode annotationNode) {
			ReadLock ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast, ann.getClass()) //
				.withLockMethod("readLock") //
				.handle(ann.value(), ReadLock.class, new JavacParameterValidator(), new JavacParameterSanitizer());
		}
	}

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleWriteLock extends JavacAnnotationHandler<WriteLock> {
		@Override public void handle(final AnnotationValues<WriteLock> annotation, final JCAnnotation ast, final JavacNode annotationNode) {
			WriteLock ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast, ann.getClass()) //
				.withLockMethod("writeLock") //
				.handle(ann.value(), WriteLock.class, new JavacParameterValidator(), new JavacParameterSanitizer());
		}
	}

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleSignal extends JavacAnnotationHandler<Signal> {
		@Override public void handle(final AnnotationValues<Signal> annotation, final JCAnnotation ast, final JavacNode annotationNode) {
			Signal ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast, ann.getClass()) //
				.withSignal(new SignalData(ann.value(), ann.pos()))
				.handle(ann.lockName(), Signal.class, new JavacParameterValidator(), new JavacParameterSanitizer());
		}
	}

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleAwait extends JavacAnnotationHandler<Await> {
		@Override public void handle(final AnnotationValues<Await> annotation, final JCAnnotation ast, final JavacNode annotationNode) {
			Await ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast, ann.getClass()) //
				.withAwait(new AwaitData(ann.conditionName(), ann.conditionMethod(), ann.pos()))
				.handle(ann.lockName(), Await.class, new JavacParameterValidator(), new JavacParameterSanitizer());
		}
	}

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleAwaitBeforeAndSignalAfter extends JavacAnnotationHandler<AwaitBeforeAndSignalAfter> {
		@Override public void handle(final AnnotationValues<AwaitBeforeAndSignalAfter> annotation, final JCAnnotation ast, final JavacNode annotationNode) {
			AwaitBeforeAndSignalAfter ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast, ann.getClass()) //
				.withAwait(new AwaitData(ann.awaitConditionName(), ann.awaitConditionMethod(), Position.BEFORE))
				.withSignal(new SignalData(ann.signalConditionName(), Position.AFTER))
				.handle(ann.lockName(), AwaitBeforeAndSignalAfter.class, new JavacParameterValidator(), new JavacParameterSanitizer());
		}
	}
}
