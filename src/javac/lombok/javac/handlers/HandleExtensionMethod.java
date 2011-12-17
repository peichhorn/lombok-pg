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
import static lombok.javac.handlers.JavacHandlerUtil.deleteAnnotationIfNeccessary;
import static lombok.javac.handlers.ast.JavacResolver.CLASS;
import static lombok.javac.handlers.ast.JavacResolver.CLASS_AND_METHOD;

import java.util.*;

import javax.lang.model.element.ElementKind;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.util.As;
import lombok.core.util.Is;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.ResolutionBased;
import lombok.javac.handlers.ast.JavacType;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ForAll;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.ErrorType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Types;
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

	@Override
	public void handle(final AnnotationValues<ExtensionMethod> annotation, final JCAnnotation source, final JavacNode annotationNode) {
		deleteAnnotationIfNeccessary(annotationNode, ExtensionMethod.class);
		JavacType type = JavacType.typeOf(annotationNode, source);
		if (type.isAnnotation() || type.isInterface()) {
			annotationNode.addError(canBeUsedOnClassAndEnumOnly(ExtensionMethod.class));
			return;
		}

		boolean suppressBaseMethods = annotation.getInstance().suppressBaseMethods();

		List<Object> extensionProviders = annotation.getActualExpressions("value");
		if (extensionProviders.isEmpty()) {
			annotationNode.addError(String.format("@%s has no effect since no extension types were specified.", ExtensionMethod.class.getName()));
			return;
		}

		final List<Extension> extensions = getExtensions(type.node(), extensionProviders);
		if (extensions.isEmpty()) return;

		// call HandleVal explicitly to ensure val gets handled before @ExtensionMethdod gets handled.
		// TODO maybe we should prioritize lombok handler
		type.node().traverse(new HandleVal());

		new ExtensionMethodReplaceVisitor(type, extensions, suppressBaseMethods).replace();

		type.rebuild();
	}

	private List<Extension> getExtensions(final JavacNode typeNode, final List<Object> extensionProviders) {
		List<Extension> extensions = new ArrayList<Extension>();
		for (Object extensionProvider : extensionProviders) {
			if (extensionProvider instanceof JCFieldAccess) {
				JCFieldAccess provider = (JCFieldAccess) extensionProvider;
				if ("class".equals(As.string(provider.name))) {
					Type providerType = CLASS.resolveMember(typeNode, provider.selected);
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

	@RequiredArgsConstructor
	@Getter
	private static class Extension {
		private final List<MethodSymbol> extensionMethods;
		private final TypeSymbol extensionProvider;
	}

	@RequiredArgsConstructor
	private static class ExtensionMethodReplaceVisitor extends TreeScanner<Void, Void> {
		private final JavacType type;
		private final List<Extension> extensions;
		private final boolean suppressBaseMethods;

		public void replace() {
			type.get().accept(this, null);
		}

		@Override
		public Void visitMethodInvocation(final MethodInvocationTree tree, final Void p) {
			handleMethodCall((JCMethodInvocation) tree);
			return super.visitMethodInvocation(tree, p);
		}

		private void handleMethodCall(final JCMethodInvocation methodCall) {
			JavacNode methodCallNode = type.node().getAst().get(methodCall);
			JavacType surroundingType = JavacType.typeOf(methodCallNode, methodCall);
			TypeSymbol surroundingTypeSymbol = surroundingType.get().sym;
			JCExpression receiver = receiverOf(methodCall);
			String methodName = methodNameOf(methodCall);

			if (Is.oneOf(methodName, "this", "super")) return;
			Type resolvedMethodCall = CLASS_AND_METHOD.resolveMember(methodCallNode, methodCall);
			if (resolvedMethodCall == null) return;
			if (!suppressBaseMethods && !(resolvedMethodCall instanceof ErrorType)) return;
			Type receiverType = CLASS_AND_METHOD.resolveMember(methodCallNode, receiver);
			if (receiverType == null) return;
			if (As.string(receiverType.tsym).endsWith(As.string(receiver))) return;

			Types types = Types.instance(type.node().getContext());
			for (Extension extension : extensions) {
				TypeSymbol extensionProvider = extension.getExtensionProvider();
				if (surroundingTypeSymbol == extensionProvider) continue;
				for (MethodSymbol extensionMethod : extension.getExtensionMethods()) {
					if (!methodName.equals(As.string(extensionMethod.name))) continue;
					Type extensionMethodType = extensionMethod.type;
					if (Is.noneOf(extensionMethodType, MethodType.class, ForAll.class)) continue;
					Type firstArgType = types.erasure(extensionMethodType.asMethodType().argtypes.get(0));
					if (!types.isAssignable(receiverType, firstArgType)) continue;
					methodCall.args = methodCall.args.prepend(receiver);
					methodCall.meth = type.build(Call(Name(As.string(extensionProvider)), methodName), JCMethodInvocation.class).meth;
					return;
				}
			}
		}

		private String methodNameOf(final JCMethodInvocation methodCall) {
			if (methodCall.meth instanceof JCIdent) {
				return As.string(((JCIdent) methodCall.meth).name);
			} else {
				return As.string(((JCFieldAccess) methodCall.meth).name);
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
