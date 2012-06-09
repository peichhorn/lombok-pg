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

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.DiagnosticsReceiver;

@RequiredArgsConstructor
public final class SwingInvokeHandler<METHOD_TYPE extends IMethod<?, ?, ?, ?>> {
	private final METHOD_TYPE method;
	private final DiagnosticsReceiver diagnosticsReceiver;

	public void handle(final String methodName, final Class<? extends java.lang.annotation.Annotation> annotationType, final IParameterValidator<METHOD_TYPE> validation,
			final IParameterSanitizer<METHOD_TYPE> sanitizer) {
		if (method == null) {
			diagnosticsReceiver.addError(canBeUsedOnMethodOnly(annotationType));
			return;
		}

		if (method.isAbstract() || method.isEmpty()) {
			diagnosticsReceiver.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return;
		}

		method.forceQualifiedThis();

		String field = "$" + camelCase(method.name(), "runnable");

		Call elseStatementRun = Call(Name(EventQueue.class), methodName).withArgument(Name(field));

		Statement<?> elseStatement;
		if ("invokeAndWait".equals(methodName)) {
			elseStatement = Block().withStatement(generateTryCatchBlock(elseStatementRun, method));
		} else {
			elseStatement = Block().withStatement(elseStatementRun);
		}

		method.replaceBody(Block().posHint(method.get()) //
				.withStatements(validation.validateParameterOf(method)) //
				.withStatements(sanitizer.sanitizeParameterOf(method)) //
				.withStatement(LocalDecl(Type(Runnable.class), field).makeFinal().withInitialization(New(Type(Runnable.class)) //
						.withTypeDeclaration(ClassDecl("").makeAnonymous().makeLocal() //
								.withMethod(MethodDecl(Type("void"), "run").makePublic().withAnnotation(Annotation(Type(Override.class))) //
										.withStatements(method.statements()))))) //
				.withStatement(If(Call(Name(EventQueue.class), "isDispatchThread")) //
						.Then(Block().withStatement(Call(Name(field), "run"))) //
						.Else(elseStatement)));

		method.rebuild();
	}

	private Try generateTryCatchBlock(final Call elseStatementRun, final METHOD_TYPE method) {
		return Try(Block() //
				.withStatement(elseStatementRun)) //
				.Catch(Arg(Type(InterruptedException.class), "$ex1"), Block()) //
				.Catch(Arg(Type(InvocationTargetException.class), "$ex2"),Block() //
						.withStatement(LocalDecl(Type(Throwable.class), "$cause").makeFinal().withInitialization(Call(Name("$ex2"), "getCause")))
								.withStatements(rethrowStatements(method)) //
								.withStatement(Throw(New(Type(RuntimeException.class)).withArgument(Name("$cause")))));
	}

	private List<Statement<?>> rethrowStatements(final METHOD_TYPE method) {
		final List<Statement<?>> rethrowStatements = new ArrayList<Statement<?>>();
		for (TypeRef thrownException : method.thrownExceptions()) {
			rethrowStatements.add(If(InstanceOf(Name("$cause"), thrownException)) //
					.Then(Throw(Cast(thrownException, Name("$cause")))));
		}
		return rethrowStatements;
	}
}
