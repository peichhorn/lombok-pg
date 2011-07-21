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

import lombok.*;
import lombok.core.handlers.EntrypointHandler;
import lombok.core.handlers.EntrypointHandler.*;
import lombok.eclipse.EclipseASTAdapter;
import lombok.eclipse.EclipseASTVisitor;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseMethod;
import lombok.eclipse.handlers.ast.EclipseType;

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
			new EntrypointHandler<EclipseType, EclipseMethod>().createEntrypoint(type, "main", "runApp", new ApplicationParameterProvider(), new ApplicationArgumentProvider());
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
			IArgumentProvider argumentProvider = new JvmAgentArgumentProvider();
			IParameterProvider parameterProvider = new JvmAgentParameterProvider();
			new EntrypointHandler<EclipseType, EclipseMethod>().createEntrypoint(type, "agentmain", "runAgent", parameterProvider, argumentProvider);
			new EntrypointHandler<EclipseType, EclipseMethod>().createEntrypoint(type, "premain", "runAgent", parameterProvider, argumentProvider);
		}
	}

	@RequiredArgsConstructor
	public static abstract class EclipseEntrypointHandler extends EclipseASTAdapter {
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
		 * @param type
		 */
		protected abstract void handle(EclipseType type);

		@Override
		public boolean deferUntilPostDiet() {
			return false;
		}
	}
}
