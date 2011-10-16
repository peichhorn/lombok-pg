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
package lombok.javac.handlers.ast;

import static com.sun.tools.javac.code.Flags.*;
import static lombok.ast.AST.*;
import static lombok.core.util.Names.string;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.javac.JavacNode;
import lombok.javac.handlers.JavacHandlerUtil;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

public final class JavacField implements lombok.ast.IField<JavacNode, JCTree, JCVariableDecl> {
	private final JavacNode fieldNode;
	private final JavacASTMaker builder;

	private JavacField(final JavacNode fieldNode, final JCTree source) {
		if (!(fieldNode.get() instanceof JCVariableDecl)) {
			throw new IllegalArgumentException();
		}
		this.fieldNode = fieldNode;
		builder = new JavacASTMaker(fieldNode, source);
	}

	public <T extends JCTree> T build(final lombok.ast.Node node) {
		return builder.<T> build(node);
	}

	public <T extends JCTree> T build(final lombok.ast.Node node, final Class<T> extectedType) {
		return builder.build(node, extectedType);
	}

	public <T extends JCTree> List<T> build(final List<? extends lombok.ast.Node> nodes) {
		return builder.build(nodes);
	}

	public <T extends JCTree> List<T> build(final List<? extends lombok.ast.Node> nodes, final Class<T> extectedType) {
		return builder.build(nodes, extectedType);
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
		return JavacHandlerUtil.isPrimitive(get().vartype);
	}

	public JCVariableDecl get() {
		return (JCVariableDecl) fieldNode.get();
	}

	public JavacNode node() {
		return fieldNode;
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

	public lombok.ast.Expression initialization() {
		return get().init == null ? null : Expr(get().init);
	}

	public void replaceInitialization(lombok.ast.Expression initialization) {
		get().init = (initialization == null) ? null : build(initialization, JCExpression.class);
	}

	public void makePrivate() {
		makePackagePrivate();
		get().mods.flags |= PRIVATE;
	}

	public void makePackagePrivate() {
		get().mods.flags &= ~(PRIVATE | PROTECTED | PUBLIC);
	}

	public void makeProtected() {
		makePackagePrivate();
		get().mods.flags |= PROTECTED;
	}

	public void makePublic() {
		makePackagePrivate();
		get().mods.flags |= PUBLIC;
	}

	public void makeNonFinal() {
		get().mods.flags &= ~Flags.FINAL;
	}

	public List<lombok.ast.TypeRef> typeArguments() {
		final List<lombok.ast.TypeRef> typeArguments = new ArrayList<lombok.ast.TypeRef>();
		final JCExpression type = get().vartype;
		if (type instanceof JCTypeApply) {
			JCTypeApply typeRef = (JCTypeApply) type;
			for (JCExpression typeArgument : typeRef.arguments) {
				typeArguments.add(Type(string(typeArgument)));
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
				result.add(Annotation(Type(annotation.annotationType)));
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
