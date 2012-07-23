/*
 * Copyright © 2012 Philipp Eichhorn
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
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;

public final class EclipseFieldEditor implements lombok.ast.IFieldEditor<ASTNode> {
	private final EclipseField field;
	private final EclipseASTMaker builder;

	EclipseFieldEditor(final EclipseField field, final ASTNode source) {
		this.field = field;
		builder = new EclipseASTMaker(field.node(), source);
	}

	FieldDeclaration get() {
		return field.get();
	}

	public <T extends ASTNode> T build(final lombok.ast.Node<?> node) {
		return builder.<T> build(node);
	}

	public <T extends ASTNode> T build(final lombok.ast.Node<?> node, final Class<T> extectedType) {
		return builder.build(node, extectedType);
	}

	public <T extends ASTNode> List<T> build(final List<? extends lombok.ast.Node<?>> nodes) {
		return builder.build(nodes);
	}

	public <T extends ASTNode> List<T> build(final List<? extends lombok.ast.Node<?>> nodes, final Class<T> extectedType) {
		return builder.build(nodes, extectedType);
	}

	public void replaceInitialization(lombok.ast.Expression<?> initialization) {
		get().initialization = (initialization == null) ? null : build(initialization.posHint(get().initialization), Expression.class);
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

	public void makeNonFinal() {
		get().modifiers &= ~AccFinal;
	}

	@Override
	public String toString() {
		return get().toString();
	}
}
