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

import static lombok.core.util.Arrays.*;
import static lombok.eclipse.agent.Patches.*;
import static lombok.patcher.scripts.ScriptBuilder.*;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;
import org.eclipse.jdt.internal.ui.text.java.AbstractJavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.MemberProposalInfo;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.util.Types;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.HandleExtensionMethod;
import lombok.patcher.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PatchExtensionMethod {
	static void addPatches(ScriptManager sm, boolean ecj) {
		sm.addScript(exitEarly()
			.target(new MethodTarget(CLASSSCOPE, "buildFieldsAndMethods", "void"))
			.request(StackRequest.THIS)
			.decisionMethod(new Hook(PatchExtensionMethod.class.getName(), "onClassScope_buildFieldsAndMethods", "boolean", CLASSSCOPE))
			.build());
		
		sm.addScript(wrapReturnValue()
			.target(new MethodTarget(COMPLETIONPROPOSALCOLLECTOR, "getJavaCompletionProposals", IJAVACOMPLETIONPROPOSALS))
			.request(StackRequest.RETURN_VALUE)
			.request(StackRequest.THIS)
			.wrapMethod(new Hook(PatchExtensionMethod.class.getName(), "getJavaCompletionProposals", IJAVACOMPLETIONPROPOSALS, IJAVACOMPLETIONPROPOSALS, COMPLETIONPROPOSALCOLLECTOR))
			.build());
	}

	public static boolean onClassScope_buildFieldsAndMethods(ClassScope scope) {
		TypeDeclaration decl = scope.referenceContext;
		Annotation ann = getAnnotation(ExtensionMethod.class, decl);
		if (ann != null) {
			EclipseNode typeNode = getTypeNode(decl);
			if (typeNode != null) {
				EclipseNode annotationNode = typeNode.getNodeFor(ann);
				new HandleExtensionMethod().handle(Eclipse.createAnnotation(ExtensionMethod.class, annotationNode), ann, annotationNode);
			}
		}
		return false;
	}
	
	public static IJavaCompletionProposal[] getJavaCompletionProposals(IJavaCompletionProposal[] javaCompletionProposals, CompletionProposalCollector completionProposalCollector) {
		List<IJavaCompletionProposal> proposals = new ArrayList<IJavaCompletionProposal>(Arrays.asList(javaCompletionProposals));
		if (canExtendCodeAssist(proposals)) {
			IJavaCompletionProposal firstProposal = proposals.get(0);
			int replacementOffset = getReplacementOffset(firstProposal);
			for (MethodBinding method : getExtensionMethods(completionProposalCollector)) {
				ExtensionMethodCompletionProposal newProposal = new ExtensionMethodCompletionProposal(method, replacementOffset);
				copyNameLookupAndCompletionEngine(firstProposal, newProposal);
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
			TypeBinding firstParameterType = getFirstParameterType(completionProposalCollector);
			extensionMethods.addAll(getExtensionMethodsDefinedViaAnnotation(typeNode, ann, firstParameterType));
		}
		return extensionMethods;
	}
	
	private static List<MethodBinding> getExtensionMethodsDefinedViaAnnotation(EclipseNode typeNode, Annotation ann, TypeBinding firstParameterType) {
		List<MethodBinding> extensionMethods = new ArrayList<MethodBinding>();
		if ((typeNode != null) && (ann != null) && (firstParameterType != null)) {
			BlockScope blockScope = ((TypeDeclaration) typeNode.get()).initializerScope;
			EclipseNode annotationNode = typeNode.getNodeFor(ann);
			AnnotationValues<ExtensionMethod> annotation = Eclipse.createAnnotation(ExtensionMethod.class, annotationNode);
			for (Object extensionMethodProvider : annotation.getActualExpressions("value")) {
				if (extensionMethodProvider instanceof ClassLiteralAccess) {
					TypeBinding binding = ((ClassLiteralAccess)extensionMethodProvider).type.resolveType(blockScope);
					if (!(binding instanceof ReferenceBinding)) continue;
					extensionMethods.addAll(getExtensionMethodsDefinedInProvider(typeNode, (ReferenceBinding) binding, firstParameterType));
				}
			}
		}
		return extensionMethods;
	}
	
	private static List<MethodBinding> getExtensionMethodsDefinedInProvider(EclipseNode typeNode, ReferenceBinding extensionMethodProviderBinding, TypeBinding firstParameterType) {
		List<MethodBinding> extensionMethods = new ArrayList<MethodBinding>();
		CompilationUnitScope cuScope = ((CompilationUnitDeclaration) typeNode.top().get()).scope;
		for (MethodBinding method : extensionMethodProviderBinding.methods()) {
			if (!method.isStatic()) continue;
			if (!method.isPublic()) continue;
			if (isEmpty(method.parameters)) continue;
			if (!method.parameters[0].equals(firstParameterType)) continue;
			TypeBinding[] argumentTypes = Arrays.copyOfRange(method.parameters, 1, method.parameters.length);
			if ((firstParameterType instanceof ReferenceBinding) && ((ReferenceBinding) firstParameterType).getExactMethod(method.selector, argumentTypes, cuScope) != null) continue;
			extensionMethods.add(method);
		}
		return extensionMethods;
	}
	
	private static TypeBinding getFirstParameterType(CompletionProposalCollector completionProposalCollector) {
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
		} catch (Exception ignore) {
			// ignore
		}
		return firstParameterType;
	}
	
	private static ClassScope getClassScope(CompletionProposalCollector completionProposalCollector) {
		Scope scope = null;
		try {
			InternalCompletionContext context = (InternalCompletionContext) Reflection.contextField.get(completionProposalCollector);
			InternalExtendedCompletionContext extendedContext = (InternalExtendedCompletionContext) Reflection.extendedContextField.get(context);
			if (extendedContext != null) {
				scope = (Scope) Reflection.assistScopeField.get(extendedContext);
				while((scope != null) && !(scope instanceof ClassScope)) {
					scope = scope.parent;
				}
			}
		} catch (Exception ignore) {
			// ignore
		}
		return (ClassScope) scope;
	}
	
	private static void copyNameLookupAndCompletionEngine(IJavaCompletionProposal proposal, InternalCompletionProposal newProposal) {
		try {
			InternalCompletionProposal internalCompletionProposal = (InternalCompletionProposal) Reflection.proposal.get(Reflection.proposalInfoField.get(proposal));
			Reflection.nameLookup.set(newProposal, Reflection.nameLookup.get(internalCompletionProposal));
			Reflection.completionEngine.set(newProposal, Reflection.completionEngine.get(internalCompletionProposal));
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

		public ExtensionMethodCompletionProposal(MethodBinding method, int replacementOffset) {
			super(CompletionProposal.METHOD_REF, replacementOffset - 1);
			MethodBinding original = method.original();
			TypeBinding[] parameters = Arrays.copyOf(method.parameters, method.parameters.length);
			method.parameters = Arrays.copyOfRange(method.parameters, 1, method.parameters.length);
			TypeBinding[] originalParameters = null;
			if (original != method) {
				originalParameters = Arrays.copyOf(method.original().parameters, method.original().parameters.length);
				method.original().parameters = Arrays.copyOfRange(method.original().parameters, 1, method.original().parameters.length);
			}
			
			int length = method.parameters.length;
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

	private static class Reflection {
		public static final Field replacementOffsetField;
		public static final Field contextField;
		public static final Field extendedContextField;
		public static final Field assistNodeField;
		public static final Field assistScopeField;
		public static final Field proposalInfoField;
		public static final Field proposal;
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
			proposalInfoField = accessField(AbstractJavaCompletionProposal.class, "fProposalInfo", available);
			proposal = accessField(MemberProposalInfo.class, "fProposal", available);
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
