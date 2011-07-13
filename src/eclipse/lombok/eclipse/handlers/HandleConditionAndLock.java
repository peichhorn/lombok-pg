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

import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;

import java.util.*;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseMethod;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.mangosdk.spi.ProviderFor;

public class HandleConditionAndLock {
	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleReadLock extends EclipseAnnotationHandler<ReadLock> {
		@Override public void preHandle(AnnotationValues<ReadLock> annotation, Annotation ast, EclipseNode annotationNode) {
			ReadLock ann = annotation.getInstance();
			new HandleConditionAndLock()
				.withLockMethod("readLock")
				.preHandle(ann.value(), ReadLock.class, ast, annotationNode);
		}

		@Override public void handle(AnnotationValues<ReadLock> annotation, Annotation ast, EclipseNode annotationNode) {
			ReadLock ann = annotation.getInstance();
			new HandleConditionAndLock()
				.withLockMethod("readLock")
				.handle(ann.value(), ReadLock.class, ast, annotationNode);
		}

		@Override
		public boolean deferUntilPostDiet() {
			return true;
		}
	}

	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleWriteLock extends EclipseAnnotationHandler<WriteLock> {
		@Override public void preHandle(AnnotationValues<WriteLock> annotation, Annotation ast, EclipseNode annotationNode) {
			WriteLock ann = annotation.getInstance();
			new HandleConditionAndLock()
				.withLockMethod("writeLock")
				.preHandle(ann.value(), WriteLock.class, ast, annotationNode);
		}

		@Override public void handle(AnnotationValues<WriteLock> annotation, Annotation ast, EclipseNode annotationNode) {
			WriteLock ann = annotation.getInstance();
			new HandleConditionAndLock()
				.withLockMethod("writeLock")
				.handle(ann.value(), WriteLock.class, ast, annotationNode);
		}

		@Override
		public boolean deferUntilPostDiet() {
			return true;
		}
	}

	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleSignal extends EclipseAnnotationHandler<Signal> {
		@Override public void preHandle(AnnotationValues<Signal> annotation, Annotation ast, EclipseNode annotationNode) {
			Signal ann = annotation.getInstance();
			new HandleConditionAndLock()
				.withSignal(new SignalData(ann.value(), ann.pos()))
				.preHandle(ann.lockName(), Signal.class, ast, annotationNode);
		}

		@Override public void handle(AnnotationValues<Signal> annotation, Annotation ast, EclipseNode annotationNode) {
			Signal ann = annotation.getInstance();
			new HandleConditionAndLock()
				.withSignal(new SignalData(ann.value(), ann.pos()))
				.handle(ann.lockName(), Signal.class, ast, annotationNode);
		}

		@Override
		public boolean deferUntilPostDiet() {
			return true;
		}
	}

	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleAwait extends EclipseAnnotationHandler<Await> {
		@Override public void preHandle(AnnotationValues<Await> annotation, Annotation ast, EclipseNode annotationNode) {
			Await ann = annotation.getInstance();
			new HandleConditionAndLock()
				.withAwait(new AwaitData(ann.conditionName(), ann.conditionMethod(), ann.pos()))
				.preHandle(ann.lockName(), Await.class, ast, annotationNode);
		}

		@Override public void handle(AnnotationValues<Await> annotation, Annotation ast, EclipseNode annotationNode) {
			Await ann = annotation.getInstance();
			new HandleConditionAndLock()
				.withAwait(new AwaitData(ann.conditionName(), ann.conditionMethod(), ann.pos()))
				.handle(ann.lockName(), Await.class, ast, annotationNode);
		}

		@Override
		public boolean deferUntilPostDiet() {
			return true;
		}
	}

	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleAwaitBeforeAndSignalAfter extends EclipseAnnotationHandler<AwaitBeforeAndSignalAfter> {
		@Override public void preHandle(AnnotationValues<AwaitBeforeAndSignalAfter> annotation, Annotation ast, EclipseNode annotationNode) {
			AwaitBeforeAndSignalAfter ann = annotation.getInstance();
			new HandleConditionAndLock()
				.withAwait(new AwaitData(ann.awaitConditionName(), ann.awaitConditionMethod(), Position.BEFORE))
				.withSignal(new SignalData(ann.signalConditionName(), Position.AFTER))
				.preHandle(ann.lockName(), AwaitBeforeAndSignalAfter.class, ast, annotationNode);
		}

		@Override public void handle(AnnotationValues<AwaitBeforeAndSignalAfter> annotation, Annotation ast, EclipseNode annotationNode) {
			AwaitBeforeAndSignalAfter ann = annotation.getInstance();
			new HandleConditionAndLock()
				.withAwait(new AwaitData(ann.awaitConditionName(), ann.awaitConditionMethod(), Position.BEFORE))
				.withSignal(new SignalData(ann.signalConditionName(), Position.AFTER))
				.handle(ann.lockName(), AwaitBeforeAndSignalAfter.class, ast, annotationNode);
		}

		@Override
		public boolean deferUntilPostDiet() {
			return true;
		}
	}

