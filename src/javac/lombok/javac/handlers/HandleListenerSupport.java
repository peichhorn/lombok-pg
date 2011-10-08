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

import static lombok.ast.AST.*;
import static lombok.core.util.Names.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static lombok.javac.handlers.ast.JavacResolver.CLASS;

import java.util.*;

import javax.lang.model.element.ElementKind;
import lombok.*;
import lombok.ast.Argument;
import lombok.ast.Expression;
import lombok.core.AnnotationValues;
import lombok.core.handlers.ListenerSupportHandler;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.ResolutionBased;
import lombok.javac.handlers.ast.JavacType;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

import org.mangosdk.spi.ProviderFor;

@ProviderFor(JavacAnnotationHandler.class)
@ResolutionBased
public class HandleListenerSupport extends JavacAnnotationHandler<ListenerSupport> {
	private final JavacListenerSupportHandler handler = new JavacListenerSupportHandler();

	@Override public void handle(final AnnotationValues<ListenerSupport> annotation, final JCAnnotation source, final JavacNode annotationNode) {
		final Class<? extends java.lang.annotation.Annotation> annotationType = ListenerSupport.class;
		deleteAnnotationIfNeccessary(annotationNode, annotationType);

		JavacType type = JavacType.typeOf(annotationNode, source);
		if (type.isAnnotation() || type.isInterface()) {
			annotationNode.addError(canBeUsedOnClassAndEnumOnly(annotationType));
			return;
		}

		List<Object> listenerInterfaces = annotation.getActualExpressions("value");
		if (listenerInterfaces.isEmpty()) {
			annotationNode.addError(String.format("@%s has no effect since no interface types were specified.", annotationType.getName()));
			return;
		}
		List<TypeSymbol> resolvedInterfaces = resolveInterfaces(annotationNode, annotationType, listenerInterfaces);
		for (TypeSymbol interfaze : resolvedInterfaces) {
			handler.addListenerField(type, interfaze);
			handler.addAddListenerMethod(type, interfaze);
			handler.addRemoveListenerMethod(type, interfaze);
			addFireListenerMethods(type, interfaze);
		}

		type.rebuild();
	}

	private List<TypeSymbol> resolveInterfaces(final JavacNode annotationNode, final Class<? extends java.lang.annotation.Annotation> annotationType,
			final List<Object> listenerInterfaces) {
		List<TypeSymbol> resolvedInterfaces = new ArrayList<TypeSymbol>();
		for (Object listenerInterface : listenerInterfaces) {
			if (listenerInterface instanceof JCFieldAccess) {
				JCFieldAccess interfaze = (JCFieldAccess)listenerInterface;
				if ("class".equals(string(interfaze.name))) {
					Type interfaceType = CLASS.resolveMember(annotationNode, interfaze.selected);
					if (interfaceType == null) continue;
					if (interfaceType.isInterface()) {
						TypeSymbol interfaceSymbol = interfaceType.asElement();
						if (interfaceSymbol != null) resolvedInterfaces.add(interfaceSymbol);
					} else {
						annotationNode.addWarning(String.format("@%s works only with interfaces. %s was skipped", annotationType.getName(), listenerInterface));
					}
				}
			}
		}
		return resolvedInterfaces;
	}

	private void addFireListenerMethods(final JavacType type, final TypeSymbol interfaze) {
		addAllFireListenerMethods(type, interfaze, interfaze);
	}

	private void addAllFireListenerMethods(final JavacType type, final TypeSymbol interfaze, final TypeSymbol superInterfaze) {
		for (Symbol member : superInterfaze.getEnclosedElements()) {
			if (member.getKind() != ElementKind.METHOD) continue;
			handler.addFireListenerMethod(type, interfaze, (MethodSymbol)member);
		}
		ClassType superInterfazeType = (ClassType) superInterfaze.type;
		if (superInterfazeType.interfaces_field != null) for (Type iface : superInterfazeType.interfaces_field) {
			addAllFireListenerMethods(type, interfaze, iface.asElement());
		}
	}

	private static class JavacListenerSupportHandler extends ListenerSupportHandler<JavacType> {

		@Override
		protected void createParamsAndArgs(final Object method, final List<Argument> params, final List<Expression> args) {
			MethodType mtype = (MethodType) type(method);
			if (mtype.argtypes.isEmpty()) return;
			int argCounter = 0;
			for (Type parameter : mtype.getParameterTypes()) {
				String arg = "arg" + argCounter++;
				params.add(Arg(Type(parameter), arg));
				args.add(Name(arg));
			}
		}

		@Override
		protected String name(final Object object) {
			return string(((Symbol)object).name);
		}

		@Override
		protected Object type(final Object object) {
			return ((Symbol)object).type;
		}
	}
}
