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
package lombok.ast;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import lombok.core.util.Cast;

public abstract class Node<SELF_TYPE extends Node<SELF_TYPE>> {
	private Node<?> parent;
	private Object posHint;

	public final <T extends Node<?>> T child(final T node) {
		if (node != null) node.parent = this;
		return node;
	}

	public final Node<?> up() {
		return parent;
	}

	public final <T extends Node<?>> T upTo(final Class<T> type) {
		Node<?> node = this;
		while ((node != null) && !type.isInstance(node)) {
			node = node.up();
		}
		return type.cast(node);
	}

	protected final SELF_TYPE self() {
		return Cast.<SELF_TYPE> uncheckedCast(this);
	}

	public final SELF_TYPE posHint(final Object posHint) {
		this.posHint = posHint;
		return self();
	}

	public final <T> T posHint() {
		Node<?> node = this;
		while ((node != null) && (node.posHint == null)) {
			node = node.up();
		}
		return node == null ? null : Cast.<T> uncheckedCast(node.posHint);
	}

	public String toString() {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintStream ps = new PrintStream(baos);
		accept(new ASTPrinter(), new ASTPrinter.State(ps));
		return baos.toString();
	}

	public abstract <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p);
}