	private AwaitData await;
	private SignalData signal;
	private String lockMethod;

	public HandleConditionAndLock withAwait(final AwaitData await) {
		this.await = await;
		return this;
	}

	public HandleConditionAndLock withSignal(final SignalData signal) {
		this.signal = signal;
		return this;
	}

	public HandleConditionAndLock withLockMethod(final String lockMethod) {
		this.lockMethod = lockMethod;
		return this;
	}

	public boolean preHandle(String lockName, Class<? extends java.lang.annotation.Annotation> annotationType, Annotation source, EclipseNode annotationNode) {
		final EclipseType type = EclipseType.typeOf(annotationNode, source);
		final EclipseMethod method = EclipseMethod.methodOf(annotationNode, source);
		if (method == null) {
			annotationNode.addError(canBeUsedOnMethodOnly(annotationType));
			return false;
		}
		if (method.isAbstract()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return false;
		}

		boolean lockMode = lockMethod != null;

		if (!lockMode && (await == null) && (signal == null)) {
			return false; // wrong configured handler, so better stop here
		}

		String annotationTypeName = annotationType.getSimpleName();
		String completeLockName = createCompleteLockName(lockName);

		if (!tryToAddLockField(type, annotationNode, completeLockName, lockMode, annotationTypeName)) return false;

		if (!lockMode) {
			if (!tryToAddConditionField(type, annotationNode, await, completeLockName, annotationTypeName)) return false;
			if (!tryToAddConditionField(type, annotationNode, signal, completeLockName, annotationTypeName)) return false;
		}
		return true;
	}

	public void handle(String lockName, Class<? extends java.lang.annotation.Annotation> annotationType, Annotation source, EclipseNode annotationNode) {
		if (!preHandle(lockName, annotationType, source, annotationNode)) return;

		final EclipseType type = EclipseType.typeOf(annotationNode, source);
		final EclipseMethod method = EclipseMethod.methodOf(annotationNode, source);

		boolean lockMode = lockMethod != null;

		String annotationTypeName = annotationType.getSimpleName();
		String completeLockName = createCompleteLockName(lockName);

		List<lombok.ast.Statement> beforeMethodBlock = new ArrayList<lombok.ast.Statement>();
		List<lombok.ast.Statement> afterMethodBlock = new ArrayList<lombok.ast.Statement>();

		if (!lockMode) {
			if (!getConditionStatements(type, annotationNode, await, completeLockName, annotationTypeName, beforeMethodBlock, afterMethodBlock)) return;
			if (!getConditionStatements(type, annotationNode, signal, completeLockName, annotationTypeName, beforeMethodBlock, afterMethodBlock)) return;
		}

		final lombok.ast.Call lockCall;
		final lombok.ast.Call unLockCall;
		if (lockMode) {
			lockCall = Call(Call(Field(This(), completeLockName), lockMethod), "lock");
			unLockCall = Call(Call(Field(This(), completeLockName), lockMethod), "unlock");
		} else {
			lockCall = Call(Field(This(), completeLockName), "lock");
			unLockCall = Call(Field(This(), completeLockName), "unlock");
		}

		method.body(Block() //
			.withStatement(lockCall)
			.withStatement(Try(Block() //
				.withStatements(beforeMethodBlock) //
				.withStatements(method.statements()) //
				.withStatements(afterMethodBlock)//
			).Finally(Block() //
				.withStatement(unLockCall) //
			) //
		));

		if (await != null) {
			method.withException(Type("java.lang.InterruptedException"));
		}

		method.rebuild();
	}

