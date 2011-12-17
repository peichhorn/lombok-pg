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

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.handlers.ConditionAndLockHandler;
import lombok.core.handlers.ConditionAndLockHandler.*;
import lombok.eclipse.DeferUntilPostDiet;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseMethod;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.mangosdk.spi.ProviderFor;

public class HandleConditionAndLock {

	/**
	 * Handles the {@code lombok.ReadLock} annotation for eclipse.
	 */
	@ProviderFor(EclipseAnnotationHandler.class)
	@DeferUntilPostDiet
	public static class HandleReadLock extends EclipseAnnotationHandler<ReadLock> {
		@Override
		public void preHandle(final AnnotationValues<ReadLock> annotation, final Annotation ast, final EclipseNode annotationNode) {
			ReadLock ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast) //
					.withLockMethod("readLock") //
					.preHandle(ann.value(), ReadLock.class);
		}

		@Override
		public void handle(final AnnotationValues<ReadLock> annotation, final Annotation ast, final EclipseNode annotationNode) {
			ReadLock ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast) //
					.withLockMethod("readLock") //
					.handle(ann.value(), ReadLock.class, new EclipseParameterValidator(), new EclipseParameterSanitizer());
		}
	}

	/**
	 * Handles the {@code lombok.WriteLock} annotation for eclipse.
	 */
	@ProviderFor(EclipseAnnotationHandler.class)
	@DeferUntilPostDiet
	public static class HandleWriteLock extends EclipseAnnotationHandler<WriteLock> {
		@Override
		public void preHandle(final AnnotationValues<WriteLock> annotation, final Annotation ast, final EclipseNode annotationNode) {
			WriteLock ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast) //
					.withLockMethod("writeLock") //
					.preHandle(ann.value(), WriteLock.class);
		}

		@Override
		public void handle(final AnnotationValues<WriteLock> annotation, final Annotation ast, final EclipseNode annotationNode) {
			WriteLock ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast) //
					.withLockMethod("writeLock") //
					.handle(ann.value(), WriteLock.class, new EclipseParameterValidator(), new EclipseParameterSanitizer());
		}
	}

	/**
	 * Handles the {@code lombok.Signal} annotation for eclipse.
	 */
	@ProviderFor(EclipseAnnotationHandler.class)
	@DeferUntilPostDiet
	public static class HandleSignal extends EclipseAnnotationHandler<Signal> {
		@Override
		public void preHandle(final AnnotationValues<Signal> annotation, final Annotation ast, final EclipseNode annotationNode) {
			Signal ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast) //
					.withSignal(new SignalData(ann.value(), ann.pos())) //
					.preHandle(ann.lockName(), Signal.class);
		}

		@Override
		public void handle(final AnnotationValues<Signal> annotation, final Annotation ast, final EclipseNode annotationNode) {
			Signal ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast) //
					.withSignal(new SignalData(ann.value(), ann.pos())) //
					.handle(ann.lockName(), Signal.class, new EclipseParameterValidator(), new EclipseParameterSanitizer());
		}
	}

	/**
	 * Handles the {@code lombok.Await} annotation for eclipse.
	 */
	@ProviderFor(EclipseAnnotationHandler.class)
	@DeferUntilPostDiet
	public static class HandleAwait extends EclipseAnnotationHandler<Await> {
		@Override
		public void preHandle(final AnnotationValues<Await> annotation, final Annotation ast, final EclipseNode annotationNode) {
			Await ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast) //
					.withAwait(new AwaitData(ann.conditionName(), ann.conditionMethod(), ann.pos())) //
					.preHandle(ann.lockName(), Await.class);
		}

		@Override
		public void handle(final AnnotationValues<Await> annotation, final Annotation ast, final EclipseNode annotationNode) {
			Await ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast) //
					.withAwait(new AwaitData(ann.conditionName(), ann.conditionMethod(), ann.pos())) //
					.handle(ann.lockName(), Await.class, new EclipseParameterValidator(), new EclipseParameterSanitizer());
		}
	}

	/**
	 * Handles the {@code lombok.AwaitBeforeAndSignalAfter} annotation for eclipse.
	 */
	@ProviderFor(EclipseAnnotationHandler.class)
	@DeferUntilPostDiet
	public static class HandleAwaitBeforeAndSignalAfter extends EclipseAnnotationHandler<AwaitBeforeAndSignalAfter> {
		@Override
		public void preHandle(final AnnotationValues<AwaitBeforeAndSignalAfter> annotation, final Annotation ast, final EclipseNode annotationNode) {
			AwaitBeforeAndSignalAfter ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast) //
					.withAwait(new AwaitData(ann.awaitConditionName(), ann.awaitConditionMethod(), Position.BEFORE)) //
					.withSignal(new SignalData(ann.signalConditionName(), Position.AFTER)) //
					.preHandle(ann.lockName(), AwaitBeforeAndSignalAfter.class);
		}

		@Override
		public void handle(final AnnotationValues<AwaitBeforeAndSignalAfter> annotation, final Annotation ast, final EclipseNode annotationNode) {
			AwaitBeforeAndSignalAfter ann = annotation.getInstance();
			prepareConditionAndLockHandler(annotationNode, ast) //
					.withAwait(new AwaitData(ann.awaitConditionName(), ann.awaitConditionMethod(), Position.BEFORE)) //
					.withSignal(new SignalData(ann.signalConditionName(), Position.AFTER)) //
					.handle(ann.lockName(), AwaitBeforeAndSignalAfter.class, new EclipseParameterValidator(), new EclipseParameterSanitizer());
		}
	}

	private static ConditionAndLockHandler<EclipseType, EclipseMethod> prepareConditionAndLockHandler(final EclipseNode node, final Annotation source) {
		return new ConditionAndLockHandler<EclipseType, EclipseMethod>(EclipseType.typeOf(node, source), EclipseMethod.methodOf(node, source), node);
	}
}
