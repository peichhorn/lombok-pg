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

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.ElementKind;
import lombok.*;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacResolution;
import lombok.javac.handlers.ast.JavacType;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

import org.mangosdk.spi.ProviderFor;

@ProviderFor(JavacAnnotationHandler.class)
public class HandleListenerSupport extends JavacAnnotationHandler<ListenerSupport> {

	@Override public void handle(AnnotationValues<ListenerSupport> annotation, JCAnnotation source, JavacNode annotationNode) {
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
		List<Type> resolvedInterfaces = new ArrayList<Type>();
		for (Object listenerInterface : listenerInterfaces) {
			if (listenerInterface instanceof JCFieldAccess) {
				JCFieldAccess interfaze = (JCFieldAccess)listenerInterface;
				if (interfaze.name.toString().equals("class")) {
					Type interfaceType = getType(interfaze.selected, annotationNode);
					if (interfaceType == null) continue;
					if (interfaceType.isInterface()) {
						resolvedInterfaces.add(interfaceType);
					} else {
						annotationNode.addWarning(String.format("@%s works only with interfaces. %s was skipped", annotationType.getName(), listenerInterface));
					}
				}
			}
		}
		for (Type interfaze : resolvedInterfaces) {
			addListenerField(type, (ClassType) interfaze);
			addAddListenerMethod(type, (ClassType) interfaze);
			addRemoveListenerMethod(type, (ClassType) interfaze);
			addFireListenerMethod(type, (ClassType) interfaze);
		}

		type.rebuild();
	}

	@Override
	public boolean isResolutionBased() {
		return true;
	}

	private static Type getType(JCExpression expr, JavacNode annotationNode) {
		Type type = expr.type;
		if (type == null) {
			new JavacResolution(annotationNode.getContext()).resolveClassMember(annotationNode);
			type = expr.type;
		}
		return type;
	}

	/**
	 * creates:
	 * <pre>
	 * private final java.util.List<LISTENER_FULLTYPE> $registeredLISTENER_TYPE =
	 *   new java.util.concurrent.CopyOnWriteArrayList<LISTENER_FULLTYPE>();
	 * </pre>
	 */
	private void addListenerField(JavacType type, ClassType ct) {
		TypeSymbol tsym = ct.asElement();
		if (tsym == null) return;
		String interfaceName = interfaceName(tsym.name.toString());
		type.injectField(FieldDecl(Type("java.util.List").withTypeArgument(Type(tsym.type)), "$registered" + interfaceName).makePrivate().makeFinal() //
			.withInitialization(New(Type("java.util.concurrent.CopyOnWriteArrayList").withTypeArgument(Type(tsym.type)))));
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
	private void addAddListenerMethod(JavacType type, ClassType ct) {
		TypeSymbol tsym = ct.asElement();
		if (tsym == null) return;
		String interfaceName = interfaceName(tsym.name.toString());
		type.injectMethod(MethodDecl(Type("void"), "add" + interfaceName).makePublic().withArgument(Arg(Type(tsym.type), "l")) //
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
	private void addRemoveListenerMethod(JavacType type, ClassType ct) {
		TypeSymbol tsym = ct.asElement();
		if (tsym == null) return;
		String interfaceName = interfaceName(tsym.name.toString());
		type.injectMethod(MethodDecl(Type("void"), "remove" + interfaceName).makePublic().withArgument(Arg(Type(tsym.type), "l")) //
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
	private void addFireListenerMethod(JavacType type, ClassType interfazeType) {
		addFireListenerMethodAll(type, interfazeType, interfazeType);
	}

	private void addFireListenerMethodAll(JavacType type, ClassType interfazeType, ClassType superInterfazeType) {
		TypeSymbol isym = interfazeType.asElement();
		String interfaceName = interfaceName(isym.name.toString());
		TypeSymbol tsym = superInterfazeType.asElement();
		if (tsym == null) return;
		for (Symbol member : tsym.getEnclosedElements()) {
			if (member.getKind() != ElementKind.METHOD) continue;
			String methodName = member.name.toString();
			List<lombok.ast.Expression> args = new ArrayList<lombok.ast.Expression>();
			List<lombok.ast.Argument> params = new ArrayList<lombok.ast.Argument>();
			createParamsAndArgs((MethodType)((MethodSymbol)member).type, params, args);
			type.injectMethod(MethodDecl(Type("void"), camelCase("fire", methodName)).makeProtected().withArguments(params) //
				.withStatement(Foreach(LocalDecl(Type(isym.type), "l")).In(Name("$registered" + interfaceName)) //
					.Do(Call(Name("l"), methodName).withArguments(args))));
		}
		if (superInterfazeType.interfaces_field != null) for (Type iface : superInterfazeType.interfaces_field) {
			if (iface instanceof ClassType) addFireListenerMethodAll(type, interfazeType, (ClassType) iface);
		}
	}

	private void createParamsAndArgs(MethodType mtype, List<lombok.ast.Argument> params, List<lombok.ast.Expression> args) {
		if (mtype.argtypes.isEmpty()) return;
		int argCounter = 0;
		String arg;
		for (Type parameter : mtype.argtypes) {
			arg = "arg" + argCounter++;
			params.add(Arg(Type(parameter), arg));
			args.add(Name(arg));
		}
	}
}
