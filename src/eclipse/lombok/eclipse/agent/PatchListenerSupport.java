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
package lombok.eclipse.agent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.eclipse.jdt.core.dom.Modifier.*;
import static java.lang.Character.*;
import static lombok.eclipse.Eclipse.*;
import static lombok.eclipse.handlers.EclipseNodeBuilder.*;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import lombok.ListenerSupport;
import lombok.eclipse.EclipseAST;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.TransformEclipseAST;
import lombok.patcher.Hook;
import lombok.patcher.MethodTarget;
import lombok.patcher.ScriptManager;
import lombok.patcher.StackRequest;
import lombok.patcher.scripts.ScriptBuilder;

public class PatchListenerSupport {
	static void addPatches(ScriptManager sm, boolean ecj) {
		final String CLASSSCOPE_SIG = "org.eclipse.jdt.internal.compiler.lookup.ClassScope";
		
		sm.addScript(ScriptBuilder.exitEarly()
				.target(new MethodTarget(CLASSSCOPE_SIG, "buildFieldsAndMethods", "void"))
				.request(StackRequest.THIS)
				.decisionMethod(new Hook(PatchListenerSupport.class.getName(), "handleListenerSupportForType", "boolean", CLASSSCOPE_SIG))
				.build());
	}
	
	public static boolean handleListenerSupportForType(ClassScope scope) {
		TypeDeclaration decl = scope.referenceContext;
		if (decl == null) return false;
		if (decl.annotations == null) return false;
		
		for (Annotation ann : decl.annotations) {
			if (ann.type == null) continue;
			TypeBinding tb = ann.resolvedType;
			if (tb == null) tb = ann.type.resolveType(decl.initializerScope);
			if (!new String(tb.readableName()).equals(ListenerSupport.class.getName())) continue;
			List<ClassLiteralAccess> listenerInterfaces = new ArrayList<ClassLiteralAccess>();
			for (MemberValuePair pair : ann.memberValuePairs()) {
				if (pair.name == null || "value".equals(new String(pair.name))) {
					if (pair.value instanceof ArrayInitializer) {
						for (Expression expr : ((ArrayInitializer)pair.value).expressions) {
							if (expr instanceof ClassLiteralAccess) listenerInterfaces.add((ClassLiteralAccess) expr);
						}
					}
					if (pair.value instanceof ClassLiteralAccess) {
						listenerInterfaces.add((ClassLiteralAccess) pair.value);
					}
				}
			}
			
			CompilationUnitDeclaration cud = decl.scope.compilationUnitScope().referenceContext;
			EclipseAST astNode = TransformEclipseAST.getAST(cud, true);
			EclipseNode typeNode = astNode.get(decl);
			if (listenerInterfaces.isEmpty()) {
				typeNode.addError("@ListenerSupport has no effect with if no interface classes was specified.", ann.sourceStart, ann.sourceEnd);
				return false;
			}
			for (ClassLiteralAccess cla : listenerInterfaces) {
				TypeBinding binding = cla.type.resolveType(decl.initializerScope);
				if (!binding.isInterface()) {
					typeNode.addWarning(String.format("@ListenerSupport works only with interfaces. %s was skipped", new String(binding.readableName())), ann.sourceStart, ann.sourceEnd);
					continue;
				}
				addListenerField(typeNode, ann, binding);
				addListenerMethod(typeNode, ann, binding);
				removeListenerMethod(typeNode, ann, binding);
				fireListenerMethod(typeNode, ann, binding);
			}
		}
		return false;
	}
	
	private static void addListenerField(EclipseNode typeNode, ASTNode source, TypeBinding binding) {
		// private final java.util.List<LISTENER_FULLTYPE> $registeredLISTENER_TYPE = new java.util.concurrent.CopyOnWriteArrayList<LISTENER_FULLTYPE>();
		AllocationExpression initialization = new AllocationExpression();
		setGeneratedByAndCopyPos(initialization, source);
		initialization.type = createTypeReference(source, "java.util.concurrent.CopyOnWriteArrayList", binding);
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		field(typeNode, source, PRIVATE | FINAL, createTypeReference(source, "java.util.List", binding), "$registered" + interfaceName)
			.withInitialization(initialization).inject();
	}
	
	private static TypeReference createTypeReference(ASTNode source, String typeName, TypeBinding binding) {
		char[][] listNameTokens = fromQualifiedName(typeName);
		TypeReference[][] args = new TypeReference[listNameTokens.length][];
		args[listNameTokens.length - 1] = new TypeReference[1];
		args[listNameTokens.length - 1][0] = makeType(binding, source, false);
		ParameterizedQualifiedTypeReference typeReference = new ParameterizedQualifiedTypeReference(listNameTokens, args, 0, poss(source, listNameTokens.length));
		setGeneratedByAndCopyPos(typeReference, source);
		return typeReference;
	}
	
