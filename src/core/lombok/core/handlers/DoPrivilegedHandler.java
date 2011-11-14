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

import java.security.*;
import java.util.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.DiagnosticsReceiver;

@RequiredArgsConstructor
public class DoPrivilegedHandler<METHOD_TYPE extends IMethod<?, ?, ?, ?>> {
	private final METHOD_TYPE method;
	private final DiagnosticsReceiver diagnosticsReceiver;

	public void handle(final Class<? extends java.lang.annotation.Annotation> annotationType, final IParameterValidator<METHOD_TYPE> validation,
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

		final TypeRef innerReturnType = method.boxedReturns();
		if (method.returns("void")) {
			method.replaceReturns(Return(Null()).posHint(method.get()));
			method.replaceBody(Block().posHint(method.get()) //
				.withStatements(validation.validateParameterOf(method)) //
				.withStatements(sanitizer.sanitizeParameterOf(method)) //
				.withStatement(Try(Block() //
					.withStatement(Call(Name(AccessController.class), "doPrivileged").withArgument( //
						New(Type(PrivilegedExceptionAction.class).withTypeArgument(innerReturnType)).withTypeDeclaration(ClassDecl("").makeAnonymous().makeLocal() //
						.withMethod(MethodDecl(innerReturnType, "run").makePublic().withThrownExceptions(method.thrownExceptions()) //
							.withStatements(method.statements()) //
							.withStatement(Return(Null()))))))) //
				.Catch(Arg(Type(PrivilegedActionException.class), "$ex"), Block() //
					.withStatement(LocalDecl(Type(Throwable.class), "$cause").makeFinal().withInitialization(Call(Name("$ex"), "getCause"))) //
					.withStatements(rethrowStatements(method)) //
					.withStatement(Throw(New(Type(RuntimeException.class)).withArgument(Name("$cause")))))));
		} else {
			method.replaceBody(Block().posHint(method.get()) //
				.withStatements(validation.validateParameterOf(method)) //
				.withStatements(sanitizer.sanitizeParameterOf(method)) //
				.withStatement(Try(Block() //
					.withStatement(Return(Call(Name(AccessController.class), "doPrivileged").withArgument( //
						New(Type(PrivilegedExceptionAction.class).withTypeArgument(innerReturnType)).withTypeDeclaration(ClassDecl("").makeAnonymous().makeLocal() //
						.withMethod(MethodDecl(innerReturnType, "run").makePublic().withThrownExceptions(method.thrownExceptions()) //
							.withStatements(method.statements()))))))) //
				.Catch(Arg(Type(PrivilegedActionException.class), "$ex"), Block() //
					.withStatement(LocalDecl(Type(Throwable.class), "$cause").makeFinal().withInitialization(Call(Name("$ex"), "getCause"))) //
					.withStatements(rethrowStatements(method)) //
					.withStatement(Throw(New(Type(RuntimeException.class)).withArgument(Name("$cause")))))));
		}

		method.rebuild();
	}

	private List<Statement<?>> rethrowStatements(final METHOD_TYPE method) {
		final List<Statement<?>> rethrowStatements = new ArrayList<Statement<?>>();
		for (lombok.ast.TypeRef thrownException : method.thrownExceptions()) {
			rethrowStatements.add(If(InstanceOf(Name("$cause"), thrownException)) //
				.Then(Throw(Cast(thrownException, Name("$cause")))));
		}
		return rethrowStatements;
	}
}
