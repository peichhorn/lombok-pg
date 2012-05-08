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
package lombok.eclipse.handlers;

import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.Eclipse.poss;

import java.util.ArrayList;
import java.util.List;

import lombok.Action;
import lombok.Function;
import lombok.Predicate;
import lombok.core.AnnotationValues;
import lombok.core.handlers.ActionFunctionAndPredicateHandler;
import lombok.core.handlers.ActionFunctionAndPredicateHandler.TemplateData;
import lombok.core.util.As;
import lombok.core.util.Each;
import lombok.eclipse.DeferUntilBuildFieldsAndMethods;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseMethod;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.mangosdk.spi.ProviderFor;

public class HandleActionFunctionAndPredicate {

	/**
	 * Handles the {@link Action} annotation for eclipse.
	 */
	@ProviderFor(EclipseAnnotationHandler.class)
	@DeferUntilBuildFieldsAndMethods
	public static class HandleAction extends EclipseAnnotationHandler<Action> {

		@Override
		public void handle(final AnnotationValues<Action> annotation, final Annotation source, final EclipseNode annotationNode) {
			new HandleActionFunctionAndPredicate().handle(annotation.getInstance().value(), source, annotationNode, "void");
		}
	}

	/**
	 * Handles the {@link Function} annotation for eclipse.
	 */
	@ProviderFor(EclipseAnnotationHandler.class)
	@DeferUntilBuildFieldsAndMethods
	public static class HandleFunction extends EclipseAnnotationHandler<Function> {

		@Override
		public void handle(final AnnotationValues<Function> annotation, final Annotation source, final EclipseNode annotationNode) {
			new HandleActionFunctionAndPredicate().handle(annotation.getInstance().value(), source, annotationNode, null);
		}
	}

	/**
	 * Handles the {@link Predicate} annotation for eclipse.
	 */
	@ProviderFor(EclipseAnnotationHandler.class)
	@DeferUntilBuildFieldsAndMethods
	public static class HandlePredicate extends EclipseAnnotationHandler<Predicate> {

		@Override
		public void handle(final AnnotationValues<Predicate> annotation, final Annotation source, final EclipseNode annotationNode) {
			new HandleActionFunctionAndPredicate().handle(annotation.getInstance().value(), source, annotationNode, "boolean");
		}
	}

	public void handle(final Class<?> templates, final Annotation source, final EclipseNode annotationNode, final String forcedReturnType) {
		final TypeReference annotationType = source.type;
		final EclipseMethod method = EclipseMethod.methodOf(annotationNode, source);
		if (method.isAbstract()) {
			annotationNode.addError(String.format("@%s can be used on concrete methods only", annotationType));
			return;
		}
		if ((forcedReturnType != null) && !method.returns(forcedReturnType)) {
			annotationNode.addError(String.format("@%s can only be used on methods with '%s' as return type", annotationType, forcedReturnType));
			return;
		}

		final ReferenceBinding resolvedTemplates = resolveTemplates(method.node(), source, templates);
		if (resolvedTemplates == null) {
			annotationNode.addError(String.format("@%s unable to resolve template type", annotationType));
			return;
		}
		final List<TemplateData> matchingTemplates = findTemplatesFor(method.get(), resolvedTemplates, forcedReturnType);
		if (matchingTemplates.isEmpty()) {
			annotationNode.addError(String.format("@%s no template found that matches the given method signature", annotationType));
			return;
		}
		if (matchingTemplates.size() > 1) {
			annotationNode.addError(String.format("@%s more than one template found that matches the given method signature", annotationType));
			return;
		}
		new ActionFunctionAndPredicateHandler<EclipseType, EclipseMethod>().rebuildMethod(method, matchingTemplates.get(0), new EclipseParameterValidator(), new EclipseParameterSanitizer());

	}

