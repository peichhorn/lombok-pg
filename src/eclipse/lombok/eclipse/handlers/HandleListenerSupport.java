package lombok.eclipse.handlers;

import static lombok.core.util.Arrays.*;
import static lombok.core.util.Names.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.eclipse.Eclipse.*;
import static lombok.eclipse.handlers.Eclipse.typeDeclFiltering;
import static lombok.eclipse.handlers.EclipseNodeBuilder.*;
import static org.eclipse.jdt.core.dom.Modifier.*;
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

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
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
			addListenerMethod(typeNode, ann, binding);
			removeListenerMethod(typeNode, ann, binding);
			fireListenerMethod(typeNode, ann, binding);
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
		AllocationExpression initialization = new AllocationExpression();
		setGeneratedByAndCopyPos(initialization, source);
		
		initialization.type = typeReference(source, "java.util.concurrent.CopyOnWriteArrayList", makeType(binding, source, false));
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		field(typeNode, source, PRIVATE | FINAL, typeReference(source, "java.util.List", makeType(binding, source, false)), "$registered" + interfaceName)
			.withInitialization(initialization).inject();
	}
	
	private void addListenerMethod(EclipseNode typeNode, ASTNode source, TypeBinding binding) {
		// public void addLISTENER_TYPE(final LISTENER_FULLTYPE l) {
		//   if (!$registeredLISTENER_TYPE.contains(l))
		//     $registeredLISTENER_TYPE.add(l);
		// }
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		method(typeNode, source, PUBLIC, typeReference(source, "void"), "add" + interfaceName).withParameter(makeType(binding, source, false), "l") //
				.withStatement(ifNotStatement(source, methodCall(source, "$registered" + interfaceName, "contains", nameReference(source, "l")), //
						methodCall(source, "$registered" + interfaceName, "add", nameReference(source, "l")))).inject();
	}
	
	private void removeListenerMethod(EclipseNode typeNode, ASTNode source, TypeBinding binding) {
		// public void removeLISTENER_TYPE(final LISTENER_FULLTYPE l) {
		//   $registeredLISTENER_TYPE.remove(l);
		// }
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		method(typeNode, source, PUBLIC, typeReference(source, "void"), "remove" + interfaceName).withParameter(makeType(binding, source, false), "l")
				.withStatement(methodCall(source, "$registered" + interfaceName, "remove", nameReference(source, "l"))).inject();
	}
	
	
	private void fireListenerMethod(EclipseNode typeNode, ASTNode source, TypeBinding binding) {
		// protected void fireMETHOD_NAME(METHOD_PARAMETER) {
		//   for (LISTENER_FULLTYPE l :  $registeredLISTENER_TYPE)
		//     $registeredLISTENER_TYPE.METHOD_NAME(METHOD_ARGUMENTS);
		// }
		String interfaceName = interfaceName(new String(binding.shortReadableName()));
		List<MethodBinding> methods = getInterfaceMethods(binding);
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
			method(typeNode, source, PROTECTED, typeReference(source, "void"), camelCase("fire", methodName)).withParameters(params)
				.withStatement(forEach).inject();
		}
	}
	
	private void createParamsAndArgs(ASTNode source, MethodBinding methodBinding , List<Argument> params, List<Expression> args) {
		if (isEmpty(methodBinding.parameters)) return;
		int argCounter = 0;
		String arg;
		for (TypeBinding parameter : methodBinding.parameters) {
			arg = " arg" + argCounter++;
			params.add(argument(source, makeType(parameter, source, false), arg));
			args.add(nameReference(source, arg));
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
