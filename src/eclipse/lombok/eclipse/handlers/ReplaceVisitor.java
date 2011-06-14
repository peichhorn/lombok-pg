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
package lombok.eclipse.handlers;

import static lombok.core.util.Arrays.isNotEmpty;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ReplaceVisitor<NODE_TYPE extends ASTNode> extends ASTVisitor {
	private final IReplacementProvider<NODE_TYPE> replacement;

	public void visit(ASTNode astNode) {
		if (astNode instanceof MethodDeclaration) {
			((MethodDeclaration)astNode).traverse(this, (ClassScope)null);
		} else {
			astNode.traverse(this, null);
		}
	}

	protected final void replace(NODE_TYPE[] nodes) {
		if (isNotEmpty(nodes)) for (int i = 0, iend = nodes.length; i < iend; i++) {
			if (needsReplacing(nodes[i])) {
				nodes[i] = replacement.getReplacement();
			}
		}
	}

	protected final NODE_TYPE replace(NODE_TYPE node) {
		if (needsReplacing(node)) {
			return replacement.getReplacement();
		}
		return node;
	}

	protected abstract boolean needsReplacing(NODE_TYPE node);
}