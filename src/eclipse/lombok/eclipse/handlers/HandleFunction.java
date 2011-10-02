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

import static lombok.core.util.Arrays.isNotEmpty;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Lists.list;
import static lombok.core.util.Names.string;
import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.Eclipse.poss;

import java.util.*;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.handlers.FunctionHandler;
import lombok.core.handlers.FunctionHandler.TemplateData;
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
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;

/**
 * Handles the {@link Function} annotation for eclipse.
 */
// @ProviderFor(EclipseAnnotationHandler.class) // TODO
public class HandleFunction extends EclipseAnnotationHandler<Function> {

	@Override
	public void handle(final AnnotationValues<Function> annotation, final Annotation source, final EclipseNode annotationNode) {
		final Class<? extends java.lang.annotation.Annotation> annotationType = Function.class;
		final EclipseMethod method = EclipseMethod.methodOf(annotationNode, source);
		if (method.isAbstract() || method.isEmpty()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return;
		}

		final Class<?> templates = annotation.getInstance().template();
		final ReferenceBinding resolvedTemplates = resolveTemplates(method.node(), source, templates);
		if (resolvedTemplates == null) {
			annotationNode.addError("@Function unable to resolve template type");
			return;
		}
		final List<TemplateData> matchingTemplates = findTemplatesFor(method.get(), resolvedTemplates);
		if (matchingTemplates.isEmpty()) {
			annotationNode.addError("@Function no template found that matches the given method signature");
			return;
		}
		if (matchingTemplates.size() > 1) {
			annotationNode.addError("@Function more than one template found that matches the given method signature");
			return;
		}
		new FunctionHandler<EclipseType, EclipseMethod>().rebuildFunctionMethod(method, matchingTemplates.get(0), new EclipseParameterValidator(), new EclipseParameterSanitizer());
	}

	private ReferenceBinding resolveTemplates(final EclipseNode node, final Annotation annotation, final Class<?> templatesDef) {
		final EclipseType type = EclipseType.typeOf(node, annotation);
		final BlockScope blockScope = type.get().initializerScope;
		final char[][] typeNameTokens = fromQualifiedName(templatesDef.getName());
		final TypeReference typeRef = new QualifiedTypeReference(typeNameTokens, poss(annotation, typeNameTokens.length));
		return (ReferenceBinding) typeRef.resolveType(blockScope);
	}

	private List<TemplateData> findTemplatesFor(final AbstractMethodDeclaration methodDecl, final ReferenceBinding template) {
		final List<TemplateData> foundTemplates = new ArrayList<TemplateData>();
		final TemplateData templateData = templateDataFor(methodDecl, template);
		if (templateData != null) foundTemplates.add(templateData);
		final ReferenceBinding[] memberTypes = template.memberTypes();
		if (isNotEmpty(memberTypes)) for (ReferenceBinding memberType : memberTypes) {
			if (!template.isInterface() && !memberType.isStatic()) continue;
			foundTemplates.addAll(findTemplatesFor(methodDecl, memberType));
		}
		return foundTemplates;
	}

	private TemplateData templateDataFor(final AbstractMethodDeclaration methodDecl, final ReferenceBinding template) {
		if (!template.isPublic()) return null;
		if (!template.isInterface() && !template.isAbstract()) return null;
		final List<TypeVariableBinding> templateTypeArguments = list(template.typeVariables());
		final List<MethodBinding> enclosedMethods = enclosedMethodsOf(template);
		if (enclosedMethods.size() != 1) return null;
		final MethodBinding enclosedMethod = enclosedMethods.get(0);
		final List<TypeBinding> methodTypeArguments = list(enclosedMethod.parameters);
		methodTypeArguments.add(enclosedMethod.returnType);
		if (!templateTypeArguments.equals(methodTypeArguments)) return null;
		if ((numberOfFunctionParameters(methodDecl) + 1) != templateTypeArguments.size()) return null;
		return new TemplateData(qualifiedName(template), string(enclosedMethod.selector));
	}

	private int numberOfFunctionParameters(final AbstractMethodDeclaration methodDecl) {
		int numberOfFunctionParameters = 0;
		if (isNotEmpty(methodDecl.arguments)) for (Argument param : methodDecl.arguments) {
			if (!string(param.name).startsWith("_")) numberOfFunctionParameters++;
		}
		return numberOfFunctionParameters;
	}

	private String qualifiedName(final TypeBinding typeBinding) {
		String qualifiedName = string(typeBinding.qualifiedPackageName());
		if (!qualifiedName.isEmpty()) qualifiedName += ".";
		qualifiedName += string(typeBinding.qualifiedSourceName());
		return qualifiedName;
	}

	private List<MethodBinding> enclosedMethodsOf(final TypeBinding type) {
		final List<MethodBinding> enclosedMethods = new ArrayList<MethodBinding>();
		if (type instanceof ReferenceBinding) {
			ReferenceBinding rb = (ReferenceBinding) type;
			for (MethodBinding enclosedElement : list(rb.availableMethods())) {
				if (!enclosedElement.isAbstract()) continue;
				enclosedMethods.add(enclosedElement);
			}
		}
		return enclosedMethods;
	}
}
