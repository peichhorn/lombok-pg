/*
 * Copyright © 2010-2011 Philipp Eichhorn
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
package lombok;

public class With {

	/**
	 * All the expressions in the {@code with} statement work on a scope, where the implicit {@code this} gets resolved by the specified
	 * {@code implicitThisExpression}. With the magic symbol {@code _} the {@code implicitThisExpression} can used
	 * explicitly. More importantly the explicit {@code this} is not affected by the {@code with} statement.
	 * <p>
	 * For the time being there are two use-cases:
	 * <p>
	 * 1. Initialization
	 * <pre>
	 * {@code
	 * JFrame frame = with(new JFrame(),
	 *                  setTitle("Application"),
	 *                  setResizable(false),
	 *                  setFrameIcon(this.ICON),
	 *                  setLayout(new BorderLayout()),
	 *                  add(this.createButton(), BorderLayout.SOUTH),
	 *                  pack(),
	 *                  setVisible(true));
	 * }
	 * </pre>
	 * <p>
	 * 2. Chaining non-fluent API
	 * <pre>
	 * {@code
	 * List<String> list1 = new ArrayList<String>();
	 * String s = with(list1,
	 *              add("Hello"),
	 *              add("World")).toString();
	 * }
	 * </pre>
	 * <p>
	 * <b>Note:</b> This is a highly experimental feature. I'm still not sure that I like the way it works right now.
	 */
	public static <T> T with(T implicitThisExpression, Object firstExpression, Object... otherExpressions) {
		return null; // yup, that's about all we need
	}
}
