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

import static lombok.core.util.ErrorMessages.canBeUsedOnConcreteMethodOnly;
import static lombok.core.util.ErrorMessages.canBeUsedOnMethodOnly;
import static lombok.core.util.Names.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static lombok.javac.handlers.JavacTreeBuilder.*;

import java.lang.annotation.Annotation;

import lombok.Await;
import lombok.AwaitBeforeAndSignalAfter;
import lombok.Position;
import lombok.ReadLock;
import lombok.RequiredArgsConstructor;
import lombok.Signal;
import lombok.WriteLock;
import lombok.core.AnnotationValues;
import lombok.javac.handlers.JavacHandlerUtil.MemberExistsResult;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;

public class HandleConditionAndLock {
	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleReadLock extends JavacNonResolutionBasedHandler implements JavacAnnotationHandler<ReadLock> {
		@Override public boolean handle(AnnotationValues<ReadLock> annotation, JCAnnotation ast, JavacNode annotationNode) {
			ReadLock ann = annotation.getInstance();
			return new HandleConditionAndLock()
					.withLockMethod("readLock")
					.handle(ann.value(), ReadLock.class, ast, annotationNode);
		}
	}

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleWriteLock extends JavacNonResolutionBasedHandler implements JavacAnnotationHandler<WriteLock> {
		@Override public boolean handle(AnnotationValues<WriteLock> annotation, JCAnnotation ast, JavacNode annotationNode) {
			WriteLock ann = annotation.getInstance();
			return new HandleConditionAndLock()
					.withLockMethod("writeLock")
					.handle(ann.value(), WriteLock.class, ast, annotationNode);
		}
	}

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleSignal extends JavacNonResolutionBasedHandler implements JavacAnnotationHandler<Signal> {
		@Override public boolean handle(AnnotationValues<Signal> annotation, JCAnnotation ast, JavacNode annotationNode) {
			Signal ann = annotation.getInstance();
			return new HandleConditionAndLock()
					.withSignal(new SignalData(ann.value(), ann.pos()))
					.handle(ann.lockName(), Signal.class, ast, annotationNode);
		}
	}

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleAwait extends JavacNonResolutionBasedHandler implements JavacAnnotationHandler<Await> {
		@Override public boolean handle(AnnotationValues<Await> annotation, JCAnnotation ast, JavacNode annotationNode) {
			Await ann = annotation.getInstance();
			return new HandleConditionAndLock()
					.withAwait(new AwaitData(ann.value(), ann.conditionMethod(), ann.pos()))
					.handle(ann.lockName(), Await.class, ast, annotationNode);
		}
	}

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleAwaitBeforeAndSignalAfter extends JavacNonResolutionBasedHandler implements JavacAnnotationHandler<AwaitBeforeAndSignalAfter> {
		@Override public boolean handle(AnnotationValues<AwaitBeforeAndSignalAfter> annotation, JCAnnotation ast, JavacNode annotationNode) {
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

	public boolean handle(String lockName, Class<? extends Annotation> annotationType, JCAnnotation ast, JavacNode annotationNode) {
		markAnnotationAsProcessed(annotationNode, annotationType);
		JavacMethod method = JavacMethod.methodOf(annotationNode);
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
			return false; // wrong configured handler, so better stop here
		}
		
		String completeLockName = createCompleteLockName(lockName);

		if (!tryToAddLockField(lockMode ? lockName : completeLockName, lockMode, annotationTypeName, annotationNode)) {
			return false;
		}

		StringBuilder beforeMethodBlock = new StringBuilder();
		StringBuilder afterMethodBlock = new StringBuilder();

		if (!lockMode) {
			if (!getConditionStatements(await, completeLockName, annotationTypeName, annotationNode, beforeMethodBlock, afterMethodBlock)) {
				return false;
			}
			if (!getConditionStatements(signal, completeLockName, annotationTypeName, annotationNode, beforeMethodBlock, afterMethodBlock)) {
				return false;
			}
		}

		TreeMaker maker = method.node().getTreeMaker();
		method.body(statements(method.node(), "this.%s.lock(); try { %s %s %s } finally { this.%s.unlock(); }",
				completeLockName, beforeMethodBlock, removeCurlyBrackets(method.get().body.toString()), afterMethodBlock, completeLockName));
		if (await != null) {
			method.get().thrown = method.get().thrown.append(chainDotsString(maker, method.node(), "java.lang.InterruptedException"));
		}

		method.rebuild();

		return true;
	}

	private boolean getConditionStatements(ConditionData condition, String lockName, String annotationTypeName, JavacNode node, StringBuilder before, StringBuilder after) {
		if (condition == null) {
			return true;
		}
		if (tryToAddConditionField(condition.condition, lockName, annotationTypeName, node)) {
			switch (condition.pos) {
			case BEFORE:
				before.append(condition);
				break;
			default:
			case AFTER:
				after.append(condition);
				break;
			}
			return true;
		}
		return false;
	}

	private String createCompleteLockName(String lockName) {
		String completeLockName;
		if (lockMethod != null) {
			completeLockName = lockName + "." + lockMethod + "()";
		} else {
			if (trim(lockName).isEmpty()) {
				String awaitCondition = trim(await == null ? "" : await.condition);
				String signalCondition = trim(signal == null ? "" : signal.condition);
				completeLockName = "$" + camelCase(awaitCondition, signalCondition, "lock");
			} else {
				completeLockName = lockName;
			}
		}
		return completeLockName;
	}

	private static boolean tryToAddLockField(String lockName, boolean isReadWriteLock, String annotationTypeName, JavacNode annotationNode) {
		lockName = trim(lockName);
		if (lockName.isEmpty()) {
			annotationNode.addError(String.format("@%s 'lockName' may not be empty or null.", annotationTypeName));
			return false;
		}
		JavacNode methodNode = annotationNode.up();
		if (fieldExists(lockName, methodNode) == MemberExistsResult.NOT_EXISTS) {
			if(isReadWriteLock) {
				addReadWriteLockField(methodNode, lockName);
			} else {
				addLockField(methodNode, lockName);
			}
		} else {
			// TODO type check
			// java.util.concurrent.locks.ReadWriteLock
			// java.util.concurrent.locks.Lock
		}
		return true;
	}

	private static boolean tryToAddConditionField(String conditionName, String lockName, String annotationTypeName, JavacNode annotationNode) {
		conditionName = trim(conditionName);
		if (conditionName.isEmpty()) {
			annotationNode.addError(String.format("@%s 'conditionName' may not be empty or null.", annotationTypeName));
			return false;
		}
		JavacNode methodNode = annotationNode.up();
		if (fieldExists(conditionName, methodNode) == MemberExistsResult.NOT_EXISTS) {
			addConditionField(methodNode, conditionName, lockName);
		} else {
			// TODO type check
			// java.util.concurrent.locks.Condition
		}
		return true;
	}
	
	private static void addLockField(JavacNode node, String lockName) {
		field(node.up(), "private final java.util.concurrent.locks.Lock %s = new java.util.concurrent.locks.ReentrantLock();", lockName).inject();
	}

	private static void addReadWriteLockField(JavacNode node, String lockName) {
		field(node.up(), "private final java.util.concurrent.locks.ReadWriteLock %s = new java.util.concurrent.locks.ReentrantReadWriteLock();", lockName).inject();
	}

	private static void addConditionField(JavacNode node, String conditionName, String lockName) {
		field(node.up(), "private final java.util.concurrent.locks.Condition %s = %s.newCondition();", conditionName, lockName).inject();
	}

	private static class AwaitData extends ConditionData {
		public final String conditionMethod;

		public AwaitData(final String condition, final String conditionMethod, final Position pos) {
			super(condition, pos);
			this.conditionMethod = conditionMethod;
		}

		@Override
		public String toString() {
			return String.format("while(this.%s()) { this.%s.await();}", conditionMethod, condition);
		}
	}

	private static class SignalData extends ConditionData {
		public SignalData(final String condition, final Position pos) {
			super(condition, pos);
		}

		@Override
		public String toString() {
			return String.format("this.%s.signal();", condition);
		}
	}

	@RequiredArgsConstructor
	private static abstract class ConditionData {
		public final String condition;
		public final Position pos;
	}
}
