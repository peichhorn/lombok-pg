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
 * 
 * 
 * Credit where credit is due:
 * ===========================
 * 
 *   Yield is based on the idea, algorithms and sources of
 *   Arthur and Vladimir Nesterovsky (http://www.nesterovsky-bros.com/weblog),
 *   who generously allowed me to use them.
 */
package lombok;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Yield {

	/**
	 *  A take on yield return.
	 * <pre>
	 * public Iterable&lt;T&gt; doSomething(Collection&lt;T&gt; entries) {
	 *   for (T entry : entries) {
	 *     if (isMet(cond))
	 *       yield(entry);
	 *     if (isMet(otherCond))
	 *       break;
	 *     yield(transform(entry));
	 *   }
	 * }
	 * </pre>
	 * or:
	 * <pre>
	 * public Iterator&lt;Integer&gt; genFib() {
	 *   long a = 0;
	 *   long b = 0;
	 *   while (b >= 0) {
	 *     yield(a);
	 *     long c = a + b;
	 *     a = b;
	 *     b = c;
	 *   }
	 * }
	 * </pre>
	 * <b>Note:</b> This is an experimental feature and might not work in some cases.
	 * Please file a bug report if you stumble over anything weird.
	 */
	public static <T> void yield(final T value) {
		return; // yup, that's about all we need
	}
}