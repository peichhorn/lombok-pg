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

import lombok.RequiredArgsConstructor;
import lombok.ast.*;
import lombok.core.DiagnosticsReceiver;

@RequiredArgsConstructor
public abstract class SwingInvokeHandler<METHOD_TYPE extends IMethod<?, ?, ?, ?>> {
	private final METHOD_TYPE method;
	private final DiagnosticsReceiver diagnosticsReceiver;
	
	public void generateSwingInvoke(String methodName, Class<? extends java.lang.annotation.Annotation> annotationType) {
		if (method == null) {
			diagnosticsReceiver.addError(canBeUsedOnMethodOnly(annotationType));
			return;
		}

		if (method.isAbstract() || method.isEmpty()) {
			diagnosticsReceiver.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return;
		}

		replaceWithQualifiedThisReference(method);

		String field = "$" + camelCase(method.name(), "runnable");

		Call elseStatementRun = Call(Name("java.awt.EventQueue"), methodName).withArgument(Name(field));

		Statement elseStatement;
		if ("invokeAndWait".equals(methodName)) {
			elseStatement =  Block().withStatement(generateTryCatchBlock(elseStatementRun, method));
		} else {
			elseStatement = Block().withStatement(elseStatementRun);
		}

		method.body(Block() //
			.withStatement(LocalDecl(Type("java.lang.Runnable"), field).makeFinal().withInitialization(New(Type("java.lang.Runnable")) //
				.withTypeDeclaration(ClassDecl("").makeAnonymous().makeLocal() //
					.withMethod(MethodDecl(Type("void"), "run").makePublic().withAnnotation(Annotation(Type("java.lang.Override"))) //
						.withStatements(method.statements()))))) //
			.withStatement(If(Call(Name("java.awt.EventQueue"), "isDispatchThread")) //
				.Then(Block().withStatement(Call(Name(field), "run"))) //
				.Else(elseStatement)));

		method.rebuild();
	}

	private Try generateTryCatchBlock(Call elseStatementRun, final METHOD_TYPE method) {
		return Try(Block() //
				.withStatement(elseStatementRun)) //
			.Catch(Arg(Type("java.lang.InterruptedException"), "$ex1"), Block()) //
			.Catch(Arg(Type("java.lang.reflect.InvocationTargetException"), "$ex2"), Block() //
				.withStatement(LocalDecl(Type("java.lang.Throwable"), "$cause").makeFinal().withInitialization(Call(Name("$ex2"), "getCause")))
				.withStatements(rethrowStatements(method)) //
				.withStatement(Throw(New(Type("java.lang.RuntimeException")).withArgument(Name("$cause")))));
	}

	private List<Statement> rethrowStatements(final METHOD_TYPE method) {
		final List<Statement> rethrowStatements = new ArrayList<Statement>();
		for (TypeRef thrownException : method.thrownExceptions()) {
			rethrowStatements.add(If(InstanceOf(Name("$cause"), thrownException)) //
				.Then(Throw(Cast(thrownException, Name("$cause")))));
		}
		return rethrowStatements;
	}

	protected abstract void replaceWithQualifiedThisReference(final METHOD_TYPE methode);
}
