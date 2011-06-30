/*
 * Copyright © 2010-2011 Philipp Eichhorn
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

import static com.sun.tools.javac.code.Flags.ABSTRACT;
import static com.sun.tools.javac.code.Flags.INTERFACE;
import static com.sun.tools.javac.code.Flags.IPROXY;
import static com.sun.tools.javac.code.Kinds.MTH;
import static com.sun.tools.javac.code.TypeTags.CLASS;
import static lombok.core.util.ErrorMessages.canBeUsedOnClassAndEnumOnly;
import static lombok.javac.handlers.Javac.typeNodeOf;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static lombok.ast.AST.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import lombok.AccessLevel;
import lombok.AutoGenMethodStub;
import lombok.RequiredArgsConstructor;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacType;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

/**
 * Handles the {@code lombok.AutoGenMethodStub} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleAutoGenMethodStub extends JavacAnnotationHandler<AutoGenMethodStub> {
	// TODO scan for lombok annotations that come after @AutoGenMethodStub and print a warning that @AutoGenMethodStub
	// should be the last annotation to avoid major issues, once again.. curve ball
	@Override public void handle(AnnotationValues<AutoGenMethodStub> annotation, JCAnnotation source, JavacNode annotationNode) {
		deleteAnnotationIfNeccessary(annotationNode, AutoGenMethodStub.class);
		JavacNode typeNode = annotationNode.up();

		final JavacType type = JavacType.typeOf(annotationNode, source);
		if (type.isInterface() || type.isAnnotation()) {
			annotationNode.addError(canBeUsedOnClassAndEnumOnly(AutoGenMethodStub.class));
			return;
		}

		AutoGenMethodStub autoGenMethodStub = annotation.getInstance();
		if (autoGenMethodStub.throwException()) {
			for (MethodSymbol methodSymbol : UndefiniedMethods.of(typeNode)) {
				type.injectMethod(MethodDecl(methodSymbol).implementing().withStatement(Throw(New(Type("java.lang.UnsupportedOperationException")).withArgument(String("This method is not implemented yet.")))));
			}
		} else {
			for (MethodSymbol methodSymbol : UndefiniedMethods.of(typeNode)) {
				type.injectMethod(MethodDecl(methodSymbol).implementing().withStatement(ReturnDefault()));
			}
		}

		typeNode.rebuild();
	}

	@Override
	public boolean isResolutionBased() {
		return true;
	}

	@RequiredArgsConstructor(access=AccessLevel.PRIVATE)
	private static class UndefiniedMethods implements Iterator<MethodSymbol>, Iterable<MethodSymbol> {
		private final Set<String> handledMethods = new HashSet<String>();
		private final ClassSymbol classSymbol;
		private final Types types;
		private boolean hasNext;
		private boolean nextDefined;
		private MethodSymbol next;

		@Override
		public Iterator<MethodSymbol> iterator() {
			return this;
		}

		@Override
		public boolean hasNext() {
			if (!nextDefined) {
				hasNext = getNext();
				nextDefined = true;
			}
			return hasNext;
		}

		@Override
		public MethodSymbol next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			nextDefined = false;
			return next;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		private boolean getNext() {
			MethodSymbol firstUndefinedMethod = getFirstUndefinedMethod(classSymbol);
			if (firstUndefinedMethod != null) {
				next = createMethodStubFor(firstUndefinedMethod);
				handledMethods.add(firstUndefinedMethod.toString());
				return true;
			}
			return false;
		}

		public static UndefiniedMethods of(JavacNode node) {
			JavacNode typeNode = typeNodeOf(node);
			return new UndefiniedMethods(((JCClassDecl)typeNode.get()).sym, Types.instance(typeNode.getAst().getContext()));
		}

		private MethodSymbol createMethodStubFor(MethodSymbol methodSym) {
			MethodType type = (MethodType) methodSym.type;
			Name name = methodSym.name;
			MethodSymbol methodStubSym = new MethodSymbol(methodSym.flags() & ~Flags.ABSTRACT, name, types.memberType(classSymbol.type, methodSym), classSymbol);
			ListBuffer<VarSymbol> paramSyms = new ListBuffer<VarSymbol>();
			int i = 1;
			if (type.argtypes != null) for (Type argType : type.argtypes) {
				paramSyms.append(new VarSymbol(Flags.PARAMETER, Name.fromString(name.table, "arg" + i++), argType, methodStubSym));
			}
			methodStubSym.params = paramSyms.toList();
			return methodStubSym;
		}

		private MethodSymbol getFirstUndefinedMethod(ClassSymbol c) {
			MethodSymbol undef = null;
			// Do not bother to search in classes that are not abstract, since they cannot have abstract members.
			if (c == classSymbol || (c.flags() & (ABSTRACT | INTERFACE)) != 0) {
				Scope s = c.members();
				for (Scope.Entry e = s.elems; undef == null && e != null; e = e.sibling) {
					if (e.sym.kind == MTH && (e.sym.flags() & (ABSTRACT | IPROXY)) == ABSTRACT) {
						MethodSymbol absmeth = (MethodSymbol) e.sym;
						MethodSymbol implmeth = absmeth.implementation(classSymbol, types, true);
						if ((implmeth == null || implmeth == absmeth) && !handledMethods.contains(absmeth.toString())) undef = absmeth;
					}
				}
				if (undef == null) {
					Type st = types.supertype(c.type);
					if (st.tag == CLASS) undef = getFirstUndefinedMethod((ClassSymbol) st.tsym);
				}
				for (List<Type> l = types.interfaces(c.type); undef == null && l.nonEmpty(); l = l.tail) {
					undef = getFirstUndefinedMethod((ClassSymbol) l.head.tsym);
				}
			}
			return undef;
		}
	}
}