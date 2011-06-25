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

import static lombok.core.util.Names.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.javac.handlers.Javac.typeDeclFiltering;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static lombok.javac.handlers.JavacTreeBuilder.*;
import static com.sun.tools.javac.code.Flags.*;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.ElementKind;
import lombok.ListenerSupport;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacResolution;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

@ProviderFor(JavacAnnotationHandler.class)
public class HandleListenerSupport extends JavacAnnotationHandler<ListenerSupport> {

	@Override public void handle(AnnotationValues<ListenerSupport> annotation, JCAnnotation source, JavacNode annotationNode) {
		deleteAnnotationIfNeccessary(annotationNode, ListenerSupport.class);

		if (isNoClassAndNoEnum(annotationNode)) {
			return;
		}

		List<Object> listenerInterfaces = annotation.getActualExpressions("value");
		if (listenerInterfaces.isEmpty()) {
			annotationNode.addError("@ListenerSupport has no effect with if no interface classes was specified.");
			return;
		}
		List<Type> resolvedInterfaces = new ArrayList<Type>();
		for (Object listenerInterface : listenerInterfaces) {
			if (listenerInterface instanceof JCFieldAccess) {
				JCFieldAccess interfaze = (JCFieldAccess)listenerInterface;
				if (interfaze.name.toString().equals("class")) {
					Type type = getType(interfaze.selected, annotationNode);
					if (type == null) continue;
					if (type.isInterface()) {
						resolvedInterfaces.add(type);
					} else {
						annotationNode.addWarning(String.format("@ListenerSupport works only with interfaces. %s was skipped", listenerInterface));
					}
				}
			}
		}
		for (Type interfaze : resolvedInterfaces) {
			addListenerField((ClassType) interfaze, annotationNode, source);
			addListenerMethod((ClassType) interfaze, annotationNode, source);
			removeListenerMethod((ClassType) interfaze, annotationNode, source);
			fireListenerMethod((ClassType) interfaze, annotationNode, source);
		}

		annotationNode.up().rebuild();
	}

	public boolean isResolutionBased() {
		return true;
	}

	private static boolean isNoClassAndNoEnum(JavacNode annotationNode) {
		JavacNode typeNode = annotationNode.up();
		JCClassDecl typeDecl = typeDeclFiltering(typeNode, INTERFACE | ANNOTATION);
		if (typeDecl == null) {
			annotationNode.addError(canBeUsedOnClassAndEnumOnly(ListenerSupport.class));
			return true;
		}
		return false;
	}

	private static Type getType(JCExpression expr, JavacNode annotationNode) {
		Type type = expr.type;
		if (type == null) {
			new JavacResolution(annotationNode.getContext()).resolveClassMember(annotationNode);
			type = expr.type;
		}
		return type;
	}

	private static void addListenerField(ClassType ct, JavacNode node, JCTree source) {
		final String defString = "private final java.util.List<%s> $registered%s = new java.util.concurrent.CopyOnWriteArrayList<%s>();";
		TypeSymbol tsym = ct.asElement();
		if (tsym == null) return;
		field(node.up(), defString, tsym.type, interfaceName(tsym.name.toString()), tsym.type).inject(source);
	}

	private static void addListenerMethod(ClassType ct, JavacNode node, JCTree source) {
		final String defString = "public void add%s(final %s l) { if (!$registered%s.contains(l)) { $registered%s.add(l); } }";
		TypeSymbol tsym = ct.asElement();
		if (tsym == null) return;
		String interfaceName = interfaceName(tsym.name.toString());
		method(node.up(), defString, interfaceName, tsym.type, interfaceName, interfaceName).inject(source);
	}

	private static void removeListenerMethod(ClassType ct, JavacNode node, JCTree source) {
		final String defString = "public void remove%s(final %s l) { $registered%s.remove(l); }";
		TypeSymbol tsym = ct.asElement();
		if (tsym == null) return;
		String interfaceName = interfaceName(tsym.name.toString());
		method(node.up(), defString, interfaceName, tsym.type, interfaceName).inject(source);
	}

	private static void fireListenerMethod(ClassType interfazeType, JavacNode node, JCTree source) {
		fireListenerMethodAll(interfazeType, interfazeType, node, source);
	}

	private static void fireListenerMethodAll(ClassType interfazeType, ClassType superInterfazeType, JavacNode node, JCTree source) {
		final String defString = "protected void %s(%s) { for (%s l : $registered%s) { l.%s(%s); } }";
		TypeSymbol isym = interfazeType.asElement();
		String interfaceName = interfaceName(isym.name.toString());
		TypeSymbol tsym = superInterfazeType.asElement();
		if (tsym == null) return;
		for (Symbol member : tsym.getEnclosedElements()) {
			if (member.getKind() != ElementKind.METHOD) continue;
			StringBuilder params = new StringBuilder();
			StringBuilder args = new StringBuilder();
			createParamsAndArgs((MethodType)((MethodSymbol)member).type, params, args);
			method(node.up(), defString, camelCase("fire", member.name.toString()), params, isym.type, interfaceName, member.name, args).inject(source);
		}
		if (superInterfazeType.interfaces_field != null) for (Type iface : superInterfazeType.interfaces_field) {
			if (iface instanceof ClassType) fireListenerMethodAll(interfazeType, (ClassType) iface, node, source);
		}
	}

	private static void createParamsAndArgs(MethodType mtype, StringBuilder params, StringBuilder args) {
		if (mtype.argtypes.isEmpty()) return;
		int argCounter = 0;
		params.append("final ").append(mtype.argtypes.head).append(" arg").append(argCounter);
		args.append("arg").append(argCounter++);
		if (mtype.argtypes.tail != null) for (Type atype : mtype.argtypes.tail) {
			params.append(", final ").append(atype).append(" arg").append(argCounter);
			args.append(", arg").append(argCounter++);
		}
	}
}
