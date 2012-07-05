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
package lombok.javac.handlers.replace;

import lombok.*;
import lombok.javac.handlers.ast.JavacMethod;

import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ReplaceVisitor<NODE_TYPE extends JCTree> extends TreeScanner<Void, Void> {
	private final JavacMethod method;
	private final lombok.ast.Statement<?> replacement;

	public void visit(final JCTree node) {
		node.accept(this, null);
	}

	protected final List<NODE_TYPE> replace(final List<NODE_TYPE> nodes) {
		ListBuffer<NODE_TYPE> newNodes = ListBuffer.lb();
		for (NODE_TYPE node : nodes) {
			if (needsReplacing(node)) {
				node = method.editor().<NODE_TYPE> build(replacement);
			}
			newNodes.append(node);
		}
		return newNodes.toList();
	}

	protected final NODE_TYPE replace(final NODE_TYPE node) {
		if ((node != null) && needsReplacing(node)) {
			return method.editor().<NODE_TYPE> build(replacement);
		}
		return node;
	}

	protected abstract boolean needsReplacing(NODE_TYPE node);
}