	private ReferenceBinding resolveTemplates(final EclipseNode node, final Annotation annotation, final Class<?> templatesDef) {
		final EclipseType type = EclipseType.typeOf(node, annotation);
		final BlockScope blockScope = type.get().initializerScope;
		final char[][] typeNameTokens = fromQualifiedName(templatesDef.getName());
		final TypeReference typeRef = new QualifiedTypeReference(typeNameTokens, poss(annotation, typeNameTokens.length));
		return (ReferenceBinding) typeRef.resolveType(blockScope);
	}

	private List<TemplateData> findTemplatesFor(final AbstractMethodDeclaration methodDecl, final ReferenceBinding template, final String forcedReturnType) {
		final List<TemplateData> foundTemplates = new ArrayList<TemplateData>();
		final TemplateData templateData = templateDataFor(methodDecl, template, forcedReturnType);
		if (templateData != null) foundTemplates.add(templateData);
		for (ReferenceBinding memberType : Each.elementIn(template.memberTypes())) {
			if (!template.isInterface() && !memberType.isStatic()) continue;
			foundTemplates.addAll(findTemplatesFor(methodDecl, memberType, forcedReturnType));
		}
		return foundTemplates;
	}

	private TemplateData templateDataFor(final AbstractMethodDeclaration methodDecl, final ReferenceBinding template, final String forcedReturnType) {
		if (!template.isPublic()) return null;
		if (!template.isInterface() && !template.isAbstract()) return null;
		final List<TypeVariableBinding> templateTypeArguments = As.list(template.typeVariables());
		final List<MethodBinding> enclosedMethods = enclosedMethodsOf(template);
		if (enclosedMethods.size() != 1) return null;
		final MethodBinding enclosedMethod = enclosedMethods.get(0);
		if (!matchesReturnType(enclosedMethod, forcedReturnType)) return null;
		final List<TypeBinding> methodTypeArguments = As.list(enclosedMethod.parameters);
		if (forcedReturnType == null) methodTypeArguments.add(enclosedMethod.returnType);
		if (!templateTypeArguments.equals(methodTypeArguments)) return null;
		if (forcedReturnType == null) {
			if ((numberOfParameters(methodDecl) + 1) != templateTypeArguments.size()) return null;
		} else {
			if (numberOfParameters(methodDecl) != templateTypeArguments.size()) return null;
		}
		return new TemplateData(qualifiedName(template), As.string(enclosedMethod.selector), forcedReturnType);
	}

	// for now only works for void or boolean
	private boolean matchesReturnType(final MethodBinding method, final String forcedReturnType) {
		if (forcedReturnType == null) return true;
		if ("void".equals(forcedReturnType)) return method.returnType.id == TypeIds.T_void;
		if ("boolean".equals(forcedReturnType)) return method.returnType.id == TypeIds.T_boolean;
		return false;
	}

	private int numberOfParameters(final AbstractMethodDeclaration methodDecl) {
		int numberOfParameters = 0;
		for (Argument param : Each.elementIn(methodDecl.arguments)) {
			if (!As.string(param.name).startsWith("_")) numberOfParameters++;
		}
		return numberOfParameters;
	}

	private String qualifiedName(final TypeBinding typeBinding) {
		String qualifiedName = As.string(typeBinding.qualifiedPackageName());
		if (!qualifiedName.isEmpty()) qualifiedName += ".";
		qualifiedName += As.string(typeBinding.qualifiedSourceName());
		return qualifiedName;
	}

	private List<MethodBinding> enclosedMethodsOf(final TypeBinding type) {
		final List<MethodBinding> enclosedMethods = new ArrayList<MethodBinding>();
		if (type instanceof ReferenceBinding) {
			ReferenceBinding rb = (ReferenceBinding) type;
			for (MethodBinding enclosedElement : Each.elementIn(rb.availableMethods())) {
				if (!enclosedElement.isAbstract()) continue;
				enclosedMethods.add(enclosedElement);
			}
		}
		return enclosedMethods;
	}
}
