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
import static lombok.core.util.Names.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.eclipse.handlers.Eclipse.*;
import static lombok.eclipse.Eclipse.*;
import static lombok.eclipse.handlers.ast.ASTBuilder.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.ListenerSupport;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.agent.PatchListenerSupport;
import lombok.eclipse.handlers.ast.ExpressionBuilder;
import lombok.eclipse.handlers.ast.StatementBuilder;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@link ListenerSupport} annotation for eclipse using the {@link PatchListenerSupport}.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleListenerSupport implements EclipseAnnotationHandler<ListenerSupport> {
	// error handling only
	@Override public boolean handle(AnnotationValues<ListenerSupport> annotation, Annotation ast, EclipseNode annotationNode) {
		EclipseNode typeNode = annotationNode.up();
		TypeDeclaration typeDecl = typeDeclFiltering(typeNode, AccInterface | AccAnnotation);
		if (typeDecl == null) {
			annotationNode.addError(canBeUsedOnClassAndEnumOnly(ListenerSupport.class));
		}
		return false;
	}

	// real meat
	public void handle(Annotation ann, TypeDeclaration decl, EclipseNode typeNode) {
		List<ClassLiteralAccess> listenerInterfaces = getListenerInterface(ann, "value");

		if (listenerInterfaces.isEmpty()) {
			typeNode.addError("@ListenerSupport has no effect with if no interface classes was specified.", ann.sourceStart, ann.sourceEnd);
			return;
		}
		for (ClassLiteralAccess cla : listenerInterfaces) {
			TypeBinding binding = cla.type.resolveType(decl.initializerScope);
			if (!binding.isInterface()) {
				typeNode.addWarning(String.format("@ListenerSupport works only with interfaces. %s was skipped", new String(binding.readableName())), ann.sourceStart, ann.sourceEnd);
				continue;
			}
			addListenerField(typeNode, ann, binding);
			addAddListenerMethod(typeNode, ann, binding);
			addRemoveListenerMethod(typeNode, ann, binding);
			addFireListenerMethod(typeNode, ann, binding);
		}
		return;
	}

	private Expression getAnnotationArgumentValue(Annotation ann, String arumentName) {
		for (MemberValuePair pair : ann.memberValuePairs()) {
			if ((pair.name == null) || arumentName.equals(new String(pair.name))) {
				return pair.value;
			}
		}
		return new NullLiteral(0, 0);
	}

	private List<ClassLiteralAccess> getListenerInterface(Annotation ann, String arumentName) {
		Expression value = getAnnotationArgumentValue(ann, arumentName);
		List<ClassLiteralAccess> listenerInterfaces = new ArrayList<ClassLiteralAccess>();
		if (value instanceof ArrayInitializer) {
			for (Expression expr : ((ArrayInitializer)value).expressions) {
				tryToAdd(listenerInterfaces, expr);
			}
		} else tryToAdd(listenerInterfaces, value);
		return listenerInterfaces;
	}

	private void tryToAdd(List<ClassLiteralAccess> list, Expression cla) {
		if (cla instanceof ClassLiteralAccess) {
			list.add((ClassLiteralAccess) cla);
		}
	}

	private void addListenerField(EclipseNode typeNode, ASTNode source, TypeBinding binding) {
		// private final java.util.List<LISTENER_FULLTYPE> $registeredLISTENER_TYPE = new java.util.concurrent.CopyOnWriteArrayList<LISTENER_FULLTYPE>();
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		FieldDef(Type("java.util.List").withTypeArgument(Type(makeType(binding, source, false))), "$registered" + interfaceName).makePrivateFinal() //
			.withInitialization(New(Type("java.util.concurrent.CopyOnWriteArrayList").withTypeArgument(Type(makeType(binding, source, false))))).injectInto(typeNode, source);
	}

	private void addAddListenerMethod(EclipseNode typeNode, ASTNode source, TypeBinding binding) {
		// public void addLISTENER_TYPE(final LISTENER_FULLTYPE l) {
		//   if (!$registeredLISTENER_TYPE.contains(l))
		//     $registeredLISTENER_TYPE.add(l);
		// }
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		MethodDef(Type("void"), "add" + interfaceName).makePublic().withArgument(Arg(Type(makeType(binding, source, false)), "l")) //
			.withStatement(If(Not(Call(Name("$registered" + interfaceName), "contains").withArgument(Name("l")))) //
					.Then(Call(Name("$registered" + interfaceName), "add").withArgument(Name("l")))).injectInto(typeNode, source);
	}

	private void addRemoveListenerMethod(EclipseNode typeNode, ASTNode source, TypeBinding binding) {
		// public void removeLISTENER_TYPE(final LISTENER_FULLTYPE l) {
		//   $registeredLISTENER_TYPE.remove(l);
		// }
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		MethodDef(Type("void"), "remove" + interfaceName).makePublic().withArgument(Arg(Type(makeType(binding, source, false)), "l")) //
			.withStatement(Call(Name("$registered" + interfaceName), "remove").withArgument(Name("l"))).injectInto(typeNode, source);
	}

	private void addFireListenerMethod(EclipseNode typeNode, ASTNode source, TypeBinding binding) {
		// protected void fireMETHOD_NAME(METHOD_PARAMETER) {
		//   for (LISTENER_FULLTYPE l :  $registeredLISTENER_TYPE)
		//     l.METHOD_NAME(METHOD_ARGUMENTS);
		// }
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		List<MethodBinding> methods = getInterfaceMethods(binding);
		for (MethodBinding methodBinding : methods) {
			String methodName = new String(methodBinding.selector);
			List<ExpressionBuilder<? extends Expression>> args = new ArrayList<ExpressionBuilder<? extends Expression>>();
			List<StatementBuilder<? extends Argument>> params = new ArrayList<StatementBuilder<? extends Argument>>();
			createParamsAndArgs(source, methodBinding, params, args);
			MethodDef(Type("void"), camelCase("fire", methodName)).makeProtected().withArguments(params) //
				.withStatement(Foreach(LocalDef(Type(makeType(binding, source, false)), "l")).In(Name("$registered" + interfaceName)) //
					.Do(Call(Name("l"), methodName).withArguments(args))).injectInto(typeNode, source);
		}
	}

	private void createParamsAndArgs(ASTNode source, MethodBinding methodBinding , List<StatementBuilder<? extends Argument>> params, List<ExpressionBuilder<? extends Expression>> args) {
		if (isEmpty(methodBinding.parameters)) return;
		int argCounter = 0;
		String arg;
		for (TypeBinding parameter : methodBinding.parameters) {
			arg = " arg" + argCounter++;
			params.add(Arg(Type(makeType(parameter, source, false)), arg));
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
