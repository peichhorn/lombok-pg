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

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import lombok.ast.*;

public final class EntrypointHandler<TYPE_TYPE extends IType<METHOD_TYPE, ?, ?, ?, ?, ?>, METHOD_TYPE extends IMethod<TYPE_TYPE, ?, ?, ?>> {

	/**
	 * Checks if there is an entry point with the provided name.
	 * 
	 * @param methodName
	 *            the entry point name to check for.
	 * @param node
	 *            Any node that represents the Type to look in, or any child node thereof.
	 */
	public boolean entrypointExists(final String methodName, final TYPE_TYPE type) {
		for (METHOD_TYPE method : type.methods()) {
			if (method.isStatic() && method.returns("void") && method.name().equals(methodName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates an entrypoint like this:
	 * 
	 * <pre>
	 * public static void &lt;NAME&gt;(&lt;PARAMETER&gt;) throws java.lang.Throwable {
	 *   new &lt;TYPENAME&gt;().&lt;METHODNAME&gt;(&lt;ARGUMENTS&gt;);
	 * }
	 * </pre>
	 * 
	 * @param type
	 *            Type
	 * @param name
	 *            name of the entrypoint ("main", "premain, "agentmain")
	 * @param methodName
	 *            name of method that should be called in the entrypoint
	 * @param paramProvider
	 *            parameter provider used for the entrypoint
	 * @param argsProvider
	 *            argument provider used for the constructor
	 */
	public void createEntrypoint(final TYPE_TYPE type, final String name, final String methodName, final Parameters params, final Arguments args) {
		if (entrypointExists(name, type)) {
			return;
		}

		type.editor().injectMethod(MethodDecl(Type("void"), name).makePublic().makeStatic().withArguments(params.get(name)).withThrownException(Type("java.lang.Throwable")) //
				.withStatement(Call(New(Type(type.name())), methodName).withArguments(args.get(name))));
	}

	public enum Arguments {
		APPLICATION {
			@Override
			public List<Expression<?>> get(final String name) {
				List<Expression<?>> args = new ArrayList<Expression<?>>();
				args.add(Name("args"));
				return args;
			}
		},
		JVM_AGENT {
			@Override
			public List<Expression<?>> get(final String name) {
				List<Expression<?>> args = new ArrayList<Expression<?>>();
				args.add(("agentmain".equals(name) ? True() : False()));
				args.add(Name("params"));
				args.add(Name("instrumentation"));
				return args;
			}
		};

		public abstract List<Expression<?>> get(String name);
	}

	public enum Parameters {
		APPLICATION {
			@Override
			public List<Argument> get(final String name) {
				List<Argument> params = new ArrayList<Argument>();
				params.add(Arg(Type(String.class).withDimensions(1), "args"));
				return params;
			}
		},
		JVM_AGENT {
			@Override
			public List<Argument> get(final String name) {
				List<Argument> params = new ArrayList<Argument>();
				params.add(Arg(Type(String.class), "params"));
				params.add(Arg(Type(Instrumentation.class), "instrumentation"));
				return params;
			}
		};

		public abstract List<Argument> get(String name);
	}
}
