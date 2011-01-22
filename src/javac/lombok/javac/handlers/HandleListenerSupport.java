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
import static lombok.javac.handlers.JavacTreeBuilder.*;
import static com.sun.tools.javac.code.Flags.*;
import static java.lang.Character.*;

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
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.util.Name;

@ProviderFor(JavacAnnotationHandler.class)
public class HandleListenerSupport implements JavacAnnotationHandler<ListenerSupport> {
	@Override public boolean isResolutionBased() {
		return true;
	}
	
	@Override public boolean handle(AnnotationValues<ListenerSupport> annotation, JCAnnotation ast, JavacNode annotationNode) {
		markAnnotationAsProcessed(annotationNode, ListenerSupport.class);
		
		if (isNoClassAndNoEnum(annotationNode)) {
			return false;
		}
			
		List<Object> listenerInterfaces = annotation.getActualExpressions("value");
		if (listenerInterfaces.isEmpty()) {
			annotationNode.addError("@ListenerSupport has no effect with if no interface classes was specified.");
			return false;
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
			addListenerField((ClassType) interfaze, annotationNode);
			addListenerMethod((ClassType) interfaze, annotationNode);
			removeListenerMethod((ClassType) interfaze, annotationNode);
			fireListenerMethod((ClassType) interfaze, annotationNode);
		}
		
		annotationNode.up().rebuild();
		
		return true;
	}
	
	private static boolean isNoClassAndNoEnum(JavacNode annotationNode) {
		JavacNode typeNode = annotationNode.up();
		JCClassDecl typeDecl = null;
		if (typeNode.get() instanceof JCClassDecl) typeDecl = (JCClassDecl)typeNode.get();
		long flags = typeDecl == null ? 0 : typeDecl.mods.flags;
		boolean notAClass = (flags & (INTERFACE | ANNOTATION)) != 0;
		if (typeDecl == null || notAClass) {
			annotationNode.addError("@ListenerSupport is legal only on classes and enums.");
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
	
	private static void addListenerField(ClassType ct, JavacNode node) {
		final String defString = "private final java.util.List<%s> $registered%s = new java.util.concurrent.CopyOnWriteArrayList<%s>();";
		TypeSymbol tsym = ct.asElement();
		if (tsym == null) return;
		field(node.up(), defString, tsym.type, interfaceName(tsym.name), tsym.type).inject();
	}
	
	private static void addListenerMethod(ClassType ct, JavacNode node) {
		final String defString = "public void add%s(final %s l) { if (!$registered%s.contains(l)) { $registered%s.add(l); } }";
		TypeSymbol tsym = ct.asElement();
		if (tsym == null) return;
		String interfaceName = interfaceName(tsym.name);
		method(node.up(), defString, interfaceName, tsym.type, interfaceName, interfaceName).inject();
	}
	
	private static void removeListenerMethod(ClassType ct, JavacNode node) {
		final String defString = "public void remove%s(final %s l) { $registered%s.remove(l); }";
		TypeSymbol tsym = ct.asElement();
		if (tsym == null) return;
		String interfaceName = interfaceName(tsym.name);
		method(node.up(), defString, interfaceName, tsym.type, interfaceName).inject();
	}
	
	private static void fireListenerMethod(ClassType interfazeType, JavacNode node) {
		fireListenerMethodAll(interfazeType, interfazeType, node);
	}
	
	private static void fireListenerMethodAll(ClassType interfazeType, ClassType superInterfazeType, JavacNode node) {
		final String defString = "protected void fire%s(%s) { for (%s l : $registered%s) { l.%s(%s); } }";
		TypeSymbol isym = interfazeType.asElement();
		String interfaceName = interfaceName(isym.name);
		TypeSymbol tsym = superInterfazeType.asElement();
		if (tsym == null) return;
		for (Symbol member : tsym.getEnclosedElements()) {
			if (member.getKind() != ElementKind.METHOD) continue;
			StringBuilder params = new StringBuilder();
			StringBuilder args = new StringBuilder();
			createParamsAndArgs((MethodType)((MethodSymbol)member).type, params, args);
			method(node.up(), defString, capizalize(member.name), params, isym.type, interfaceName, member.name, args).inject();
		}
		if (superInterfazeType.interfaces_field != null) for (Type iface : superInterfazeType.interfaces_field) {
			if (iface instanceof ClassType) fireListenerMethodAll(interfazeType, (ClassType) iface, node);
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
	
	private static String interfaceName(Name name) {
		if ((name.length() > 2) && (name.charAt(0) == 'I') && isUpperCase(name.charAt(1)) && isLowerCase(name.charAt(2))) {
			return name.toString().substring(1);
		}
		return name.toString();
	}
	
	private static String capizalize(final Name name) {
		final String s = name.toString();
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
}
