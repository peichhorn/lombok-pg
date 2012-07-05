/*
 * Copyright © 2011-2012 Philipp Eichhorn
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
package lombok.core.handlers;

import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;

import java.util.*;
import java.util.concurrent.locks.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.DiagnosticsReceiver;

@RequiredArgsConstructor
public final class ConditionAndLockHandler<TYPE_TYPE extends IType<METHOD_TYPE, ?, ?, ?, ?, ?>, METHOD_TYPE extends IMethod<TYPE_TYPE, ?, ?, ?>> {
	private final TYPE_TYPE type;
	private final METHOD_TYPE method;
	private final DiagnosticsReceiver diagnosticsReceiver;
	private AwaitData await;
	private SignalData signal;
	private String lockMethod;

	public ConditionAndLockHandler<TYPE_TYPE, METHOD_TYPE> withAwait(final AwaitData await) {
		this.await = await;
		return this;
	}

	public ConditionAndLockHandler<TYPE_TYPE, METHOD_TYPE> withSignal(final SignalData signal) {
		this.signal = signal;
		return this;
	}

	public ConditionAndLockHandler<TYPE_TYPE, METHOD_TYPE> withLockMethod(final String lockMethod) {
		this.lockMethod = lockMethod;
		return this;
	}

	public boolean preHandle(final String lockName, final Class<? extends java.lang.annotation.Annotation> annotationType) {
		if (method == null) {
			diagnosticsReceiver.addError(canBeUsedOnMethodOnly(annotationType));
			return false;
		}
		if (method.isAbstract()) {
			diagnosticsReceiver.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return false;
		}

		boolean isReadWriteLock = lockMethod != null;

		if (!isReadWriteLock && (await == null) && (signal == null)) {
			return false; // wrong configured handler, so better stop here
		}

		String annotationTypeName = annotationType.getSimpleName();
		String completeLockName = createCompleteLockName(lockName, isReadWriteLock);

		if (!tryToAddLockField(completeLockName, isReadWriteLock, annotationTypeName)) return false;

		if (!isReadWriteLock) {
			if (!tryToAddConditionField(await, completeLockName, annotationTypeName)) return false;
			if (!tryToAddConditionField(signal, completeLockName, annotationTypeName)) return false;
		}
		return true;
	}

	public void handle(final String lockName, final Class<? extends java.lang.annotation.Annotation> annotationType, final IParameterValidator<METHOD_TYPE> validation,
			final IParameterSanitizer<METHOD_TYPE> sanitizer) {
		if (!preHandle(lockName, annotationType)) return;

		boolean isReadWriteLock = lockMethod != null;

		String annotationTypeName = annotationType.getSimpleName();
		String completeLockName = createCompleteLockName(lockName, isReadWriteLock);

		List<Statement<?>> beforeMethodBlock = new ArrayList<Statement<?>>();
		List<Statement<?>> afterMethodBlock = new ArrayList<Statement<?>>();

		if (!isReadWriteLock) {
			if (!getConditionStatements(await, completeLockName, annotationTypeName, beforeMethodBlock, afterMethodBlock)) return;
			if (!getConditionStatements(signal, completeLockName, annotationTypeName, beforeMethodBlock, afterMethodBlock)) return;
		}

		final Call lockCall;
		final Call unLockCall;
		if (isReadWriteLock) {
			lockCall = Call(Call(Field(completeLockName), lockMethod), "lock");
			unLockCall = Call(Call(Field(completeLockName), lockMethod), "unlock");
		} else {
			lockCall = Call(Field(completeLockName), "lock");
			unLockCall = Call(Field(completeLockName), "unlock");
		}

		method.editor().replaceBody(Block().posHint(method.get()) //
				.withStatements(validation.validateParameterOf(method)) //
				.withStatements(sanitizer.sanitizeParameterOf(method)) //
				.withStatement(lockCall) //
				.withStatement(Try(Block() //
						.withStatements(beforeMethodBlock) //
						.withStatements(method.statements()) //
						.withStatements(afterMethodBlock)//
						).Finally(Block() //
								.withStatement(unLockCall) //
						) //
				));

		method.editor().rebuild();
	}

	private String createCompleteLockName(final String lockName, final boolean isReadWriteLock) {
		String completeLockName = lockName;
		if ((!isReadWriteLock) && trim(lockName).isEmpty()) {
			String awaitCondition = trim(await == null ? "" : await.condition);
			String signalCondition = trim(signal == null ? "" : signal.condition);
			completeLockName = "$" + camelCase(awaitCondition, signalCondition, "lock");
		}
		return completeLockName;
	}

	private boolean getConditionStatements(final ConditionData condition, final String lockName, final String annotationTypeName, final List<Statement<?>> before,
			final List<Statement<?>> after) {
		if (condition == null) {
			return true;
		}
		if (tryToAddConditionField(condition, lockName, annotationTypeName)) {
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

	private boolean tryToAddLockField(final String lockName, final boolean isReadWriteLock, final String annotationTypeName) {
		String trimmedLockName = trim(lockName);
		if (trimmedLockName.isEmpty()) {
			diagnosticsReceiver.addError(String.format("@%s 'lockName' may not be empty or null.", annotationTypeName));
			return false;
		}
		if (!type.hasField(trimmedLockName)) {
			if (isReadWriteLock) {
				type.editor().injectField(FieldDecl(Type(ReadWriteLock.class), trimmedLockName).makePrivate().makeFinal() //
						.withInitialization(New(Type(ReentrantReadWriteLock.class))));
			} else {
				type.editor().injectField(FieldDecl(Type(Lock.class), trimmedLockName).makePrivate().makeFinal() //
						.withInitialization(New(Type(ReentrantLock.class))));
			}
		}
		return true;
	}

	private boolean tryToAddConditionField(final ConditionData condition, final String lockName, final String annotationTypeName) {
		if (condition == null) {
			return true;
		}
		String conditionName = trim(condition.condition);
		if (conditionName.isEmpty()) {
			diagnosticsReceiver.addError(String.format("@%s 'conditionName' may not be empty or null.", annotationTypeName));
			return false;
		}
		if (!type.hasField(conditionName)) {
			type.editor().injectField(FieldDecl(Type(Condition.class), conditionName).makePrivate().makeFinal() //
					.withInitialization(Call(Name(lockName), "newCondition")));
		}
		return true;
	}

	public static class AwaitData extends ConditionData {
		protected final String conditionMethod;

		public AwaitData(final String condition, final String conditionMethod, final Position pos) {
			super(condition, pos);
			this.conditionMethod = conditionMethod;
		}

		@Override
		public Statement<?> toStatement() {
			return Try(Block().withStatement(While(Call(This(), conditionMethod)).Do(Call(Field(condition), "await")))) //
					.Catch(Arg(Type(InterruptedException.class), "e"), Block().withStatement(Throw(New(Type(RuntimeException.class)).withArgument(Name("e")))));
		}
	}

	public static class SignalData extends ConditionData {
		public SignalData(final String condition, final Position pos) {
			super(condition, pos);
		}

		@Override
		public Statement<?> toStatement() {
			return Call(Field(condition), "signal");
		}
	}

	@RequiredArgsConstructor
	public abstract static class ConditionData {
		protected final String condition;
		protected final Position pos;

		public abstract Statement<?> toStatement();
	}
}
