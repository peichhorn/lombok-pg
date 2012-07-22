/*
 * Copyright © 2010-2012 Philipp Eichhorn
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

import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.eclipse.handlers.Eclipse.ensureAllClassScopeMethodWereBuild;

import java.util.*;

import lombok.*;
import lombok.ast.Argument;
import lombok.ast.Expression;
import lombok.core.AnnotationValues;
import lombok.core.handlers.ListenerSupportHandler;
import lombok.core.util.As;
import lombok.core.util.Each;
import lombok.eclipse.DeferUntilBuildFieldsAndMethods;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@link ListenerSupport} annotation for eclipse using the {@link PatchListenerSupport}.
 */
@ProviderFor(EclipseAnnotationHandler.class)
@DeferUntilBuildFieldsAndMethods
public class HandleListenerSupport extends EclipseAnnotationHandler<ListenerSupport> {
	private final EclipseListenerSupportHandler handler = new EclipseListenerSupportHandler();

	@Override
	public void handle(final AnnotationValues<ListenerSupport> annotation, final Annotation source, final EclipseNode annotationNode) {
		EclipseType type = EclipseType.typeOf(annotationNode, source);
		if (type.isAnnotation() || type.isInterface()) {
			annotationNode.addError(canBeUsedOnClassAndEnumOnly(ListenerSupport.class));
			return;
		}

		List<Object> listenerInterfaces = annotation.getActualExpressions("value");
		if (listenerInterfaces.isEmpty()) {
			annotationNode.addError(String.format("@%s has no effect since no interface types were specified.", ListenerSupport.class.getName()));
			return;
		}
		for (Object listenerInterface : listenerInterfaces) {
			if (listenerInterface instanceof ClassLiteralAccess) {
				TypeBinding binding = ((ClassLiteralAccess) listenerInterface).type.resolveType(type.get().initializerScope);
				if (binding == null) continue;
				if (!binding.isInterface()) {
					annotationNode.addWarning(String.format("@%s works only with interfaces. %s was skipped", ListenerSupport.class.getName(), As.string(binding.readableName())));
					continue;
				}
				handler.addListenerField(type, binding);
				handler.addAddListenerMethod(type, binding);
				handler.addRemoveListenerMethod(type, binding);
				addFireListenerMethods(type, binding);
			}
		}

		type.editor().rebuild();
	}

	private void addFireListenerMethods(final EclipseType type, final TypeBinding interfaze) {
		List<MethodBinding> methods = getInterfaceMethods(interfaze);
		for (MethodBinding method : methods) {
			handler.addFireListenerMethod(type, interfaze, method);
		}
	}

	private List<MethodBinding> getInterfaceMethods(final TypeBinding binding) {
		List<MethodBinding> methods = new ArrayList<MethodBinding>();
		getInterfaceMethods(binding, methods, new HashSet<String>());
		return methods;
	}

	private void getInterfaceMethods(final TypeBinding binding, final List<MethodBinding> methods, final Set<String> banList) {
		if (binding == null) return;
		ensureAllClassScopeMethodWereBuild(binding);
		if (binding instanceof ReferenceBinding) {
			ReferenceBinding rb = (ReferenceBinding) binding;
			MethodBinding[] availableMethods = rb.availableMethods();
			for (MethodBinding mb : Each.elementIn(availableMethods)) {
				String sig = As.string(mb.readableName());
				if (!banList.add(sig)) continue;
				methods.add(mb);
			}
			ReferenceBinding[] interfaces = rb.superInterfaces();
			for (ReferenceBinding iface : Each.elementIn(interfaces)) {
				getInterfaceMethods(iface, methods, banList);
			}
		}
	}

	private static class EclipseListenerSupportHandler extends ListenerSupportHandler<EclipseType> {

		@Override
		protected void createParamsAndArgs(final Object method, final List<Argument> params, final List<Expression<?>> args) {
			MethodBinding methodBinding = (MethodBinding) method;
			int argCounter = 0;
			for (TypeBinding parameter : Each.elementIn(methodBinding.parameters)) {
				String arg = "arg" + argCounter++;
				params.add(Arg(Type(parameter), arg));
				args.add(Name(arg));
			}
		}

		@Override
		protected String name(final Object object) {
			if (object instanceof MethodBinding) {
				return As.string(((MethodBinding) object).selector);
			} else {
				return As.string(((Binding) object).shortReadableName());
			}
		}

		@Override
		protected Object type(final Object object) {
			return object;
		}
	}
}
