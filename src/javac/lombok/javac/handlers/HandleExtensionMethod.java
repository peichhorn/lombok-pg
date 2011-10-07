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

import static com.sun.tools.javac.code.Flags.*;
import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.core.util.Types.*;
import static lombok.javac.handlers.JavacHandlerUtil.deleteAnnotationIfNeccessary;

import java.util.*;

import javax.lang.model.element.ElementKind;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.javac.JavacASTAdapter;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacResolution;
import lombok.javac.ResolutionBased;
import lombok.javac.handlers.ast.JavacType;

import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ForAll;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.ErrorType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@link ExtensionMethod} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
@ResolutionBased
public class HandleExtensionMethod extends JavacAnnotationHandler<ExtensionMethod> {

	@Override public void handle(final AnnotationValues<ExtensionMethod> annotation, final JCAnnotation source, final JavacNode annotationNode) {
		final Class<? extends java.lang.annotation.Annotation> annotationType = ExtensionMethod.class;
		deleteAnnotationIfNeccessary(annotationNode, annotationType);
		JavacType type = JavacType.typeOf(annotationNode, source);
		if (type.isAnnotation() || type.isInterface()) {
			annotationNode.addError(canBeUsedOnClassAndEnumOnly(annotationType));
			return;
		}

		List<Object> extensionProviders = annotation.getActualExpressions("value");
		if (extensionProviders.isEmpty()) {
			annotationNode.addError(String.format("@%s has no effect since no extension types were specified.", annotationType.getName()));
			return;
		}
		
		final List<Extension> extensions = getExtensions(type.node(), annotation);
		if (extensions.isEmpty()) return;
		
		new ExtensionMethodReplaceVisitor(type, extensions).replace();
		
		type.rebuild();
	}
	
	private List<Extension> getExtensions(final JavacNode typeNode, final AnnotationValues<ExtensionMethod> annotation) {
		List<Extension> extensions = new ArrayList<Extension>();
		for (Object extensionProvider : annotation.getActualExpressions("value")) {
			if (extensionProvider instanceof JCFieldAccess) {
				JCFieldAccess provider = (JCFieldAccess)extensionProvider;
				if ("class".equals(string(provider.name))) {
					Type providerType = resolveClassMember(typeNode, provider.selected);
					if (providerType == null) continue;
					if ((providerType.tsym.flags() & (INTERFACE | ANNOTATION)) != 0) continue;
					extensions.add(getExtension(typeNode, (ClassType) providerType));
				}
			}
		}
		return extensions;
	}
	
	private Extension getExtension(final JavacNode typeNode, final ClassType extensionMethodProviderType) {
		List<MethodSymbol> extensionMethods = new ArrayList<MethodSymbol>();
		TypeSymbol tsym = extensionMethodProviderType.asElement();
		if (tsym != null) for (Symbol member : tsym.getEnclosedElements()) {
			if (member.getKind() != ElementKind.METHOD) continue;
			MethodSymbol method = (MethodSymbol) member;
			if ((method.flags() & (STATIC | PUBLIC)) == 0) continue;
			if (method.params().isEmpty()) continue;
			extensionMethods.add(method);
		}
		return new Extension(extensionMethods, tsym);
	}

	private static Type resolveClassMember(final JavacNode node, final JCExpression expr) {
		Type type = expr.type;
		if (type == null) {
			new JavacResolution(node.getContext()).resolveClassMember(node);
			type = expr.type;
		}
		return type;
	}
	
	private static Type resolveMethodMember(final JavacNode node, final JCExpression expr) {
		Type type = expr.type;
		if (type == null) {
			JCExpression resolvedExpression = ((JCExpression) new JavacResolution(node.getContext()).resolveMethodMember(node).get(expr));
			if (resolvedExpression != null) type = resolvedExpression.type;
		}
		return type;
	}

	@RequiredArgsConstructor
	@Getter
	private static class Extension {
		private final List<MethodSymbol> extensionMethods;
		private final TypeSymbol extensionProvider;
	}

	private static class ExtensionMethodReplaceVisitor extends JavacASTAdapter {
		private final JavacType type;
		private final List<Extension> extensions;

		public ExtensionMethodReplaceVisitor(final JavacType type, final List<Extension> extensions) {
			super();
			this.type = type;
			this.extensions = extensions;
		}

		public void replace() {
			type.node().traverse(this);
		}

		@Override public void visitStatement(final JavacNode statementNode, final JCTree statement) {
			if (!(statement instanceof JCMethodInvocation)) return;
			JCMethodInvocation methodCall = (JCMethodInvocation) statement;
			JCExpression receiver = receiverOf(methodCall);
			String methodName = methodNameOf(methodCall);
			if (isOneOf(methodName, "this", "super")) return;
			Type resolvedMethodCall = resolveMethodMember(statementNode, methodCall);
			if (!(resolvedMethodCall instanceof ErrorType)) return;
			Type receiverType = resolveMethodMember(statementNode, receiver);
			Types types = Types.instance(type.node().getContext());
			for (Extension extension : extensions) {
				for (MethodSymbol extensionMethod : extension.getExtensionMethods()) {
					if (!methodName.equals(string(extensionMethod.name))) continue;
					Type extensionMethodType = extensionMethod.type;
					if (isNoneOf(extensionMethodType, MethodType.class, ForAll.class)) continue;
					Type firstArgType = types.erasure(extensionMethodType.asMethodType().argtypes.get(0));
					if (!types.isAssignable(receiverType, firstArgType)) continue;
					methodCall.args = methodCall.args.prepend(receiver);
					methodCall.meth = type.build(Call(Name(string(extension.getExtensionProvider())), methodName), JCMethodInvocation.class).meth;
					return;
				}
			}
		}

		private String methodNameOf(final JCMethodInvocation methodCall) {
			if (methodCall.meth instanceof JCIdent) {
				return string(((JCIdent)methodCall.meth).name);
			} else {
				return string(((JCFieldAccess)methodCall.meth).name);
			}
		}

		private JCExpression receiverOf(final JCMethodInvocation methodCall) {
			if (methodCall.meth instanceof JCIdent) {
				return type.build(This());
			} else {
				return ((JCFieldAccess) methodCall.meth).selected;
			}
		}
	}
}
