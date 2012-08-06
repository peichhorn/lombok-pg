/*
 * Copyright © 2011-2012 Philipp Eichhorn
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

import static lombok.javac.handlers.Javac.deleteImport;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.util.List;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.handlers.BoundSetterHandler;
import lombok.core.util.As;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.ResolutionBased;
import lombok.javac.handlers.ast.JavacField;
import lombok.javac.handlers.ast.JavacType;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;

/**
 * Handles the {@code lombok.BoundSetter} annotation for javac.
 */
@ResolutionBased
@ProviderFor(JavacAnnotationHandler.class)
public class HandleBoundSetter extends JavacAnnotationHandler<BoundSetter> {

	@Override
	public void handle(final AnnotationValues<BoundSetter> annotation, final JCAnnotation ast, final JavacNode annotationNode) {
		BoundSetter annotationInstance = annotation.getInstance();
		new BoundSetterHandler<JavacType, JavacField, JavacNode, JCTree>(annotationNode, ast) {

			@Override
			protected JavacType typeOf(JavacNode node, JCTree ast) {
				return JavacType.typeOf(node, ast);
			}

			@Override
			protected JavacField fieldOf(JavacNode node, JCTree ast) {
				return JavacField.fieldOf(node, ast);
			}

			@Override
			protected boolean hasMethodIncludingSupertypes(final JavacType type, final String methodName, final lombok.ast.TypeRef... argumentTypes) {
				return hasMethod(type.get().sym, methodName, type.editor().build(As.list(argumentTypes)));
			}

			private boolean hasMethod(final TypeSymbol type, final String methodName, final List<JCTree> argumentTypes) {
				if (type == null) return false;
				for (Symbol enclosedElement : type.getEnclosedElements()) {
					if (enclosedElement instanceof MethodSymbol) {
						if ((enclosedElement.flags() & (Flags.ABSTRACT)) != 0) continue;
						if ((enclosedElement.flags() & (Flags.PUBLIC)) == 0) continue;
						MethodSymbol method = (MethodSymbol) enclosedElement;
						if (!methodName.equals(As.string(method.name))) continue;
						MethodType methodType = (MethodType) method.type;
						if (argumentTypes.size() != methodType.argtypes.size()) continue;
						// TODO check actual types..
						return true;
					}
				}
				Type supertype = ((ClassSymbol) type).getSuperclass();
				return hasMethod(supertype.tsym, methodName, argumentTypes);
			}

			@Override
			protected boolean lookForBoundSetter(final JavacType type, final boolean needsToBeVetoable) {
				final TypeSymbol typeSymbol = type.get().sym;
				if (typeSymbol == null) return false;
				Type supertype = ((ClassSymbol) typeSymbol).getSuperclass();
				return lookForBoundSetter0(supertype.tsym, needsToBeVetoable);
			}

			private boolean lookForBoundSetter0(final TypeSymbol type, final boolean needsToBeVetoable) {
				if (type == null) return false;
				if (isAnnotatedWithBoundSetter(type, needsToBeVetoable)) return true;
				for (Symbol enclosedElement : type.getEnclosedElements()) {
					if (enclosedElement instanceof VarSymbol) {
						final VarSymbol var = (VarSymbol) enclosedElement;
						if (isAnnotatedWithBoundSetter(var, needsToBeVetoable)) return true;
					}
				}
				Type supertype = ((ClassSymbol) type).getSuperclass();
				return lookForBoundSetter0(supertype.tsym, needsToBeVetoable);
			}

			private boolean isAnnotatedWithBoundSetter(final Symbol type, final boolean needsToBeVetoable) {
				final BoundSetter boundSetter = JavacElements.getAnnotation(type, BoundSetter.class);
				if (boundSetter == null) return false;
				return needsToBeVetoable ? (boundSetter.vetoable() || boundSetter.throwVetoException()) : true;
			}
		}.handle(annotationInstance.value(), annotationInstance.vetoable(), annotationInstance.throwVetoException());
		deleteAnnotationIfNeccessary(annotationNode, BoundSetter.class);
		deleteImport(annotationNode, AccessLevel.class);
	}
}
