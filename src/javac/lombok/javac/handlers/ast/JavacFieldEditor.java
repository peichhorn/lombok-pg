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
package lombok.javac.handlers.ast;

import static com.sun.tools.javac.code.Flags.*;

import java.util.List;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

public final class JavacFieldEditor implements lombok.ast.IFieldEditor<JCTree> {
	private final JavacField field;
	private final JavacASTMaker builder;

	JavacFieldEditor(final JavacField field, final JCTree source) {
		this.field = field;
		builder = new JavacASTMaker(field.node(), source);
	}

	JCVariableDecl get() {
		return field.get();
	}

	public <T extends JCTree> T build(final lombok.ast.Node<?> node) {
		return builder.<T> build(node);
	}

	public <T extends JCTree> T build(final lombok.ast.Node<?> node, final Class<T> extectedType) {
		return builder.build(node, extectedType);
	}

	public <T extends JCTree> List<T> build(final List<? extends lombok.ast.Node<?>> nodes) {
		return builder.build(nodes);
	}

	public <T extends JCTree> List<T> build(final List<? extends lombok.ast.Node<?>> nodes, final Class<T> extectedType) {
		return builder.build(nodes, extectedType);
	}

	public void replaceInitialization(lombok.ast.Expression<?> initialization) {
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

	@Override
	public String toString() {
		return get().toString();
	}
}
