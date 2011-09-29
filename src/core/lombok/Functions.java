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

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Functions {

	public static abstract class Function0<R> {
		public abstract R apply();

		public final Class<?> getReturnType() {
			return TypeArguments.getClassFor(getClass(), 0);
		}
	}

	public static abstract class Function1<T1, R> {
		public abstract R apply(T1 t1);

		public final Class<?> getReturnType() {
			return TypeArguments.getClassFor(getClass(), 1);
		}

		public final Class<?> getParameterType1() {
			return TypeArguments.getClassFor(getClass(), 0);
		}
	}

	public static abstract class Function2<T1, T2, R> {
		public abstract R apply(T1 t1, T2 t2);

		public final Class<?> getReturnType() {
			return TypeArguments.getClassFor(getClass(), 2);
		}

		public final Class<?> getParameterType1() {
			return TypeArguments.getClassFor(getClass(), 0);
		}

		public final Class<?> getParameterType2() {
			return TypeArguments.getClassFor(getClass(), 1);
		}
	}

	public static abstract class Function3<T1, T2, T3, R> {
		public abstract R apply(T1 t1, T2 t2, T3 t3);

		public final Class<?> getReturnType() {
			return TypeArguments.getClassFor(getClass(), 3);
		}

		public final Class<?> getParameterType1() {
			return TypeArguments.getClassFor(getClass(), 0);
		}

		public final Class<?> getParameterType2() {
			return TypeArguments.getClassFor(getClass(), 1);
		}

		public final Class<?> getParameterType3() {
			return TypeArguments.getClassFor(getClass(), 2);
		}
	}

	public static abstract class Function4<T1, T2, T3, T4, R> {
		public abstract R apply(T1 t1, T2 t2, T3 t3, T4 t4);

		public final Class<?> getReturnType() {
			return TypeArguments.getClassFor(getClass(), 4);
		}

		public final Class<?> getParameterType1() {
			return TypeArguments.getClassFor(getClass(), 0);
		}

		public final Class<?> getParameterType2() {
			return TypeArguments.getClassFor(getClass(), 1);
		}

		public final Class<?> getParameterType3() {
			return TypeArguments.getClassFor(getClass(), 2);
		}

		public final Class<?> getParameterType4() {
			return TypeArguments.getClassFor(getClass(), 3);
		}
	}

	public static abstract class Function5<T1, T2, T3, T4, T5, R> {
		public abstract R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);

		public final Class<?> getReturnType() {
			return TypeArguments.getClassFor(getClass(), 5);
		}

		public final Class<?> getParameterType1() {
			return TypeArguments.getClassFor(getClass(), 0);
		}

		public final Class<?> getParameterType2() {
			return TypeArguments.getClassFor(getClass(), 1);
		}

		public final Class<?> getParameterType3() {
			return TypeArguments.getClassFor(getClass(), 2);
		}

		public final Class<?> getParameterType4() {
			return TypeArguments.getClassFor(getClass(), 3);
		}

		public final Class<?> getParameterType5() {
			return TypeArguments.getClassFor(getClass(), 4);
		}
	}

	public static abstract class Function6<T1, T2, T3, T4, T5, T6, R> {
		public abstract R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);

		public final Class<?> getReturnType() {
			return TypeArguments.getClassFor(getClass(), 6);
		}

		public final Class<?> getParameterType1() {
			return TypeArguments.getClassFor(getClass(), 0);
		}

		public final Class<?> getParameterType2() {
			return TypeArguments.getClassFor(getClass(), 1);
		}

		public final Class<?> getParameterType3() {
			return TypeArguments.getClassFor(getClass(), 2);
		}

		public final Class<?> getParameterType4() {
			return TypeArguments.getClassFor(getClass(), 3);
		}

		public final Class<?> getParameterType5() {
			return TypeArguments.getClassFor(getClass(), 4);
		}

		public final Class<?> getParameterType6() {
			return TypeArguments.getClassFor(getClass(), 5);
		}
	}

	public static abstract class Function7<T1, T2, T3, T4, T5, T6, T7, R> {
		public abstract R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7);

		public final Class<?> getReturnType() {
			return TypeArguments.getClassFor(getClass(), 7);
		}

		public final Class<?> getParameterType1() {
			return TypeArguments.getClassFor(getClass(), 0);
		}

		public final Class<?> getParameterType2() {
			return TypeArguments.getClassFor(getClass(), 1);
		}

		public final Class<?> getParameterType3() {
			return TypeArguments.getClassFor(getClass(), 2);
		}

		public final Class<?> getParameterType4() {
			return TypeArguments.getClassFor(getClass(), 3);
		}

		public final Class<?> getParameterType5() {
			return TypeArguments.getClassFor(getClass(), 4);
		}

		public final Class<?> getParameterType6() {
			return TypeArguments.getClassFor(getClass(), 5);
		}

		public final Class<?> getParameterType7() {
			return TypeArguments.getClassFor(getClass(), 6);
		}
	}

	public static abstract class Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> {
		public abstract R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8);

		public final Class<?> getReturnType() {
			return TypeArguments.getClassFor(getClass(), 8);
		}

		public final Class<?> getParameterType1() {
			return TypeArguments.getClassFor(getClass(), 0);
		}

		public final Class<?> getParameterType2() {
			return TypeArguments.getClassFor(getClass(), 1);
		}

		public final Class<?> getParameterType3() {
			return TypeArguments.getClassFor(getClass(), 2);
		}

		public final Class<?> getParameterType4() {
			return TypeArguments.getClassFor(getClass(), 3);
		}

		public final Class<?> getParameterType5() {
			return TypeArguments.getClassFor(getClass(), 4);
		}

		public final Class<?> getParameterType6() {
			return TypeArguments.getClassFor(getClass(), 5);
		}

		public final Class<?> getParameterType7() {
			return TypeArguments.getClassFor(getClass(), 6);
		}

		public Class<?> getParameterType8() {
			return TypeArguments.getClassFor(getClass(), 7);
		}
	}
}