	private static void addListenerMethod(EclipseNode typeNode, ASTNode source, TypeBinding binding) {
		// public void addLISTENER_TYPE(final LISTENER_FULLTYPE l) {
		//   if (!$registeredLISTENER_TYPE.contains(l))
		//     $registeredLISTENER_TYPE.add(l);
		// }
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		MessageSend containsL = methodCall(source, "$registered" + interfaceName, "contains", nameReference(source, "l"));
		Expression notContainsL = new UnaryExpression(containsL, OperatorIds.NOT);
		setGeneratedByAndCopyPos(notContainsL, source);
		IfStatement ifStatement = new IfStatement(notContainsL, methodCall(source, "$registered" + interfaceName, "add", nameReference(source, "l")), 0, 0);
		setGeneratedByAndCopyPos(ifStatement, source);
		method(typeNode, source, PUBLIC, typeReference(source, "void"), "add" + interfaceName).withParameter(makeType(binding, source, false), "l").withStatement(ifStatement).inject();
	}
	
	private static void removeListenerMethod(EclipseNode typeNode, ASTNode source, TypeBinding binding) {
		// public void removeLISTENER_TYPE(final LISTENER_FULLTYPE l) {
		//   $registeredLISTENER_TYPE.remove(l);
		// }
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		method(typeNode, source, PUBLIC, typeReference(source, "void"), "remove" + interfaceName).withParameter(makeType(binding, source, false), "l")
				.withStatement(methodCall(source, "$registered" + interfaceName, "remove", nameReference(source, "l"))).inject();
	}
	
	
	private static void fireListenerMethod(EclipseNode typeNode, ASTNode source, TypeBinding binding) {
		// protected void fireMETHOD_NAME(METHOD_PARAMETER) {
		//   for (LISTENER_FULLTYPE l :  $registeredLISTENER_TYPE)
		//     $registeredLISTENER_TYPE.METHOD_NAME(METHOD_ARGUMENTS);
		// }
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		List<MethodBinding> methods = new ArrayList<MethodBinding>();
		getInterfaceMethods(binding, methods);
		for (MethodBinding methodBinding : methods) {
			String methodName = new String(methodBinding.selector);
			LocalDeclaration local = local(typeNode, source, 0, makeType(binding, source, false), "l").build();
			ForeachStatement forEach = new ForeachStatement(local, 0);
			setGeneratedByAndCopyPos(forEach, source);
			forEach.collection = nameReference(source, "$registered" + interfaceName);
			List<Expression> args = new ArrayList<Expression>();
			List<Argument> params = new ArrayList<Argument>();
			createParamsAndArgs(source, methodBinding, params, args);
			forEach.action = methodCall(source, "l", methodName, args.toArray(new Expression[args.size()]));
			method(typeNode, source, PROTECTED, typeReference(source, "void"), "fire" + capizalize(methodName)).withParameters(params)
				.withStatement(forEach).inject();
		}
	}
	
	private static void createParamsAndArgs(ASTNode source, MethodBinding methodBinding , List<Argument> params, List<Expression> args) {
		if ((methodBinding.parameters == null) || (methodBinding.parameters.length == 0)) return;
		int argCounter = 0;
		String arg;
		for (TypeBinding parameter : methodBinding.parameters) {
			arg = " arg" + argCounter++;
			params.add(argument(source, makeType(parameter, source, false), arg));
			args.add(nameReference(source, arg));
		}
	}
	
	private static void getInterfaceMethods(TypeBinding binding, List<MethodBinding> list) {
		getInterfaceMethods(binding, list, new HashSet<String>());
	}
	
	private static void getInterfaceMethods(TypeBinding binding, List<MethodBinding> methods, final Set<String> banList) {
		if (binding == null) return;
		if (binding instanceof SourceTypeBinding) { // we need to build all sourcetype methods not only membertypes
			ClassScope cs = ((SourceTypeBinding)binding).scope;
			if (cs != null) {
				try {
					Reflection.classScopeBuildMethodsMethod.invoke(cs);
				} catch (Exception e) {
					// See 'Reflection' class for why we ignore this exception.
				}
			}
		}
		
		if (binding instanceof ReferenceBinding) {
			ReferenceBinding rb = (ReferenceBinding) binding;
			for (MethodBinding mb : rb.availableMethods()) {
				String sig = new String(mb.readableName());
				if (!banList.add(sig)) continue;
				methods.add(mb);
			}
			ReferenceBinding[] interfaces = rb.superInterfaces();
			if (interfaces != null) {
				for (ReferenceBinding iface : interfaces) getInterfaceMethods(iface, methods, banList);
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
	
	private static String interfaceName(String name) {
		if ((name.length() > 2) && (name.charAt(0) == 'I') && isUpperCase(name.charAt(1)) && isLowerCase(name.charAt(2))) {
			return name.substring(1);
		}
		return name;
	}
	
	private static String capizalize(final String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
}
