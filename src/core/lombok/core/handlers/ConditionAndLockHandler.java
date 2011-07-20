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
package lombok.core.handlers;

import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;

import java.util.ArrayList;
import java.util.List;

import lombok.*;
import lombok.ast.*;
import lombok.core.DiagnosticsReceiver;

public final class ConditionAndLockHandler {
	private AwaitData await;
	private SignalData signal;
	private String lockMethod;
	private DiagnosticsReceiver diagnosticsReceiver;
	private IType<?, ?, ?, ?, ?> type;
	private IMethod<?, ?, ?, ?> method;
	
	public ConditionAndLockHandler withAwait(final AwaitData await) {
		this.await = await;
		return this;
	}

	public ConditionAndLockHandler withSignal(final SignalData signal) {
		this.signal = signal;
		return this;
	}
	
	public ConditionAndLockHandler withLockMethod(final String lockMethod) {
		this.lockMethod = lockMethod;
		return this;
	}
	
	public ConditionAndLockHandler withDiagnosticsReceiver(final DiagnosticsReceiver diagnosticsReceiver) {
		this.diagnosticsReceiver = diagnosticsReceiver;
		return this;
	}
	
	public ConditionAndLockHandler withTypeAndMethod(final IType<?, ?, ?, ?, ?> type, final IMethod<?, ?, ?, ?> method) {
		this.type = type;
		this.method = method;
		return this;
	}
	
	public boolean preHandle(String lockName, Class<? extends java.lang.annotation.Annotation> annotationType) {
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

	public void handle(String lockName, Class<? extends java.lang.annotation.Annotation> annotationType) {
		if (!preHandle(lockName, annotationType)) return;

		boolean isReadWriteLock = lockMethod != null;

		String annotationTypeName = annotationType.getSimpleName();
		String completeLockName = createCompleteLockName(lockName, isReadWriteLock);

		List<Statement> beforeMethodBlock = new ArrayList<Statement>();
		List<Statement> afterMethodBlock = new ArrayList<Statement>();

		if (!isReadWriteLock) {
			if (!getConditionStatements(await, completeLockName, annotationTypeName, beforeMethodBlock, afterMethodBlock)) return;
			if (!getConditionStatements(signal, completeLockName, annotationTypeName, beforeMethodBlock, afterMethodBlock)) return;
		}

		final Call lockCall;
		final Call unLockCall;
		if (isReadWriteLock) {
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

		method.rebuild();
	}

	private String createCompleteLockName(String lockName, boolean isReadWriteLock) {
		String completeLockName = lockName;
		if ((!isReadWriteLock) && trim(lockName).isEmpty()) {
			String awaitCondition = trim(await == null ? "" : await.condition);
			String signalCondition = trim(signal == null ? "" : signal.condition);
			completeLockName = "$" + camelCase(awaitCondition, signalCondition, "lock");
		}
		return completeLockName;
	}
	
	private boolean getConditionStatements(ConditionData condition, String lockName, String annotationTypeName, List<Statement> before, List<Statement> after) {
		if (condition == null) {
			return true;
		}
		if (tryToAddConditionField( condition, lockName, annotationTypeName)) {
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

	private boolean tryToAddLockField(String lockName, boolean isReadWriteLock, String annotationTypeName) {
		lockName = trim(lockName);
		if (lockName.isEmpty()) {
			diagnosticsReceiver.addError(String.format("@%s 'lockName' may not be empty or null.", annotationTypeName));
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

	private boolean tryToAddConditionField(ConditionData condition, String lockName, String annotationTypeName) {
		if (condition == null) {
			return true;
		}
		String conditionName = trim(condition.condition);
		if (conditionName.isEmpty()) {
			diagnosticsReceiver.addError(String.format("@%s 'conditionName' may not be empty or null.", annotationTypeName));
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

	public static class AwaitData extends ConditionData {
		public final String conditionMethod;

		public AwaitData(final String condition, final String conditionMethod, final Position pos) {
			super(condition, pos);
			this.conditionMethod = conditionMethod;
		}

		@Override
		public Statement toStatement() {
			return Try(Block().withStatement(While(Call(This(), conditionMethod)).Do(Call(Field(This(), condition), "await")))) //
				.Catch(Arg(Type("java.lang.InterruptedException"), "e"), Block().withStatement(Throw(New(Type("java.lang.RuntimeException")).withArgument(Name("e")))));
		}
	}

	public static class SignalData extends ConditionData {
		public SignalData(final String condition, final Position pos) {
			super(condition, pos);
		}

		@Override
		public Statement toStatement() {
			return Call(Field(This(), condition), "signal");
		}
	}

	@RequiredArgsConstructor
	public static abstract class ConditionData {
		public final String condition;
		public final Position pos;

		public abstract Statement toStatement();
	}
}
