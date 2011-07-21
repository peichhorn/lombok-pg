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

import java.util.ArrayList;
import java.util.List;

import lombok.ast.*;

public final class EntrypointHandler<TYPE_TYPE extends IType<METHOD_TYPE, ?, ?, ?, ?>, METHOD_TYPE extends IMethod<TYPE_TYPE, ?, ?, ?>> {

	/**
	 * Checks if there is an entry point with the provided name.
	 *
	 * @param methodName the entry point name to check for.
	 * @param node Any node that represents the Type to look in, or any child node thereof.
	 */
	public boolean entrypointExists(String methodName, TYPE_TYPE type) {
		for (METHOD_TYPE method : type.methods()) {
			if (method.isStatic() && method.returns("void") && method.name().equals(methodName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates an entrypoint like this:
	 * <pre>
	 * public static void &lt;NAME&gt;(&lt;PARAMETER&gt;) throws java.lang.Throwable {
	 *   new &lt;TYPENAME&gt;().&lt;METHODNAME&gt;(&lt;ARGUMENTS&gt;);
	 * }
	 * </pre>
	 * @param type Type
	 * @param name name of the entrypoint ("main", "premain, "agentmain")
	 * @param methodName name of method that should be called in the entrypoint
	 * @param paramProvider parameter provider used for the entrypoint
	 * @param argsProvider argument provider used for the constructor
	 */
	public void createEntrypoint(TYPE_TYPE type, String name, String methodName, IParameterProvider paramProvider, IArgumentProvider argsProvider) {
		if (!type.hasMethod(methodName)) {
			return;
		}

		if (entrypointExists(name, type)) {
			return;
		}

		type.injectMethod(MethodDecl(Type("void"), name).makePublic().makeStatic().withArguments(paramProvider.getParams(name)).withThrownException(Type("java.lang.Throwable")) //
				.withStatement(Call(New(Type(type.name())), methodName).withArguments(argsProvider.getArgs(name))));
	}

	public static interface IArgumentProvider {
		public List<Expression> getArgs(String name);
	}

	public static interface IParameterProvider {
		public List<Argument> getParams(String name);
	}
	
	public static class ApplicationArgumentProvider implements IArgumentProvider {
		@Override public List<Expression> getArgs(String name) {
			List<Expression> args = new ArrayList<Expression>();
			args.add(Name("args"));
			return args;
		}
	}

	public static class ApplicationParameterProvider implements IParameterProvider {
		@Override public List<Argument> getParams(String name) {
			List<Argument> params = new ArrayList<Argument>();
			params.add(Arg(Type("java.lang.String").withDimensions(1), "args"));
			return params;
		}
	}
	
	public static class JvmAgentArgumentProvider implements IArgumentProvider {
		@Override public List<lombok.ast.Expression> getArgs(String name) {
			List<lombok.ast.Expression> args = new ArrayList<lombok.ast.Expression>();
			args.add(("agentmain".equals(name) ? True() : False()));
			args.add(Name("params"));
			args.add(Name("instrumentation"));
			return args;
		}
	}

	public static class JvmAgentParameterProvider implements IParameterProvider {
		@Override public List<lombok.ast.Argument> getParams(String name) {
			List<lombok.ast.Argument> params = new ArrayList<lombok.ast.Argument>();
			params.add(Arg(Type("java.lang.String"), "params"));
			params.add(Arg(Type("java.lang.instrument.Instrumentation"), "instrumentation"));
			return params;
		}
	}
}