	private boolean getConditionStatements(EclipseType type, EclipseNode annotationNode, ConditionData condition, String lockName, String annotationTypeName, List<lombok.ast.Statement> before, List<lombok.ast.Statement> after) {
		if (condition == null) {
			return true;
		}
		if (tryToAddConditionField(type, annotationNode, condition, lockName, annotationTypeName)) {
			switch (condition.pos) {
			case BEFORE:
				before.add(condition.toStatement());
				break;
			default:
			case AFTER:
				after.add(condition.toStatement());
				break;
			}
			return true;
		}
		return false;
	}

	private String createCompleteLockName(String lockName) {
		String completeLockName = lockName;
		if ((lockMethod == null) && trim(lockName).isEmpty()) {
			String awaitCondition = trim(await == null ? "" : await.condition);
			String signalCondition = trim(signal == null ? "" : signal.condition);
			completeLockName = "$" + camelCase(awaitCondition, signalCondition, "lock");
		}
		return completeLockName;
	}

	private static boolean tryToAddLockField(EclipseType type, EclipseNode annotationNode, String lockName, boolean isReadWriteLock, String annotationTypeName) {
		lockName = trim(lockName);
		if (lockName.isEmpty()) {
			annotationNode.addError(String.format("@%s 'lockName' may not be empty or null.", annotationTypeName));
			return false;
		}
		if (!type.hasField(lockName)) {
			if(isReadWriteLock) {
				type.injectField(FieldDecl(Type("java.util.concurrent.locks.ReadWriteLock"), lockName).makePrivate().makeFinal() //
					.withInitialization(New(Type("java.util.concurrent.locks.ReentrantReadWriteLock"))));
			} else {
				type.injectField(FieldDecl(Type("java.util.concurrent.locks.Lock"), lockName).makePrivate().makeFinal() //
					.withInitialization(New(Type("java.util.concurrent.locks.ReentrantLock"))));
			}
		} else {
			// TODO type check
			// java.util.concurrent.locks.ReadWriteLock
			// java.util.concurrent.locks.Lock
		}
		return true;
	}

	private static boolean tryToAddConditionField(EclipseType type, EclipseNode annotationNode, ConditionData condition, String lockName, String annotationTypeName) {
		if (condition == null) {
			return true;
		}
		String conditionName = trim(condition.condition);
		if (conditionName.isEmpty()) {
			annotationNode.addError(String.format("@%s 'conditionName' may not be empty or null.", annotationTypeName));
			return false;
		}
		if (!type.hasField(conditionName)) {
			type.injectField(FieldDecl(Type("java.util.concurrent.locks.Condition"), conditionName).makePrivate().makeFinal() //
				.withInitialization(Call(Name(lockName), "newCondition")));
		} else {
			// TODO type check
			// java.util.concurrent.locks.Condition
		}
		return true;
	}

	private static class AwaitData extends ConditionData {
		public final String conditionMethod;

		public AwaitData(final String condition, final String conditionMethod, final Position pos) {
			super(condition, pos);
			this.conditionMethod = conditionMethod;
		}

		@Override
		public lombok.ast.Statement toStatement() {
			return While(Call(This(), conditionMethod)).Do(Call(Field(This(), condition), "await"));
		}
	}

	private static class SignalData extends ConditionData {
		public SignalData(final String condition, final Position pos) {
			super(condition, pos);
		}

		@Override
		public lombok.ast.Statement toStatement() {
			return Call(Field(This(), condition), "signal");
		}
	}

	@RequiredArgsConstructor
	private static abstract class ConditionData {
		public final String condition;
		public final Position pos;

		public abstract lombok.ast.Statement toStatement();
	}
}
