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

import static lombok.ast.AST.*;
import static lombok.core.util.Arrays.*;
import static lombok.core.util.Names.*;
import static lombok.core.util.ErrorMessages.*;

import java.lang.reflect.Method;
import java.util.*;

import lombok.*;
import lombok.ast.Argument;
import lombok.ast.Expression;
import lombok.core.AnnotationValues;
import lombok.core.handlers.ListenerSupportHandler;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

/**
 * Handles the {@link ListenerSupport} annotation for eclipse using the {@link PatchListenerSupport}.
 */
// @ProviderFor(EclipseAnnotationHandler.class) // TODO
public class HandleListenerSupport extends EclipseAnnotationHandler<ListenerSupport> {
	private final EclipseListenerSupportHandler handler = new EclipseListenerSupportHandler();

	@Override public void handle(final AnnotationValues<ListenerSupport> annotation, final Annotation source, final EclipseNode annotationNode) {
		final Class<? extends java.lang.annotation.Annotation> annotationType = ListenerSupport.class;
		EclipseType type = EclipseType.typeOf(annotationNode, source);
		if (type.isAnnotation() || type.isInterface()) {
			annotationNode.addError(canBeUsedOnClassAndEnumOnly(annotationType));
			return;
		}

		List<Object> listenerInterfaces = annotation.getActualExpressions("value");
		if (listenerInterfaces.isEmpty()) {
			annotationNode.addError(String.format("@%s has no effect since no interface types were specified.", annotationType.getName()));
			return;
		}
		for (Object listenerInterface : listenerInterfaces) {
			if (listenerInterface instanceof ClassLiteralAccess) {
				TypeBinding binding = ((ClassLiteralAccess)listenerInterface).type.resolveType(type.get().initializerScope);
				if (binding == null) continue;
				if (!binding.isInterface()) {
					annotationNode.addWarning(String.format("@%s works only with interfaces. %s was skipped", annotationType.getName(), string(binding.readableName())));
					continue;
				}
				handler.addListenerField(type, binding);
				handler.addAddListenerMethod(type, binding);
				handler.addRemoveListenerMethod(type, binding);
				addFireListenerMethods(type, binding);
			}
		}

		type.rebuild();
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
			if (isNotEmpty(availableMethods)) for (MethodBinding mb : availableMethods) {
				String sig = string(mb.readableName());
				if (!banList.add(sig)) continue;
				methods.add(mb);
			}
			ReferenceBinding[] interfaces = rb.superInterfaces();
			if (isNotEmpty(interfaces)) for (ReferenceBinding iface : interfaces) {
				getInterfaceMethods(iface, methods, banList);
			}
		}
	}

	private void ensureAllClassScopeMethodWereBuild(final TypeBinding binding) {
		if (binding instanceof SourceTypeBinding) {
			ClassScope cs = ((SourceTypeBinding)binding).scope;
			if (cs != null) {
				try {
					Reflection.classScopeBuildMethodsMethod.invoke(cs);
				} catch (final Exception e) {
					// See 'Reflection' class for why we ignore this exception.
				}
			}
		}
	}

	private static final class Reflection {
		public static final Method classScopeBuildMethodsMethod;

		static {
			Method m = null;
			try {
				m = ClassScope.class.getDeclaredMethod("buildMethods");
				m.setAccessible(true);
			} catch (final Exception e) {
				// That's problematic, but as long as no local classes are used we don't actually need it.
				// Better fail on local classes than crash altogether.
			}

			classScopeBuildMethodsMethod = m;
		}
	}

	private static class EclipseListenerSupportHandler extends ListenerSupportHandler<EclipseType> {

		@Override
		protected void createParamsAndArgs(final Object method, final List<Argument> params, final List<Expression> args) {
			MethodBinding methodBinding = (MethodBinding)method;
			if (isEmpty(methodBinding.parameters)) return;
			int argCounter = 0;
			for (TypeBinding parameter : methodBinding.parameters) {
				String arg = "arg" + argCounter++;
				params.add(Arg(Type(parameter), arg));
				args.add(Name(arg));
			}
		}

		@Override
		protected String name(final Object object) {
			if (object instanceof MethodBinding) {
				return string(((MethodBinding)object).selector);
			} else {
				return string(((Binding)object).shortReadableName());
			}
		}

		@Override
		protected Object type(final Object object) {
			return object;
		}
	}
}
