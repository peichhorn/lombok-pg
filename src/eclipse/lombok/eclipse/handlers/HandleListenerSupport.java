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
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.agent.PatchListenerSupport;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
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

	@Override public void handle(AnnotationValues<ListenerSupport> annotation, Annotation source, EclipseNode annotationNode) {
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
				if (!binding.isInterface()) {
					annotationNode.addWarning(String.format("@%s works only with interfaces. %s was skipped", annotationType.getName(), new String(binding.readableName())));
					continue;
				}
				addListenerField(type, binding);
				addAddListenerMethod(type, binding);
				addRemoveListenerMethod(type, binding);
				addFireListenerMethod(type, binding);
			}
		}

		type.rebuild();
	}

	/**
	 * creates:
	 * <pre>
	 * private final java.util.List<LISTENER_FULLTYPE> $registeredLISTENER_TYPE =
	 *   new java.util.concurrent.CopyOnWriteArrayList<LISTENER_FULLTYPE>();
	 * </pre>
	 */
	private void addListenerField(EclipseType type, TypeBinding binding) {
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		type.injectField(FieldDecl(Type("java.util.List").withTypeArgument(Type(binding)), "$registered" + interfaceName).makePrivate().makeFinal() //
			.withInitialization(New(Type("java.util.concurrent.CopyOnWriteArrayList").withTypeArgument(Type(binding)))));
	}

	/**
	 * creates:
	 * <pre>
	 * public void addLISTENER_TYPE(final LISTENER_FULLTYPE l) {
	 *  if (!$registeredLISTENER_TYPE.contains(l))
	 *    $registeredLISTENER_TYPE.add(l);
	 * }
	 * </pre>
	 */
	private void addAddListenerMethod(EclipseType type, TypeBinding binding) {
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		type.injectMethod(MethodDecl(Type("void"), "add" + interfaceName).makePublic().withArgument(Arg(Type(binding), "l")) //
			.withStatement(If(Not(Call(Name("$registered" + interfaceName), "contains").withArgument(Name("l")))) //
				.Then(Call(Name("$registered" + interfaceName), "add").withArgument(Name("l")))));
	}

	/**
	 * creates:
	 * <pre>
	 * public void removeLISTENER_TYPE(final LISTENER_FULLTYPE l) {
	 *   $registeredLISTENER_TYPE.remove(l);
	 * }
	 * </pre>
	 */
	private void addRemoveListenerMethod(EclipseType type, TypeBinding binding) {
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		type.injectMethod(MethodDecl(Type("void"), "remove" + interfaceName).makePublic().withArgument(Arg(Type(binding), "l")) //
			.withStatement(Call(Name("$registered" + interfaceName), "remove").withArgument(Name("l"))));
	}

	/**
	 * creates:
	 * <pre>
	 * protected void fireMETHOD_NAME(METHOD_PARAMETER) {
	 *   for (LISTENER_FULLTYPE l :  $registeredLISTENER_TYPE)
	 *     l.METHOD_NAME(METHOD_ARGUMENTS);
	 * }
	 * </pre>
	 */
	private void addFireListenerMethod(EclipseType type, TypeBinding binding) {
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		List<MethodBinding> methods = getInterfaceMethods(binding);
		for (MethodBinding methodBinding : methods) {
			String methodName = new String(methodBinding.selector);
			List<lombok.ast.Expression> args = new ArrayList<lombok.ast.Expression>();
			List<lombok.ast.Argument> params = new ArrayList<lombok.ast.Argument>();
			createParamsAndArgs(methodBinding, params, args);
			type.injectMethod(MethodDecl(Type("void"), camelCase("fire", methodName)).makeProtected().withArguments(params) //
				.withStatement(Foreach(LocalDecl(Type(binding), "l")).In(Name("$registered" + interfaceName)) //
					.Do(Call(Name("l"), methodName).withArguments(args))));
		}
	}

	private void createParamsAndArgs(MethodBinding methodBinding, List<lombok.ast.Argument> params, List<lombok.ast.Expression> args) {
		if (isEmpty(methodBinding.parameters)) return;
		int argCounter = 0;
		String arg;
		for (TypeBinding parameter : methodBinding.parameters) {
			arg = "arg" + argCounter++;
			params.add(Arg(Type(parameter), arg));
			args.add(Name(arg));
		}
	}

	private List<MethodBinding> getInterfaceMethods(TypeBinding binding) {
		List<MethodBinding> methods = new ArrayList<MethodBinding>();
		getInterfaceMethods(binding, methods, new HashSet<String>());
		return methods;
	}

	private void getInterfaceMethods(TypeBinding binding, List<MethodBinding> methods, final Set<String> banList) {
		if (binding == null) return;
		ensureAllClassScopeMethodWereBuild(binding);
		if (binding instanceof ReferenceBinding) {
			ReferenceBinding rb = (ReferenceBinding) binding;
			for (MethodBinding mb : rb.availableMethods()) {
				String sig = new String(mb.readableName());
				if (!banList.add(sig)) continue;
				methods.add(mb);
			}
			ReferenceBinding[] interfaces = rb.superInterfaces();
			if (isNotEmpty(interfaces)) for (ReferenceBinding iface : interfaces) {
				getInterfaceMethods(iface, methods, banList);
			}
		}
	}

	private void ensureAllClassScopeMethodWereBuild(TypeBinding binding) {
		if (binding instanceof SourceTypeBinding) {
			ClassScope cs = ((SourceTypeBinding)binding).scope;
			if (cs != null) {
				try {
					Reflection.classScopeBuildMethodsMethod.invoke(cs);
				} catch (Exception e) {
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
			} catch (Exception e) {
				// That's problematic, but as long as no local classes are used we don't actually need it.
				// Better fail on local classes than crash altogether.
			}

			classScopeBuildMethodsMethod = m;
		}
	}
}
