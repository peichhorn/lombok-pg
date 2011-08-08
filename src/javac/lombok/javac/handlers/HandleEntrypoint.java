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

import static lombok.javac.handlers.JavacHandlerUtil.*;
import static lombok.javac.handlers.Javac.*;

import lombok.*;
import lombok.core.handlers.EntrypointHandler;
import lombok.core.handlers.EntrypointHandler.*;
import lombok.javac.JavacASTAdapter;
import lombok.javac.JavacASTVisitor;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacMethod;
import lombok.javac.handlers.ast.JavacType;

import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import org.mangosdk.spi.ProviderFor;

public class HandleEntrypoint {
	/**
	 * Handles the {@code lombok.Application} interface for javac.
	 */
	@ProviderFor(JavacASTVisitor.class)
	public static class HandleApplication extends AbstractHandleEntrypoint {
		public HandleApplication() {
			super(Application.class);
		}

		@Override protected void handle(final JavacType type) {
			markInterfaceAsProcessed(type.node(), Application.class);
			new EntrypointHandler<JavacType, JavacMethod>().createEntrypoint(type, "main", "runApp", new ApplicationParameterProvider(), new ApplicationArgumentProvider());
		}
	}

	/**
	 * Handles the {@code lombok.JvmAgent} interface for javac.
	 */
	@ProviderFor(JavacASTVisitor.class)
	public static class HandleJvmAgent extends AbstractHandleEntrypoint {
		public HandleJvmAgent() {
			super(JvmAgent.class);
		}

		@Override protected void handle(final JavacType type) {
			markInterfaceAsProcessed(type.node(), JvmAgent.class);
			IArgumentProvider argumentProvider = new JvmAgentArgumentProvider();
			IParameterProvider parameterProvider = new JvmAgentParameterProvider();
			new EntrypointHandler<JavacType, JavacMethod>().createEntrypoint(type, "agentmain", "runAgent", parameterProvider, argumentProvider);
			new EntrypointHandler<JavacType, JavacMethod>().createEntrypoint(type, "premain", "runAgent", parameterProvider, argumentProvider);
		}
	}

	@RequiredArgsConstructor
	public abstract static class AbstractHandleEntrypoint extends JavacASTAdapter {
		private final Class<?> interfaze;

		@Override public void visitType(final JavacNode typeNode, final JCClassDecl type) {
			boolean implementsInterface = false;
			boolean isAnImport = typeNode.getImportStatements().contains(interfaze.getName());
			if (type.getImplementsClause() != null) for (JCExpression exp : type.getImplementsClause()) {
				if (exp.toString().equals(interfaze.getName()) || (isAnImport && exp.toString().equals(interfaze.getSimpleName()))) {
					implementsInterface = true;
					break;
				}
			}
			if (implementsInterface) {
				 handle(JavacType.typeOf(typeNode, type));
			}
		}

		@Override public void endVisitCompilationUnit(final JavacNode top, final JCCompilationUnit unit) {
			deleteImportFromCompilationUnit(top, interfaze.getName());
		}

		/**
		 * Called when an interface is found that is likely to match the interface you're interested in.
		 *
		 * @param type
		 */
		protected abstract void handle(JavacType type);
	}
}
