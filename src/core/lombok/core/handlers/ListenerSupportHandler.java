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
import static lombok.core.util.Names.*;

import java.util.ArrayList;
import java.util.List;

import lombok.ast.*;

public abstract class ListenerSupportHandler<TYPE_TYPE extends IType<? extends IMethod<?, ?, ? , ?>, ?, ?, ?, ?, ?>> {

	public void addListenerField(final TYPE_TYPE type, final Object interfaze) {
		String interfaceName = interfaceName(name(interfaze));
		type.injectField(FieldDecl(Type("java.util.List").withTypeArgument(Type(type(interfaze))), "$registered" + interfaceName).makePrivate().makeFinal() //
			.withInitialization(New(Type("java.util.concurrent.CopyOnWriteArrayList").withTypeArgument(Type(type(interfaze))))));
	}

	public void addAddListenerMethod(final TYPE_TYPE type, final Object interfaze) {
		String interfaceName = interfaceName(name(interfaze));
		type.injectMethod(MethodDecl(Type("void"), "add" + interfaceName).makePublic().withArgument(Arg(Type(type(interfaze)), "l")) //
			.withStatement(If(Not(Call(Name("$registered" + interfaceName), "contains").withArgument(Name("l")))) //
				.Then(Call(Name("$registered" + interfaceName), "add").withArgument(Name("l")))));
	}

	public void addRemoveListenerMethod(final TYPE_TYPE type, final Object interfaze) {
		String interfaceName = interfaceName(name(interfaze));
		type.injectMethod(MethodDecl(Type("void"), "remove" + interfaceName).makePublic().withArgument(Arg(Type(type(interfaze)), "l")) //
			.withStatement(Call(Name("$registered" + interfaceName), "remove").withArgument(Name("l"))));
	}

	public void addFireListenerMethod(final TYPE_TYPE type, final Object interfaze, final Object method) {
		List<Expression> args = new ArrayList<Expression>();
		List<Argument> params = new ArrayList<Argument>();
		createParamsAndArgs(method, params, args);
		String interfaceName = interfaceName(name(interfaze));
		String methodName = name(method);
		type.injectMethod(MethodDecl(Type("void"), camelCase("fire", methodName)).makeProtected().withArguments(params) //
			.withStatement(Foreach(LocalDecl(Type(type(interfaze)), "l")).In(Name("$registered" + interfaceName)) //
				.Do(Call(Name("l"), methodName).withArguments(args))));
	}
	
	protected abstract void createParamsAndArgs(Object method, List<Argument> params, List<Expression> args);
	
	protected abstract String name(Object object);
	
	protected abstract Object type(Object object);
}
