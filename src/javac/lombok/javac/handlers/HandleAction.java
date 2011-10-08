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

import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static lombok.javac.handlers.ast.JavacResolver.CLASS;

import java.util.*;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.handlers.ActionHandler;
import lombok.core.handlers.ActionHandler.TemplateData;
import lombok.javac.Javac;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.ResolutionBased;
import lombok.javac.handlers.ast.JavacMethod;
import lombok.javac.handlers.ast.JavacType;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@link Action} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
@ResolutionBased
public class HandleAction extends JavacAnnotationHandler<Action> {

	@Override
	public void handle(final AnnotationValues<Action> annotation, final JCAnnotation source, final JavacNode annotationNode) {
		final Class<? extends java.lang.annotation.Annotation> annotationType = Action.class;
		deleteAnnotationIfNeccessary(annotationNode, annotationType);
		final JavacMethod method = JavacMethod.methodOf(annotationNode, source);
		if (method.isAbstract()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return;
		}
		if (!method.returns("void")) {
			annotationNode.addError("@Action can be used on methods that don't return a value");
			return;
		}

		final Object templates = annotation.getActualExpression("template");
		final TypeSymbol resolvedTemplates = resolveTemplates(method.node(), source, templates);
		if (resolvedTemplates == null) {
			annotationNode.addError("@Action unable to resolve template type");
			return;
		}
		final List<TemplateData> matchingTemplates = findTemplatesFor(method.get(), resolvedTemplates);
		if (matchingTemplates.isEmpty()) {
			annotationNode.addError("@Action no template found that matches the given method signature");
			return;
		}
		if (matchingTemplates.size() > 1) {
			annotationNode.addError("@Action more than one template found that matches the given method signature");
			return;
		}
		new ActionHandler<JavacType, JavacMethod>().rebuildActionMethod(method, matchingTemplates.get(0), new JavacParameterValidator(), new JavacParameterSanitizer());
	}

	private TypeSymbol resolveTemplates(final JavacNode node, final JCAnnotation annotation, final Object templatesDef) {
		if (templatesDef instanceof JCFieldAccess) {
			final JCFieldAccess templates = (JCFieldAccess) templatesDef;
			if (!"class".equals(string(templates.name))) return null;
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

	private List<TemplateData> findTemplatesFor(final JCMethodDecl methodDecl, final TypeSymbol template) {
		final List<TemplateData> foundTemplates = new ArrayList<TemplateData>();
		final TemplateData templateData = templateDataFor(methodDecl, template);
		if (templateData != null) foundTemplates.add(templateData);
		for (Symbol enclosedElement : template.getEnclosedElements()) {
			if (!(enclosedElement instanceof TypeSymbol)) continue;
			final TypeSymbol enclosedType = (TypeSymbol) enclosedElement;
			if (!enclosedType.isInterface() && !enclosedType.isStatic()) continue;
			foundTemplates.addAll(findTemplatesFor(methodDecl, enclosedType));
		}
		return foundTemplates;
	}

	private TemplateData templateDataFor(final JCMethodDecl methodDecl, final TypeSymbol template) {
		if ((template.flags() & (Flags.PUBLIC)) == 0) return null;
		if (!template.isInterface() && ((template.flags() & (Flags.ABSTRACT)) == 0)) return null;
		final List<Type> templateTypeArguments = new ArrayList<Type>(template.type.getTypeArguments());
		final List<MethodSymbol> enclosedMethods = enclosedMethodsOf(template);
		if (enclosedMethods.size() != 1) return null;
		final MethodSymbol enclosedMethod = enclosedMethods.get(0);
		final Type enclosedMethodType = enclosedMethod.type;
		if (enclosedMethodType.getReturnType().tag != Javac.getCtcInt(TypeTags.class, "VOID")) return null;
		final List<Type> methodTypeArguments = new ArrayList<Type>(enclosedMethodType.getParameterTypes());
		if (!templateTypeArguments.equals(methodTypeArguments)) return null;
		if (numberOfActionParameters(methodDecl) != templateTypeArguments.size()) return null;
		return new TemplateData(string(template.getQualifiedName()), string(enclosedMethod.name));
	}

	private int numberOfActionParameters(final JCMethodDecl methodDecl) {
		int numberOfActionParameters = 0;
		for (JCVariableDecl param : methodDecl.params) {
			if (!string(param.name).startsWith("_")) numberOfActionParameters++;
		}
		return numberOfActionParameters;
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
