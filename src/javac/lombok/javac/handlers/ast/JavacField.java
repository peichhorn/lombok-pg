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
import static lombok.javac.handlers.Javac.matchesType;
import static lombok.javac.handlers.JavacHandlerUtil.createAnnotation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.util.As;
import lombok.core.util.Names;
import lombok.experimental.Accessors;
import lombok.javac.JavacNode;
import lombok.javac.Javac;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

public final class JavacField implements lombok.ast.IField<JavacType, JavacNode, JCTree, JCVariableDecl> {
	private final JavacNode fieldNode;
	private final JCTree source;
	private final JavacFieldEditor editor;
	private final String filteredName;

	private JavacField(final JavacNode fieldNode, final JCTree source) {
		if (!(fieldNode.get() instanceof JCVariableDecl)) {
			throw new IllegalArgumentException();
		}
		this.fieldNode = fieldNode;
		this.source = source;
		editor = new JavacFieldEditor(this, source);
		final AnnotationValues<Accessors> accessorsValues;
		if (getAnnotation(Accessors.class) != null) {
			accessorsValues = getAnnotationValue(Accessors.class);
		} else {
			final JavacType surroundingType = surroundingType();
			accessorsValues = surroundingType.getAnnotationValue(Accessors.class);
		}
		filteredName = Names.removePrefix(name(), accessorsValues.getInstance().prefix());
	}

	public JavacFieldEditor editor() {
		return editor;
	}

	public boolean isPrivate() {
		return (get().mods.flags & PRIVATE) != 0;
	}

	public boolean isFinal() {
		return (get().mods.flags & FINAL) != 0;
	}

	public boolean isStatic() {
		return (get().mods.flags & STATIC) != 0;
	}

	public boolean isInitialized() {
		return get().init != null;
	}

	public boolean isPrimitive() {
		return Javac.isPrimitive(get().vartype);
	}

	public boolean hasJavaDoc() {
		final JCCompilationUnit compilationUnit = (JCCompilationUnit) fieldNode.top().get();
		return compilationUnit.docComments.get(get()) != null;
	}

	public JCVariableDecl get() {
		return (JCVariableDecl) fieldNode.get();
	}

	public JavacNode node() {
		return fieldNode;
	}

	public boolean ignore() {
		return filteredName == null;
	}

	public <A extends java.lang.annotation.Annotation> AnnotationValues<A> getAnnotationValue(final Class<A> expectedType) {
		final JavacNode node = getAnnotation(expectedType);
		return node == null ? AnnotationValues.of(expectedType, node()) : createAnnotation(expectedType, node);
	}

	public JavacNode getAnnotation(final Class<? extends java.lang.annotation.Annotation> expectedType) {
		return getAnnotation(expectedType.getName());
	}

	public JavacNode getAnnotation(final String typeName) {
		JavacNode annotationNode = null;
		for (JavacNode child : node().down()) {
			if (child.getKind() != Kind.ANNOTATION) continue;
			if (matchesType((JCAnnotation) child.get(), typeName)) {
				annotationNode = child;
			}
		}
		return annotationNode;
	}

	public lombok.ast.TypeRef type() {
		return Type(get().vartype);
	}

	public lombok.ast.TypeRef boxedType() {
		return JavacASTUtil.boxedType(get().vartype);
	}

	public boolean isOfType(final String typeName) {
		final JCExpression variableType = get().vartype;
		if (variableType == null) return false;
		final String type;
		if (variableType instanceof JCTypeApply) {
			type = ((JCTypeApply) variableType).clazz.toString();
		} else {
			type = variableType.toString();
		}
		return type.endsWith(typeName);
	}

	public String name() {
		return node().getName();
	}

	public String filteredName() {
		return filteredName == null ? name() : filteredName;
	}

	public JavacType surroundingType() {
		return JavacType.typeOf(node(), source);
	}

	public lombok.ast.Expression<?> initialization() {
		return get().init == null ? null : Expr(get().init);
	}

	public List<lombok.ast.TypeRef> typeArguments() {
		final List<lombok.ast.TypeRef> typeArguments = new ArrayList<lombok.ast.TypeRef>();
		final JCExpression type = get().vartype;
		if (type instanceof JCTypeApply) {
			JCTypeApply typeRef = (JCTypeApply) type;
			for (JCExpression typeArgument : typeRef.arguments) {
				typeArguments.add(Type(As.string(typeArgument)));
			}
		}
		return typeArguments;
	}

	public List<lombok.ast.Annotation> annotations() {
		return annotations(null);
	}

	public List<lombok.ast.Annotation> annotations(final Pattern namePattern) {
		List<lombok.ast.Annotation> result = new ArrayList<lombok.ast.Annotation>();
		for (JCAnnotation annotation : get().mods.annotations) {
			String name = annotation.annotationType.toString();
			int idx = name.lastIndexOf(".");
			String suspect = idx == -1 ? name : name.substring(idx + 1);
			if ((namePattern == null) || namePattern.matcher(suspect).matches()) {
				lombok.ast.Annotation ann = Annotation(Type(annotation.annotationType));
				for (JCExpression arg : annotation.args) {
					if (arg instanceof JCAssign) {
						JCAssign assign = (JCAssign) arg;
						ann.withValue(assign.lhs.toString(), Expr(assign.rhs));
					} else {
						ann.withValue(Expr(arg));
					}
				}
				result.add(ann);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return get().toString();
	}

	public static JavacField fieldOf(final JavacNode node, final JCTree source) {
		JavacNode fieldNode = node;
		while ((fieldNode != null) && !(fieldNode.get() instanceof JCVariableDecl)) {
			fieldNode = fieldNode.up();
		}
		return fieldNode == null ? null : new JavacField(fieldNode, source);
	}
}
