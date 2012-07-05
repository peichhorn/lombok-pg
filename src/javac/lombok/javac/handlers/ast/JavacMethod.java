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
package lombok.javac.handlers.ast;

import static com.sun.tools.javac.code.Flags.*;
import static lombok.ast.AST.*;
import static lombok.ast.IMethod.ArgumentStyle.BOXED_TYPES;
import static lombok.ast.IMethod.ArgumentStyle.INCLUDE_ANNOTATIONS;
import static lombok.javac.handlers.ast.JavacASTUtil.boxedType;
import static lombok.javac.handlers.ast.JavacResolver.METHOD;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.core.AST.Kind;
import lombok.core.util.As;
import lombok.core.util.Is;
import lombok.javac.JavacNode;
import lombok.javac.handlers.Javac;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

public final class JavacMethod implements lombok.ast.IMethod<JavacType, JavacNode, JCTree, JCMethodDecl> {
	private final JavacNode methodNode;
	private final JCTree source;
	private final JavacMethodEditor editor;

	private JavacMethod(final JavacNode methodNode, final JCTree source) {
		if (!(methodNode.get() instanceof JCMethodDecl)) {
			throw new IllegalArgumentException();
		}
		this.methodNode = methodNode;
		this.source = source;
		editor = new JavacMethodEditor(this, source);
	}

	public JavacMethodEditor editor() {
		return editor;
	}

	public lombok.ast.TypeRef returns() {
		return isConstructor() ? null : Type(returnType());
	}

	public lombok.ast.TypeRef boxedReturns() {
		return boxedType(returnType());
	}

	public boolean returns(final Class<?> clazz) {
		return returns(clazz.getSimpleName());
	}

	public boolean returns(final String typeName) {
		final JCExpression returnType = returnType();
		if (returnType == null) return false;
		final String type;
		if (returnType instanceof JCTypeApply) {
			type = ((JCTypeApply) returnType).clazz.toString();
		} else {
			type = returnType.toString();
		}
		return type.endsWith(typeName);
	}

	private JCExpression returnType() {
		return isConstructor() ? null : get().restype;
	}

	public AccessLevel accessLevel() {
		if ((get().mods.flags & PUBLIC) != 0) return AccessLevel.PUBLIC;
		if ((get().mods.flags & PROTECTED) != 0) return AccessLevel.PROTECTED;
		if ((get().mods.flags & PRIVATE) != 0) return AccessLevel.PRIVATE;
		return AccessLevel.PACKAGE;
	}

	public boolean isSynchronized() {
		return (get().mods.flags & SYNCHRONIZED) != 0;
	}

	public boolean isStatic() {
		return (get().mods.flags & STATIC) != 0;
	}

	public boolean isConstructor() {
		return "<init>".equals(methodNode.getName());
	}

	public boolean isAbstract() {
		return (get().mods.flags & ABSTRACT) != 0;
	}

	public boolean isEmpty() {
		return (get().body == null) || get().body.stats.isEmpty();
	}

	public JCMethodDecl get() {
		return (JCMethodDecl) methodNode.get();
	}

	public JavacNode node() {
		return methodNode;
	}

	public JavacNode getAnnotation(final Class<? extends java.lang.annotation.Annotation> expectedType) {
		return getAnnotation(expectedType.getName());
	}

	public JavacNode getAnnotation(final String typeName) {
		JavacNode annotationNode = null;
		for (JavacNode child : node().down()) {
			if (child.getKind() != Kind.ANNOTATION) continue;
			if (Javac.matchesType((JCAnnotation) child.get(), typeName)) {
				annotationNode = child;
			}
		}
		return annotationNode;
	}

	public boolean hasNonFinalArgument() {
		for (JCVariableDecl param : get().params) {
			if ((param.mods == null) || (param.mods.flags & FINAL) == 0) {
				return true;
			}
		}
		return false;
	}

	public boolean hasArguments() {
		return !get().params.isEmpty();
	}

	public String name() {
		return node().getName();
	}

	public JavacType surroundingType() {
		return JavacType.typeOf(node(), source);
	}

	public List<lombok.ast.Statement<?>> statements() {
		final List<lombok.ast.Statement<?>> methodStatements = new ArrayList<lombok.ast.Statement<?>>();
		for (JCStatement statement : get().body.stats) {
			if (isConstructorCall(statement)) continue;
			methodStatements.add(Stat(statement));
		}
		return methodStatements;
	}

	private boolean isConstructorCall(final JCStatement supect) {
		if (!(supect instanceof JCExpressionStatement)) return false;
		final JCExpression supectExpression = ((JCExpressionStatement) supect).expr;
		if (!(supectExpression instanceof JCMethodInvocation)) return false;
		return Is.oneOf(((JCMethodInvocation) supectExpression).meth.toString(), "super", "this");
	}

	public List<lombok.ast.Annotation> annotations() {
		return annotations(get().mods);
	}

	private List<lombok.ast.Annotation> annotations(final JCModifiers mods) {
		final List<lombok.ast.Annotation> annotations = new ArrayList<lombok.ast.Annotation>();
		for (JCAnnotation annotation : mods.annotations) {
			Type type = METHOD.resolveMember(node(), annotation);
			if (type.toString().startsWith("lombok.")) continue;
			lombok.ast.Annotation ann = Annotation(Type(annotation.annotationType));
			for (JCExpression arg : annotation.args) {
				if (arg instanceof JCAssign) {
					JCAssign assign = (JCAssign) arg;
					ann.withValue(assign.lhs.toString(), Expr(assign.rhs));
				} else {
					ann.withValue(Expr(arg));
				}
			}
			annotations.add(ann);
		}
		return annotations;
	}

	public List<lombok.ast.Argument> arguments(final ArgumentStyle... style) {
		final List<ArgumentStyle> styles = As.list(style);
		final List<lombok.ast.Argument> methodArguments = new ArrayList<lombok.ast.Argument>();
		for (JCVariableDecl param : get().params) {
			lombok.ast.TypeRef argType = styles.contains(BOXED_TYPES) ? boxedType(param.vartype) : Type(param.vartype);
			lombok.ast.Argument arg = Arg(argType, As.string(param.name));
			if (styles.contains(INCLUDE_ANNOTATIONS)) arg.withAnnotations(annotations(param.mods));
			methodArguments.add(arg);
		}
		return methodArguments;
	}

	public List<lombok.ast.TypeParam> typeParameters() {
		final List<lombok.ast.TypeParam> typeParameters = new ArrayList<lombok.ast.TypeParam>();
		if (isConstructor()) return typeParameters;
		for (JCTypeParameter typaram : get().typarams) {
			final lombok.ast.TypeParam typeParam = TypeParam(As.string(typaram.name));
			for (JCExpression expr : typaram.bounds) {
				typeParam.withBound(Type(expr));
			}
			typeParameters.add(typeParam);
		}
		return typeParameters;
	}

	public List<lombok.ast.TypeRef> thrownExceptions() {
		final List<lombok.ast.TypeRef> thrownExceptions = new ArrayList<lombok.ast.TypeRef>();
		for (Object thrownException : get().thrown) {
			thrownExceptions.add(Type(thrownException));
		}
		return thrownExceptions;
	}

	@Override
	public String toString() {
		return get().toString();
	}

	public static JavacMethod methodOf(final JavacNode node, final JCTree source) {
		JavacNode methodNode = Javac.methodNodeOf(node);
		return methodNode == null ? null : new JavacMethod(methodNode, source);
	}
}
