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
package lombok.eclipse.handlers.ast;

import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;
import static lombok.ast.AST.*;

import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;

import lombok.eclipse.EclipseNode;

public final class EclipseField implements lombok.ast.IField<EclipseNode, ASTNode, FieldDeclaration> {
	private final EclipseNode fieldNode;
	private final EclipseASTMaker builder;

	private EclipseField(final EclipseNode fieldNode, final ASTNode source) {
		if (!(fieldNode.get() instanceof FieldDeclaration)) {
			throw new IllegalArgumentException();
		}
		this.fieldNode = fieldNode;
		builder = new EclipseASTMaker(fieldNode, source);
	}

	public <T extends ASTNode> T build(final lombok.ast.Node node) {
		return builder.<T> build(node);
	}

	public <T extends ASTNode> T build(final lombok.ast.Node node, final Class<T> extectedType) {
		return builder.build(node, extectedType);
	}

	public <T extends ASTNode> List<T> build(final List<? extends lombok.ast.Node> nodes) {
		return builder.build(nodes);
	}

	public <T extends ASTNode> List<T> build(final List<? extends lombok.ast.Node> nodes, final Class<T> extectedType) {
		return builder.build(nodes, extectedType);
	}

	public boolean isFinal() {
		return (get().modifiers & AccFinal) != 0;
	}

	public boolean isStatic() {
		return (get().modifiers & AccStatic) != 0;
	}

	public FieldDeclaration get() {
		return (FieldDeclaration) fieldNode.get();
	}

	public EclipseNode node() {
		return fieldNode;
	}

	public lombok.ast.TypeRef type() {
		return Type(get().type);
	}

	public String name() {
		return node().getName();
	}

	public lombok.ast.Expression initialization() {
		return get().initialization == null ? null : Expr(get().initialization);
	}

	public void makePrivate() {
		makePackagePrivate();
		get().modifiers |= AccPrivate;
	}

	public void makePackagePrivate() {
		get().modifiers &= ~(AccPrivate | AccProtected | AccPublic);
	}

	public void makeProtected() {
		makePackagePrivate();
		get().modifiers |= AccProtected;
	}

	public void makePublic() {
		makePackagePrivate();
		get().modifiers |= AccPublic;
	}

	@Override
	public String toString() {
		return get().toString();
	}

	public static EclipseField fieldOf(final EclipseNode node, final ASTNode source) {
		EclipseNode fieldNode = node;
		while ((fieldNode != null) && !(fieldNode.get() instanceof FieldDeclaration)) {
			fieldNode = fieldNode.up();
		}
		return fieldNode == null ? null : new EclipseField(fieldNode, source);
	}
}
