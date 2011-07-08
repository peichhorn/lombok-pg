/*
 * Copyright © 2011 Philipp Eichhorn
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

import static lombok.ast.AST.*;
import static lombok.core.util.Arrays.*;
import static lombok.eclipse.agent.Patches.*;
import static lombok.patcher.scripts.ScriptBuilder.*;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.internal.codeassist.CompletionEngine;
import org.eclipse.jdt.internal.codeassist.InternalCompletionContext;
import org.eclipse.jdt.internal.codeassist.InternalCompletionProposal;
import org.eclipse.jdt.internal.codeassist.InternalExtendedCompletionContext;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMemberAccess;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnQualifiedNameReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnSingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.core.SearchableEnvironment;
import org.eclipse.jdt.internal.ui.text.java.AbstractJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.util.Types;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseType;
import lombok.patcher.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PatchExtensionMethod {
	static void addPatches(ScriptManager sm, boolean ecj) {
		sm.addScript(wrapReturnValue()
			.target(new MethodTarget(MESSAGESEND, "resolveType", TYPEBINDING, BLOCKSCOPE))
			.request(StackRequest.RETURN_VALUE)
			.request(StackRequest.THIS)
			.request(StackRequest.PARAM1)
			.wrapMethod(new Hook(PatchExtensionMethod.class.getName(), "resolveType", TYPEBINDING, TYPEBINDING, MESSAGESEND, BLOCKSCOPE))
			.build());

		sm.addScript(replaceMethodCall()
			.target(new MethodTarget(MESSAGESEND, "resolveType", TYPEBINDING, BLOCKSCOPE))
			.methodToReplace(new Hook(PROBLEMREPORTER, "errorNoMethodFor", "void", MESSAGESEND, TYPEBINDING, TYPEBINDINGS))
			.replacementMethod(new Hook(PatchExtensionMethod.class.getName(), "errorNoMethodFor", "void", PROBLEMREPORTER, MESSAGESEND, TYPEBINDING, TYPEBINDINGS))
			.build());
		sm.addScript(replaceMethodCall()
			.target(new MethodTarget(MESSAGESEND, "resolveType", TYPEBINDING, BLOCKSCOPE))
			.methodToReplace(new Hook(PROBLEMREPORTER, "invalidMethod", "void", MESSAGESEND, METHODBINDING))
			.replacementMethod(new Hook(PatchExtensionMethod.class.getName(), "invalidMethod", "void", PROBLEMREPORTER, MESSAGESEND, METHODBINDING))
			.build());

		if (!ecj) {
			sm.addScript(wrapReturnValue()
				.target(new MethodTarget(COMPLETIONPROPOSALCOLLECTOR, "getJavaCompletionProposals", IJAVACOMPLETIONPROPOSALS))
				.request(StackRequest.RETURN_VALUE)
				.request(StackRequest.THIS)
				.wrapMethod(new Hook(PatchExtensionMethod.class.getName(), "getJavaCompletionProposals", IJAVACOMPLETIONPROPOSALS, IJAVACOMPLETIONPROPOSALS, COMPLETIONPROPOSALCOLLECTOR))
				.build());
		}
	}

	private static Map<MessageSend, PostponedError> errors = new HashMap<MessageSend, PostponedError>();

	public static void errorNoMethodFor(ProblemReporter problemReporter, MessageSend messageSend, TypeBinding recType, TypeBinding[] params) {
		errors.put(messageSend, new PostponedNoMethodError(problemReporter, messageSend, recType, params));
	}

	public static void invalidMethod(ProblemReporter problemReporter, MessageSend messageSend, MethodBinding method) {
		errors.put(messageSend, new PostponedInvalidMethodError(problemReporter, messageSend, method));
	}

	public static TypeBinding resolveType(TypeBinding resolvedType, MessageSend methodCall, BlockScope scope) {
		if (methodCall.binding instanceof ProblemMethodBinding) {
			TypeDeclaration decl = scope.classScope().referenceContext;
			EclipseNode typeNode = getTypeNode(decl);
			Annotation ann = getAnnotation(ExtensionMethod.class, decl);
			List<MethodBinding> extensionMethods = getApplicableExtensionMethods(typeNode, ann, methodCall.receiver.resolvedType);
			if (!extensionMethods.isEmpty()) {
				EclipseType type = EclipseType.typeOf(typeNode, ann);
				for (MethodBinding extensionMethod : extensionMethods) {
					if (!Arrays.equals(methodCall.selector, extensionMethod.selector)) continue;
					if (methodCall.receiver instanceof ThisReference) {
						if ((methodCall.receiver.bits & ASTNode.IsImplicitThis) != 0) {
							methodCall.receiver.bits &= ~ASTNode.IsImplicitThis;
						}
					}
					List<Expression> arguments = new ArrayList<Expression>();
					arguments.add(methodCall.receiver);
					if (isNotEmpty(methodCall.arguments)) Collections.addAll(arguments, methodCall.arguments);
					methodCall.arguments = arguments.toArray(new Expression[0]);
					methodCall.receiver = type.build(Name(Eclipse.toQualifiedName(extensionMethod.declaringClass.compoundName)));
					methodCall.binding = extensionMethod;
					methodCall.resolvedType = extensionMethod.returnType;
					methodCall.actualReceiverType = extensionMethod.declaringClass;
					errors.remove(methodCall);
					return methodCall.resolvedType;
				}
			}
		}
		PostponedError error = errors.get(methodCall);
		if (error != null) {
			error.fire();
		}
		errors.remove(methodCall);
		return resolvedType;
	}
	
	public static IJavaCompletionProposal[] getJavaCompletionProposals(IJavaCompletionProposal[] javaCompletionProposals, CompletionProposalCollector completionProposalCollector) {
		List<IJavaCompletionProposal> proposals = new ArrayList<IJavaCompletionProposal>(Arrays.asList(javaCompletionProposals));
		if (canExtendCodeAssist(proposals)) {
			IJavaCompletionProposal firstProposal = proposals.get(0);
			int replacementOffset = getReplacementOffset(firstProposal);
			for (MethodBinding method : getExtensionMethods(completionProposalCollector)) {
				ExtensionMethodCompletionProposal newProposal = new ExtensionMethodCompletionProposal(replacementOffset);
				copyNameLookupAndCompletionEngine(completionProposalCollector, firstProposal, newProposal);
				newProposal.setMethodBinding(method);
				createAndAddJavaCompletionProposal(completionProposalCollector, newProposal, proposals);
			}
		}
		return proposals.toArray(new IJavaCompletionProposal[proposals.size()]);
	}
	
	private static List<MethodBinding> getExtensionMethods(CompletionProposalCollector completionProposalCollector) {
		List<MethodBinding> extensionMethods = new ArrayList<MethodBinding>();
		ClassScope classScope = getClassScope(completionProposalCollector);
		if (classScope != null) {
			TypeDeclaration decl = classScope.referenceContext;
			EclipseNode typeNode = getTypeNode(decl);
			Annotation ann = getAnnotation(ExtensionMethod.class, decl);
			TypeBinding firstParameterType = getFirstParameterType(decl, completionProposalCollector);
			extensionMethods.addAll(getApplicableExtensionMethods(typeNode, ann, firstParameterType));
		}
		return extensionMethods;
	}
	
	private static List<MethodBinding> getApplicableExtensionMethods(EclipseNode typeNode, Annotation ann, TypeBinding receiverType) {
		List<MethodBinding> extensionMethods = new ArrayList<MethodBinding>();
		if ((typeNode != null) && (ann != null) && (receiverType != null)) {
			BlockScope blockScope = ((TypeDeclaration) typeNode.get()).initializerScope;
			EclipseNode annotationNode = typeNode.getNodeFor(ann);
			AnnotationValues<ExtensionMethod> annotation = Eclipse.createAnnotation(ExtensionMethod.class, annotationNode);
			for (Object extensionMethodProvider : annotation.getActualExpressions("value")) {
				if (extensionMethodProvider instanceof ClassLiteralAccess) {
					TypeBinding binding = ((ClassLiteralAccess)extensionMethodProvider).type.resolveType(blockScope);
					if (binding == null) continue;
					if (!binding.isClass() && !binding.isEnum()) continue;
					extensionMethods.addAll(getApplicableExtensionMethodsDefinedInProvider(typeNode, (ReferenceBinding) binding, receiverType));
				}
			}
		}
		return extensionMethods;
	}
	
	private static List<MethodBinding> getApplicableExtensionMethodsDefinedInProvider(EclipseNode typeNode, ReferenceBinding extensionMethodProviderBinding, TypeBinding receiverType) {
		List<MethodBinding> extensionMethods = new ArrayList<MethodBinding>();
		CompilationUnitScope cuScope = ((CompilationUnitDeclaration) typeNode.top().get()).scope;
		for (MethodBinding method : extensionMethodProviderBinding.methods()) {
			if (!method.isStatic()) continue;
			if (!method.isPublic()) continue;
			if (isEmpty(method.parameters)) continue;
			if (!receiverType.isCompatibleWith(method.parameters[0])) continue;
			TypeBinding[] argumentTypes = Arrays.copyOfRange(method.parameters, 1, method.parameters.length);
			if ((receiverType instanceof ReferenceBinding) && ((ReferenceBinding) receiverType).getExactMethod(method.selector, argumentTypes, cuScope) != null) continue;
			extensionMethods.add(method);
		}
		return extensionMethods;
	}
	
	private static TypeBinding getFirstParameterType(TypeDeclaration decl, CompletionProposalCollector completionProposalCollector) {
		TypeBinding firstParameterType = null;
		try {
			InternalCompletionContext context = (InternalCompletionContext) Reflection.contextField.get(completionProposalCollector);
			InternalExtendedCompletionContext extendedContext = (InternalExtendedCompletionContext) Reflection.extendedContextField.get(context);
			if (extendedContext == null) return null; 
			ASTNode node = (ASTNode) Reflection.assistNodeField.get(extendedContext);
			if (node == null) return null;
			if( Types.isNoneOf(node, CompletionOnQualifiedNameReference.class, CompletionOnSingleNameReference.class, CompletionOnMemberAccess.class)) return null;
			if (node instanceof NameReference) {
				Binding binding = ((NameReference)node).binding;
				if (binding instanceof VariableBinding) {
					firstParameterType = ((VariableBinding)binding).type;
				} else {
					firstParameterType = (TypeBinding)binding;
				}
			} else if (node instanceof FieldReference) {
				firstParameterType = ((FieldReference)node).actualReceiverType;
			}
			if (firstParameterType == null) {
				firstParameterType = decl.binding;
			}
		} catch (Exception ignore) {
			// ignore
		}
		return firstParameterType;
	}
	
	private static ClassScope getClassScope(CompletionProposalCollector completionProposalCollector) {
		ClassScope scope = null;
		try {
			InternalCompletionContext context = (InternalCompletionContext) Reflection.contextField.get(completionProposalCollector);
			InternalExtendedCompletionContext extendedContext = (InternalExtendedCompletionContext) Reflection.extendedContextField.get(context);
			if (extendedContext != null) {
				scope = ((Scope) Reflection.assistScopeField.get(extendedContext)).classScope();
			}
		} catch (Exception ignore) {
			// ignore
		}
		return scope;
	}
	
	private static void copyNameLookupAndCompletionEngine(CompletionProposalCollector completionProposalCollector, IJavaCompletionProposal proposal, InternalCompletionProposal newProposal) {
		try {
			InternalCompletionContext context = (InternalCompletionContext) Reflection.contextField.get(completionProposalCollector);
			InternalExtendedCompletionContext extendedContext = (InternalExtendedCompletionContext) Reflection.extendedContextField.get(context);
			LookupEnvironment lookupEnvironment = (LookupEnvironment) Reflection.lookupEnvironmentField.get(extendedContext);
			Reflection.nameLookup.set(newProposal, ((SearchableEnvironment)lookupEnvironment.nameEnvironment).nameLookup);
			Reflection.completionEngine.set(newProposal, lookupEnvironment.typeRequestor);
		} catch (Exception ignore) {
			// ignore
		}
	}
	
	private static void createAndAddJavaCompletionProposal(CompletionProposalCollector completionProposalCollector, CompletionProposal newProposal, List<IJavaCompletionProposal> proposals) {
		try {
			proposals.add((IJavaCompletionProposal)Reflection.createJavaCompletionProposalMethod.invoke(completionProposalCollector, newProposal));
		} catch (Exception ignore) {
			// ignore
		}
	}
	
	private static int getReplacementOffset(IJavaCompletionProposal proposal) {
		try {
			return Reflection.replacementOffsetField.getInt(proposal);
		} catch (Exception ignore) {
			return 0;
		}
	}

	private static boolean canExtendCodeAssist(List<IJavaCompletionProposal> proposals) {
		return !proposals.isEmpty() && Reflection.canExtendCodeAssist;
	}
	
	private static class ExtensionMethodCompletionProposal extends InternalCompletionProposal {
		private final int replacementOffset;

		public ExtensionMethodCompletionProposal(int replacementOffset) {
			super(CompletionProposal.METHOD_REF, replacementOffset - 1);
			this.replacementOffset = replacementOffset; 
		}
		
		public void setMethodBinding(MethodBinding method) {
			MethodBinding original = method.original();
			TypeBinding[] parameters = Arrays.copyOf(method.parameters, method.parameters.length);
			method.parameters = Arrays.copyOfRange(method.parameters, 1, method.parameters.length);
			TypeBinding[] originalParameters = null;
			if (original != method) {
				originalParameters = Arrays.copyOf(method.original().parameters, method.original().parameters.length);
				method.original().parameters = Arrays.copyOfRange(method.original().parameters, 1, method.original().parameters.length);
			}
			
			int length = isEmpty(method.parameters) ? 0 : method.parameters.length;
			char[][] parameterPackageNames = new char[length][];
			char[][] parameterTypeNames = new char[length][];

			for (int i = 0; i < length; i++) {
				TypeBinding type = method.original().parameters[i];
				parameterPackageNames[i] = type.qualifiedPackageName();
				parameterTypeNames[i] = type.qualifiedSourceName();
			}
			char[] completion = CharOperation.concat(method.selector, new char[] { '(', ')' });
			setDeclarationSignature(CompletionEngine.getSignature(method.declaringClass));
			setSignature(CompletionEngine.getSignature(method));
			
			if (original != method) {
				setOriginalSignature(CompletionEngine.getSignature(original));
			}
			setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
			setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
			setParameterPackageNames(parameterPackageNames);
			setParameterTypeNames(parameterTypeNames);
			setPackageName(method.returnType.qualifiedPackageName());
			setTypeName(method.returnType.qualifiedSourceName());
			setName(method.selector);
			setCompletion(completion);
			setFlags(method.modifiers & (~Modifier.STATIC));
			setReplaceRange(replacementOffset, replacementOffset + completion.length);
			setTokenRange(replacementOffset, replacementOffset + completion.length);
			setRelevance(100);
			
			method.parameters = parameters;
			if (original != method) {
				method.original().parameters = originalParameters;
			}
		}
	}
	

	@RequiredArgsConstructor
	private static class PostponedNoMethodError implements PostponedError {
		private final ProblemReporter problemReporter;
		private final MessageSend messageSend;
		private final TypeBinding recType;
		private final TypeBinding[] params;
		
		public void fire() {
			problemReporter.errorNoMethodFor(messageSend, recType, params);
		}
	}

	@RequiredArgsConstructor
	private static class PostponedInvalidMethodError implements PostponedError {
		private final ProblemReporter problemReporter;
		private final MessageSend messageSend;
		private final MethodBinding method;
		
		public void fire() {
			problemReporter.invalidMethod(messageSend, method);
		}
	}

	private static interface PostponedError {
		public void fire();
	}

	private static class Reflection {
		public static final Field replacementOffsetField;
		public static final Field contextField;
		public static final Field extendedContextField;
		public static final Field assistNodeField;
		public static final Field assistScopeField;
		public static final Field lookupEnvironmentField;
		public static final Field completionEngine;
		public static final Field nameLookup;
		public static final Method createJavaCompletionProposalMethod;
		public static final boolean canExtendCodeAssist;
		
		static {
			boolean[] available = new boolean[] { true };
			replacementOffsetField = accessField(AbstractJavaCompletionProposal.class, "fReplacementOffset", available);
			contextField = accessField(CompletionProposalCollector.class, "fContext", available);
			extendedContextField = accessField(InternalCompletionContext.class, "extendedContext", available);
			assistNodeField = accessField(InternalExtendedCompletionContext.class, "assistNode", available);
			assistScopeField = accessField(InternalExtendedCompletionContext.class, "assistScope", available);
			lookupEnvironmentField = accessField(InternalExtendedCompletionContext.class, "lookupEnvironment", available);
			completionEngine = accessField(InternalCompletionProposal.class, "completionEngine", available);
			nameLookup = accessField(InternalCompletionProposal.class, "nameLookup", available);
			createJavaCompletionProposalMethod = accessMethod(CompletionProposalCollector.class, "createJavaCompletionProposal", CompletionProposal.class, available);
			canExtendCodeAssist = available[0];
		}
		
		private static Field accessField(Class<?> clazz, String fieldName, boolean[] available) {
			try {
				return makeAccessible(clazz.getDeclaredField(fieldName));
			} catch (Exception e) {
				available[0] = false;
				return null;
			}
		}
		
		private static Method accessMethod(Class<?> clazz, String methodName, Class<?> parameter, boolean[] available) {
			try {
				return makeAccessible(clazz.getDeclaredMethod(methodName, parameter));
			} catch (Exception e) {
				available[0] = false;
				return null;
			}
		}
		
		private static <T extends AccessibleObject> T makeAccessible(T object) {
			object.setAccessible(true);
			return object;
		}
	}
}
