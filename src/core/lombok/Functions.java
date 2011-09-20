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
package lombok;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class Functions {

	public static interface Function0<R> {
		public R apply();
	}

	public static interface Function1<T1, R> {
		public R apply(T1 t1);
	}

	public static interface Function2<T1, T2, R> {
		public R apply(T1 t1, T2 t2);
	}

	public static interface Function3<T1, T2, T3, R> {
		public R apply(T1 t1, T2 t2, T3 t3);
	}

	public static interface Function4<T1, T2, T3, T4, R> {
		public R apply(T1 t1, T2 t2, T3 t3, T4 t4);
	}

	public static interface Function5<T1, T2, T3, T4, T5, R> {
		public R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
	}

	public static interface Function6<T1, T2, T3, T4, T5, T6, R> {
		public R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);
	}

	public static interface Function7<T1, T2, T3, T4, T5, T6, T7, R> {
		public R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7);
	}

	public static interface Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> {
		public R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8);
	}
}
