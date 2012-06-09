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

import java.util.List;
import java.util.regex.Pattern;

import lombok.core.LombokNode;

public interface IField<LOMBOK_NODE_TYPE extends LombokNode<?, ?, ?>, AST_BASE_TYPE, AST_VARIABLE_DECL_TYPE> {
	public <T extends AST_BASE_TYPE> T build(Node<?> node);

	public <T extends AST_BASE_TYPE> T build(Node<?> node, Class<T> extectedType);

	public <T extends AST_BASE_TYPE> List<T> build(List<? extends Node<?>> nodes);

	public <T extends AST_BASE_TYPE> List<T> build(List<? extends Node<?>> nodes, Class<T> extectedType);

	public boolean isPrivate();

	public boolean isFinal();

	public boolean isStatic();

	public boolean isInitialized();

	public boolean isPrimitive();

	public AST_VARIABLE_DECL_TYPE get();

	public LOMBOK_NODE_TYPE node();

	public TypeRef type();

	public TypeRef boxedType();

	public boolean isOfType(final String typeName);

	public String name();

	public Expression<?> initialization();

	public void replaceInitialization(Expression<?> initialization);

	public void makePrivate();

	public void makePackagePrivate();

	public void makeProtected();

	public void makePublic();

	public void makeNonFinal();

	public List<TypeRef> typeArguments();

	public List<Annotation> annotations();

	public List<Annotation> annotations(final Pattern namePattern);
}
