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
package lombok.eclipse.handlers.replace;

import lombok.*;
import lombok.core.util.Is;
import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ReplaceVisitor<NODE_TYPE extends ASTNode> extends ASTVisitor {
	private final EclipseMethod method;
	private final lombok.ast.Statement replacement;

	public void visit(final ASTNode astNode) {
		if (astNode instanceof MethodDeclaration) {
			((MethodDeclaration)astNode).traverse(this, (ClassScope)null);
		} else {
			astNode.traverse(this, null);
		}
	}

	protected final void replace(final NODE_TYPE[] nodes) {
		if (Is.notEmpty(nodes)) for (int i = 0, iend = nodes.length; i < iend; i++) {
			if (needsReplacing(nodes[i])) {
				nodes[i] = method.<NODE_TYPE>build(replacement);
			}
		}
	}

	protected final NODE_TYPE replace(final NODE_TYPE node) {
		if ((node != null) && needsReplacing(node)) {
			return method.<NODE_TYPE>build(replacement);
		}
		return node;
	}

	protected abstract boolean needsReplacing(NODE_TYPE node);
}