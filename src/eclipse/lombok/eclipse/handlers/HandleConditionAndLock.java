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

import static lombok.eclipse.handlers.ast.ASTBuilder.*;
import static lombok.core.util.Names.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;

import java.util.ArrayList;
import java.util.List;

import lombok.Await;
import lombok.AwaitBeforeAndSignalAfter;
import lombok.Position;
import lombok.ReadLock;
import lombok.RequiredArgsConstructor;
import lombok.Signal;
import lombok.WriteLock;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.EclipseHandlerUtil.MemberExistsResult;
import lombok.eclipse.handlers.ast.MessageSendBuilder;
import lombok.eclipse.handlers.ast.StatementBuilder;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.mangosdk.spi.ProviderFor;

public class HandleConditionAndLock {
	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleReadLock implements EclipseAnnotationHandler<ReadLock> {
		@Override public boolean handle(AnnotationValues<ReadLock> annotation, Annotation ast, EclipseNode annotationNode) {
			ReadLock ann = annotation.getInstance();
			return new HandleConditionAndLock()
					.withLockMethod("readLock")
					.handle(ann.value(), ReadLock.class, ast, annotationNode);
		}
	}

	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleWriteLock  implements EclipseAnnotationHandler<WriteLock> {
		@Override public boolean handle(AnnotationValues<WriteLock> annotation, Annotation ast, EclipseNode annotationNode) {
			WriteLock ann = annotation.getInstance();
			return new HandleConditionAndLock()
					.withLockMethod("writeLock")
					.handle(ann.value(), WriteLock.class, ast, annotationNode);
		}
	}

	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleSignal implements EclipseAnnotationHandler<Signal> {
		@Override public boolean handle(AnnotationValues<Signal> annotation, Annotation ast, EclipseNode annotationNode) {
			Signal ann = annotation.getInstance();
			return new HandleConditionAndLock()
					.withSignal(new SignalData(ann.value(), ann.pos()))
					.handle(ann.lockName(), Signal.class, ast, annotationNode);
		}
	}

	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleAwait implements EclipseAnnotationHandler<Await> {
		@Override public boolean handle(AnnotationValues<Await> annotation, Annotation ast, EclipseNode annotationNode) {
			Await ann = annotation.getInstance();
			return new HandleConditionAndLock()
					.withAwait(new AwaitData(ann.value(), ann.conditionMethod(), ann.pos()))
					.handle(ann.lockName(), Await.class, ast, annotationNode);
		}
	}

	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleAwaitBeforeAndSignalAfter implements EclipseAnnotationHandler<AwaitBeforeAndSignalAfter> {
		@Override public boolean handle(AnnotationValues<AwaitBeforeAndSignalAfter> annotation, Annotation ast, EclipseNode annotationNode) {
			AwaitBeforeAndSignalAfter ann = annotation.getInstance();
			return new HandleConditionAndLock()
					.withAwait(new AwaitData(ann.awaitConditionName(), ann.awaitConditionMethod(), Position.BEFORE))
					.withSignal(new SignalData(ann.signalConditionName(), Position.AFTER))
					.handle(ann.lockName(), AwaitBeforeAndSignalAfter.class, ast, annotationNode);
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

	public boolean handle(String lockName, Class<? extends java.lang.annotation.Annotation> annotationType, Annotation source, EclipseNode annotationNode) {
		EclipseMethod method = EclipseMethod.methodOf(annotationNode);
		if (method == null) {
			annotationNode.addError(canBeUsedOnMethodOnly(annotationType));
			return false;
		}
		if (method.isAbstract()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return false;
		}

		String annotationTypeName = annotationType.getSimpleName();

		boolean lockMode = lockMethod != null;

		if (!lockMode && (await == null) && (signal == null)) {
			annotationNode.addWarning(String.format("Bad configured Handler for %s. Please file a bug report.", annotationTypeName));
			return true; // wrong configured handler, so better stop here
		}

		String completeLockName = createCompleteLockName(lockName);

		if (!tryToAddLockField(source, annotationNode, completeLockName, lockMode, annotationTypeName)) {
			return false;
		}

		if (!lockMode) {
			tryToAddConditionField(source, annotationNode, await, completeLockName, annotationTypeName);
			tryToAddConditionField(source, annotationNode, signal, completeLockName, annotationTypeName);
		}

		if (!method.wasCompletelyParsed()) {
			return false; // we need the method body
		}

		List<StatementBuilder<? extends Statement>> beforeMethodBlock = new ArrayList<StatementBuilder<? extends Statement>>();
		List<StatementBuilder<? extends Statement>> afterMethodBlock = new ArrayList<StatementBuilder<? extends Statement>>();

		if (!lockMode) {
			if (!getConditionStatements(source, annotationNode, await, completeLockName, annotationTypeName, beforeMethodBlock, afterMethodBlock)) {
				return false;
			}
			if (!getConditionStatements(source, annotationNode, signal, completeLockName, annotationTypeName, beforeMethodBlock, afterMethodBlock)) {
				return false;
			}
		}
		
		final MessageSendBuilder lockCall;
		final MessageSendBuilder unLockCall;
		if (lockMode) {
			lockCall = Call(Call(Field(This(), completeLockName), lockMethod), "lock");
			unLockCall = Call(Call(Field(This(), completeLockName), lockMethod), "unlock");
		} else {
			lockCall = Call(Field(This(), completeLockName), "lock");
			unLockCall = Call(Field(This(), completeLockName), "unlock");
		}
		
		method.body(source, Block() //
			.withStatement(lockCall)
			.withStatement(Try(Block() //
				.withStatements(beforeMethodBlock) //
				.withStatements(method.get().statements) //
				.withStatements(afterMethodBlock)//
			).Finally(Block() //
				.withStatement(unLockCall) //
			) //
		));

		if (await != null) {
			method.withException(Type("java.lang.InterruptedException"));
		}

		method.rebuild();

		return true;
	}

	private boolean getConditionStatements(ASTNode source, EclipseNode node, ConditionData condition, String lockName, String annotationTypeName, List<StatementBuilder<? extends Statement>> before, List<StatementBuilder<? extends Statement>> after) {
		if (condition == null) {
			return true;
		}
		if (tryToAddConditionField(source, node, condition, lockName, annotationTypeName)) {
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
		if (lockMethod == null) {
			if (trim(lockName).isEmpty()) {
				String awaitCondition = trim(await == null ? "" : await.condition);
				String signalCondition = trim(signal == null ? "" : signal.condition);
				completeLockName = "$" + camelCase(awaitCondition, signalCondition, "lock");
			}
		}
		return completeLockName;
	}

	private static boolean tryToAddLockField(ASTNode source, EclipseNode annotationNode, String lockName, boolean isReadWriteLock, String annotationTypeName) {
		lockName = trim(lockName);
		if (lockName.isEmpty()) {
			annotationNode.addError(String.format("@%s 'lockName' may not be empty or null.", annotationTypeName));
			return false;
		}
		EclipseNode methodNode = annotationNode.up();
		if (fieldExists(lockName, methodNode) == MemberExistsResult.NOT_EXISTS) {
			if(isReadWriteLock) {
				FieldDef(Type("java.util.concurrent.locks.ReadWriteLock"), lockName).makePrivateFinal() //
					.withInitialization(New(Type("java.util.concurrent.locks.ReentrantReadWriteLock"))).injectInto(methodNode, source);
			} else {
				FieldDef(Type("java.util.concurrent.locks.Lock"), lockName).makePrivateFinal() //
					.withInitialization(New(Type("java.util.concurrent.locks.ReentrantLock"))).injectInto(methodNode, source);
			}
		} else {
			// TODO type check
			// java.util.concurrent.locks.ReadWriteLock
			// java.util.concurrent.locks.Lock
		}
		return true;
	}

	private static boolean tryToAddConditionField(ASTNode source, EclipseNode annotationNode, ConditionData condition, String lockName, String annotationTypeName) {
		if (condition == null) {
			return true;
		}
		String conditionName = trim(condition.condition);
		if (conditionName.isEmpty()) {
			annotationNode.addError(String.format("@%s 'conditionName' may not be empty or null.", annotationTypeName));
			return false;
		}
		EclipseNode methodNode = annotationNode.up();
		if (fieldExists(conditionName, methodNode) == MemberExistsResult.NOT_EXISTS) {
			FieldDef(Type("java.util.concurrent.locks.Condition"), conditionName).makePrivateFinal() //
				.withInitialization(Call(Name(lockName), "newCondition")).injectInto(methodNode, source);
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
		public StatementBuilder<? extends Statement> toStatement() {
			return While(Call(This(), conditionMethod)).Do(Call(Field(This(), condition), "await"));
		}
	}

	private static class SignalData extends ConditionData {
		public SignalData(final String condition, final Position pos) {
			super(condition, pos);
		}

		@Override
		public StatementBuilder<? extends Statement> toStatement() {
			return Call(Field(This(), condition), "signal");
		}
	}

	@RequiredArgsConstructor
	private static abstract class ConditionData {
		public final String condition;
		public final Position pos;

		public abstract StatementBuilder<? extends Statement> toStatement();
	}
}
