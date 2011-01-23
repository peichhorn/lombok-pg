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
import static lombok.eclipse.handlers.EclipseHandlerUtil.methodExists;
import static lombok.eclipse.handlers.EclipseHandlerUtil.MemberExistsResult.NOT_EXISTS;
import static lombok.eclipse.handlers.EclipseNodeBuilder.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Application;
import lombok.JvmAgent;
import lombok.eclipse.EclipseASTAdapter;
import lombok.eclipse.EclipseASTVisitor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
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

		@Override protected void handle(EclipseNode typeNode, TypeDeclaration type) {
			createEntrypoint(typeNode, type, "main", "runApp", new ParameterProvider(), new ArgumentProvider());
		}

		private static class ArgumentProvider implements IArgumentProvider {
			@Override public List<? extends Expression> getArgs(ASTNode source, String name) {
				return Arrays.asList(nameReference(source, "args"));
			}
		}
		
		private static class ParameterProvider implements IParameterProvider {
			@Override public List<Argument> getParams(ASTNode source, String name) {
				return Arrays.asList(argument(source, "java.lang.String[]", "args"));
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

		@Override protected void handle(EclipseNode typeNode, TypeDeclaration type) {
			IArgumentProvider argumentProvider = new ArgumentProvider();
			IParameterProvider parameterProvider = new ParameterProvider();
			createEntrypoint(typeNode, type, "agentmain", "runAgent", parameterProvider, argumentProvider);
			createEntrypoint(typeNode, type, "premain", "runAgent", parameterProvider, argumentProvider);
		}

		private static class ArgumentProvider implements IArgumentProvider {
			@Override public List<? extends Expression> getArgs(ASTNode source, String name) {
				List<Expression> argsRef = new ArrayList<Expression>();
				argsRef.add(booleanLiteral(source, "agentmain".equals(name)));
				argsRef.add(nameReference(source, "params"));
				argsRef.add(nameReference(source, "instrumentation"));
				return argsRef;
			}
		}

		private static class ParameterProvider implements IParameterProvider {
			@Override public List<Argument> getParams(ASTNode source, String name) {
				List<Argument> params = new ArrayList<Argument>();
				params.add(argument(source, "java.lang.String", "params"));
				params.add(argument(source, "java.lang.instrument.Instrumentation", "instrumentation"));
				return params;
			}
		}
	}
	
	private static  abstract class EclipseEntrypointHandler extends EclipseASTAdapter {
		private final Class<?> interfaze;

		public EclipseEntrypointHandler(Class<?> interfaze) {
			this.interfaze = interfaze;
		}

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
				handle(typeNode, type);
			}
		}

		/**
		 * Called when an interface is found that is likely to match the interface you're interested in.
		 * 
		 * @param typeNode
		 * @param type
		 */
		protected abstract void handle(EclipseNode typeNode, TypeDeclaration type);
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
	public static void createEntrypoint(EclipseNode node, ASTNode source, String name, String methodName, IParameterProvider paramProvider, IArgumentProvider argsProvider) {
		if (methodExists(methodName, node, false) == NOT_EXISTS) {
			node.addWarning(String.format("The method '%s' is missing, not generating entrypoint 'public static void %s'.", methodName, name));
			return;
		}
		
		if (entrypointExists(name, node)) {
			return;
		}
		
		AllocationExpression newClassExp = new AllocationExpression();
		setGeneratedByAndCopyPos(newClassExp, source);
		newClassExp.type = typeReference(source, node.getName());
		
		List<? extends Expression> arguments = (argsProvider != null) ? argsProvider.getArgs(source, name) : new ArrayList<Expression>();
		MessageSend callMethod = methodCall(source, newClassExp, methodName, arguments.toArray(new Expression[arguments.size()]));
		List<Argument> parameters = new ArrayList<Argument>();
		if (paramProvider != null) {
			parameters.addAll(paramProvider.getParams(source, name));
		}
		method(node, source, PUBLIC | STATIC, "void", name).withThrownException("java.lang.Throwable").withParameters(parameters).withStatement(callMethod).inject();
	}
	
	public static interface IArgumentProvider {
		public List<? extends Expression> getArgs(ASTNode source, String name);
	}
	
	public static interface IParameterProvider {
		public List<Argument> getParams(ASTNode source, String name);
	}
}
