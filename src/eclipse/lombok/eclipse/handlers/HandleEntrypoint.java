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

import static lombok.core.util.Arrays.*;
import static lombok.eclipse.handlers.Eclipse.*;
import static org.eclipse.jdt.core.dom.Modifier.*;
import static lombok.ast.AST.*;

import java.util.ArrayList;
import java.util.List;

import lombok.Application;
import lombok.JvmAgent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseASTAdapter;
import lombok.eclipse.EclipseASTVisitor;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.mangosdk.spi.ProviderFor;

public class HandleEntrypoint {
	/**
	 * Handles the {@code lombok.Application} interface for eclipse.
	 */
	@ProviderFor(EclipseASTVisitor.class)
	public static class HandleApplication extends EclipseEntrypointHandler {
		public HandleApplication() {
			super(Application.class);
		}

		@Override protected void handle(EclipseType type) {
			createEntrypoint(type, "main", "runApp", new ParameterProvider(), new ArgumentProvider());
		}

		private static class ArgumentProvider implements IArgumentProvider {
			@Override public List<lombok.ast.Expression> getArgs(String name) {
				List<lombok.ast.Expression> args = new ArrayList<lombok.ast.Expression>();
				args.add(Name("args"));
				return args;
			}
		}

		private static class ParameterProvider implements IParameterProvider {
			@Override public List<lombok.ast.Argument> getParams(String name) {
				List<lombok.ast.Argument> params = new ArrayList<lombok.ast.Argument>();
				params.add(Arg(Type("java.lang.String").withDimensions(1), "args"));
				return params;
			}
		}
	}

	/**
	 * Handles the {@code lombok.JvmAgent} interface for eclipse.
	 */
	@ProviderFor(EclipseASTVisitor.class)
	public static class HandleJvmAgent extends EclipseEntrypointHandler {
		public HandleJvmAgent() {
			super(JvmAgent.class);
		}

		@Override protected void handle(EclipseType type) {
			IArgumentProvider argumentProvider = new ArgumentProvider();
			IParameterProvider parameterProvider = new ParameterProvider();
			createEntrypoint(type, "agentmain", "runAgent", parameterProvider, argumentProvider);
			createEntrypoint(type, "premain", "runAgent", parameterProvider, argumentProvider);
		}

		private static class ArgumentProvider implements IArgumentProvider {
			@Override public List<lombok.ast.Expression> getArgs(String name) {
				List<lombok.ast.Expression> args = new ArrayList<lombok.ast.Expression>();
				args.add(("agentmain".equals(name) ? True() : False()));
				args.add(Name("params"));
				args.add(Name("instrumentation"));
				return args;
			}
		}

		private static class ParameterProvider implements IParameterProvider {
			@Override public List<lombok.ast.Argument> getParams(String name) {
				List<lombok.ast.Argument> params = new ArrayList<lombok.ast.Argument>();
				params.add(Arg(Type("java.lang.String"), "params"));
				params.add(Arg(Type("java.lang.instrument.Instrumentation"), "instrumentation"));
				return params;
			}
		}
	}

	@RequiredArgsConstructor
	private static  abstract class EclipseEntrypointHandler extends EclipseASTAdapter {
		@NonNull
		private final Class<?> interfaze;

		@Override public void visitType(EclipseNode typeNode, TypeDeclaration type) {
			boolean implementsInterface = false;
			boolean isAnImport = typeNode.getImportStatements().contains(interfaze.getName());
			if (isNotEmpty(type.superInterfaces)) for (TypeReference ref : type.superInterfaces) {
				if (ref.toString().equals(interfaze.getName()) || (isAnImport && ref.toString().equals(interfaze.getSimpleName()))) {
					implementsInterface = true;
					break;
				}
			}
			if (implementsInterface) {
				handle(EclipseType.typeOf(typeNode, type));
			}
		}

		/**
		 * Called when an interface is found that is likely to match the interface you're interested in.
		 *
		 * @param typeNode
		 * @param type
		 */
		protected abstract void handle(EclipseType type);

		@Override
		public boolean deferUntilPostDiet() {
			return false;
		}
	}


	/**
	 * Checks if there is an entry point with the provided name.
	 *
	 * @param methodName the entry point name to check for.
	 * @param node Any node that represents the Type (TypeDeclaration) to look in, or any child node thereof.
	 */
	public static boolean entrypointExists(String methodName, EclipseNode node) {
		EclipseNode typeNode = typeNodeOf(node);
		TypeDeclaration typeDecl = (TypeDeclaration)typeNode.get();
		if (isNotEmpty(typeDecl.methods)) for (AbstractMethodDeclaration def : typeDecl.methods) {
			if (def instanceof MethodDeclaration) {
				char[] mName = def.selector;
				if (mName == null) continue;
				boolean nameEquals = methodName.equals(new String(mName));
				boolean returnTypeVoid = "void".equals(((MethodDeclaration)def).returnType.toString());
				boolean publicStatic = ((def.modifiers & (PUBLIC | STATIC)) != 0);
				if (nameEquals && returnTypeVoid && publicStatic) {
					return true;
				}
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
	 * @param node Any node that represents the Type (TypeDeclaration)
	 * @param source
	 * @param name name of the entrypoint ("main", "premain, "agentmain")
	 * @param methodName name of method that should be called in the entrypoint
	 * @param paramProvider parameter provider used for the entrypoint
	 * @param argsProvider argument provider used for the constructor
	 */
	public static void createEntrypoint(EclipseType type, String name, String methodName, @NonNull IParameterProvider paramProvider, @NonNull IArgumentProvider argsProvider) {
		if (!type.hasMethod(methodName)) {
			return;
		}

		if (entrypointExists(name, type.node())) {
			return;
		}

		type.injectMethod(MethodDecl(Type("void"), name).makePublic().makeStatic().withArguments(paramProvider.getParams(name)).withThrownException(Type("java.lang.Throwable")) //
				.withStatement(Call(New(Type(type.name())), methodName).withArguments(argsProvider.getArgs(name))));
	}

	public static interface IArgumentProvider {
		public List<lombok.ast.Expression> getArgs(String name);
	}

	public static interface IParameterProvider {
		public List<lombok.ast.Argument> getParams(String name);
	}
}
