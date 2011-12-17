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
package lombok.javac.handlers;

import static lombok.javac.handlers.JavacHandlerUtil.deleteAnnotationIfNeccessary;
import static lombok.javac.handlers.ast.JavacResolver.CLASS;

import java.util.ArrayList;
import java.util.List;

import org.mangosdk.spi.ProviderFor;

import lombok.Action;
import lombok.Function;
import lombok.Predicate;
import lombok.core.AnnotationValues;
import lombok.core.handlers.ActionFunctionAndPredicateHandler;
import lombok.core.handlers.ActionFunctionAndPredicateHandler.TemplateData;
import lombok.core.util.As;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.ResolutionBased;
import lombok.javac.handlers.ast.JavacMethod;
import lombok.javac.handlers.ast.JavacType;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

public class HandleActionFunctionAndPredicate {

	/**
	 * Handles the {@link Action} annotation for javac.
	 */
	@ProviderFor(JavacAnnotationHandler.class)
	@ResolutionBased
	public static class HandleAction extends JavacAnnotationHandler<Action> {

		@Override
		public void handle(final AnnotationValues<Action> annotation, final JCAnnotation source, final JavacNode annotationNode) {
			deleteAnnotationIfNeccessary(annotationNode, Action.class);
			new HandleActionFunctionAndPredicate().handle(annotation, source, annotationNode, "void");
		}
	}

	/**
	 * Handles the {@link Function} annotation for javac.
	 */
	@ProviderFor(JavacAnnotationHandler.class)
	@ResolutionBased
	public static class HandleFunction extends JavacAnnotationHandler<Function> {

		@Override
		public void handle(final AnnotationValues<Function> annotation, final JCAnnotation source, final JavacNode annotationNode) {
			deleteAnnotationIfNeccessary(annotationNode, Function.class);
			new HandleActionFunctionAndPredicate().handle(annotation, source, annotationNode, null);
		}
	}

	/**
	 * Handles the {@link Predicate} annotation for javac.
	 */
	@ProviderFor(JavacAnnotationHandler.class)
	@ResolutionBased
	public static class HandlePredicate extends JavacAnnotationHandler<Predicate> {

		@Override
		public void handle(final AnnotationValues<Predicate> annotation, final JCAnnotation source, final JavacNode annotationNode) {
			deleteAnnotationIfNeccessary(annotationNode, Predicate.class);
			new HandleActionFunctionAndPredicate().handle(annotation, source, annotationNode, "boolean");
		}
	}

	public void handle(final AnnotationValues<? extends java.lang.annotation.Annotation> annotation, final JCAnnotation source, final JavacNode annotationNode, final String forcedReturnType) {
		final JCTree annotationType = source.annotationType;
		final JavacMethod method = JavacMethod.methodOf(annotationNode, source);
		if (method.isAbstract()) {
			annotationNode.addError(String.format("@%s can be used on concrete methods only", annotationType));
			return;
		}
		if ((forcedReturnType != null) && !method.returns(forcedReturnType)) {
			annotationNode.addError(String.format("@%s can only be used on methods with '%s' as return type", annotationType, forcedReturnType));
			return;
		}

		final Object templates = annotation.getActualExpression("value");
		final TypeSymbol resolvedTemplates = resolveTemplates(method.node(), source, templates);
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
		new ActionFunctionAndPredicateHandler<JavacType, JavacMethod>().rebuildMethod(method, matchingTemplates.get(0), new JavacParameterValidator(), new JavacParameterSanitizer());
	}

	private TypeSymbol resolveTemplates(final JavacNode node, final JCAnnotation annotation, final Object templatesDef) {
		if (templatesDef instanceof JCFieldAccess) {
			final JCFieldAccess templates = (JCFieldAccess) templatesDef;
			if (!"class".equals(As.string(templates.name))) return null;
			final Type templatesType = CLASS.resolveMember(node, templates.selected);
			return (templatesType == null) ? null : templatesType.asElement();
		} else {
			final Type annotationType = CLASS.resolveMember(node, (JCExpression) annotation.annotationType);
			if (annotationType == null) return null;
			final List<MethodSymbol> enclosedMethods = enclosedMethodsOf(annotationType.asElement());
			if (enclosedMethods.size() != 1) return null;
			final Attribute.Class defaultValue = (Attribute.Class) enclosedMethods.get(0).getDefaultValue();
			return defaultValue.getValue().asElement();
		}
	}

	private List<TemplateData> findTemplatesFor(final JCMethodDecl methodDecl, final TypeSymbol template, final String forcedReturnType) {
		final List<TemplateData> foundTemplates = new ArrayList<TemplateData>();
		final TemplateData templateData = templateDataFor(methodDecl, template, forcedReturnType);
		if (templateData != null) foundTemplates.add(templateData);
		for (Symbol enclosedElement : template.getEnclosedElements()) {
			if (!(enclosedElement instanceof TypeSymbol)) continue;
			final TypeSymbol enclosedType = (TypeSymbol) enclosedElement;
			if (!enclosedType.isInterface() && !enclosedType.isStatic()) continue;
			foundTemplates.addAll(findTemplatesFor(methodDecl, enclosedType, forcedReturnType));
		}
		return foundTemplates;
	}

	private TemplateData templateDataFor(final JCMethodDecl methodDecl, final TypeSymbol template, final String forcedReturnType) {
		if ((template.flags() & (Flags.PUBLIC)) == 0) return null;
		if (!template.isInterface() && ((template.flags() & (Flags.ABSTRACT)) == 0)) return null;
		final List<Type> templateTypeArguments = new ArrayList<Type>(template.type.getTypeArguments());
		final List<MethodSymbol> enclosedMethods = enclosedMethodsOf(template);
		if (enclosedMethods.size() != 1) return null;
		final MethodSymbol enclosedMethod = enclosedMethods.get(0);
		final Type enclosedMethodType = enclosedMethod.type;
		if (!matchesReturnType(enclosedMethodType, forcedReturnType)) return null;
		final List<Type> methodTypeArguments = new ArrayList<Type>(enclosedMethodType.getParameterTypes());
		if (forcedReturnType == null) methodTypeArguments.add(enclosedMethodType.getReturnType());
		if (!templateTypeArguments.equals(methodTypeArguments)) return null;
		if (forcedReturnType == null) {
			if ((numberOfParameters(methodDecl) + 1) != templateTypeArguments.size()) return null;
		} else {
			if (numberOfParameters(methodDecl) != templateTypeArguments.size()) return null;
		}
		return new TemplateData(As.string(template.getQualifiedName()), As.string(enclosedMethod.name), forcedReturnType);
	}

	// for now only works for primitive return types
	private boolean matchesReturnType(final Type methodType, final String forcedReturnType) {
		if (forcedReturnType == null) return true;
		return forcedReturnType.equals(methodType.getReturnType().toString());
	}

	private int numberOfParameters(final JCMethodDecl methodDecl) {
		int numberOfParameters = 0;
		for (JCVariableDecl param : methodDecl.params) {
			if (!As.string(param.name).startsWith("_")) numberOfParameters++;
		}
		return numberOfParameters;
	}

	private List<MethodSymbol> enclosedMethodsOf(final TypeSymbol type) {
		final List<MethodSymbol> enclosedMethods = new ArrayList<MethodSymbol>();
		for (Symbol enclosedElement : type.getEnclosedElements()) {
			if (enclosedElement instanceof MethodSymbol) {
				if ((enclosedElement.flags() & (Flags.ABSTRACT)) == 0) continue;
				enclosedMethods.add((MethodSymbol) enclosedElement);
			}
		}
		return enclosedMethods;
	}
}
