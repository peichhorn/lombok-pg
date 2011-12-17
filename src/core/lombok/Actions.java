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

/**
 * Collection of action templates.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Actions {

	/**
	 * Encapsulates a method that has no parameters and does not return a value
	 */
	public static abstract class Action0 {
		public abstract void apply();
	}

	/**
	 * Encapsulates a method that has a single parameter and does not return a value.
	 * 
	 * @param <T1>
	 *            The type of the parameter of the method that this delegate encapsulates.
	 */
	public static abstract class Action1<T1> {
		public abstract void apply(T1 t1);

		public final Class<?> getParameterType1() {
			return TypeArguments.getClassFor(getClass(), 0);
		}
	}

	/**
	 * Encapsulates a method that has two parameters and does not return a value.
	 * 
	 * @param <T1>
	 *            The type of the first parameter of the method that this delegate encapsulates.
	 * @param <T2>
	 *            The type of the second parameter of the method that this delegate encapsulates.
	 */
	public static abstract class Action2<T1, T2> {
		public abstract void apply(T1 t1, T2 t2);

		public final Class<?> getParameterType1() {
			return TypeArguments.getClassFor(getClass(), 0);
		}

		public final Class<?> getParameterType2() {
			return TypeArguments.getClassFor(getClass(), 1);
		}
	}

	/**
	 * Encapsulates a method that has three parameters and does not return a value.
	 * 
	 * @param <T1>
	 *            The type of the first parameter of the method that this delegate encapsulates.
	 * @param <T2>
	 *            The type of the second parameter of the method that this delegate encapsulates.
	 * @param <T3>
	 *            The type of the third parameter of the method that this delegate encapsulates.
	 */
	public static abstract class Action3<T1, T2, T3> {
		public abstract void apply(T1 t1, T2 t2, T3 t3);

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

	/**
	 * Encapsulates a method that has four parameters and does not return a value.
	 * 
	 * @param <T1>
	 *            The type of the first parameter of the method that this delegate encapsulates.
	 * @param <T2>
	 *            The type of the second parameter of the method that this delegate encapsulates.
	 * @param <T3>
	 *            The type of the third parameter of the method that this delegate encapsulates.
	 * @param <T4>
	 *            The type of the four parameter of the method that this delegate encapsulates.
	 */
	public static abstract class Action4<T1, T2, T3, T4> {
		public abstract void apply(T1 t1, T2 t2, T3 t3, T4 t4);

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

	/**
	 * Encapsulates a method that has five parameters and does not return a value.
	 * 
	 * @param <T1>
	 *            The type of the first parameter of the method that this delegate encapsulates.
	 * @param <T2>
	 *            The type of the second parameter of the method that this delegate encapsulates.
	 * @param <T3>
	 *            The type of the third parameter of the method that this delegate encapsulates.
	 * @param <T4>
	 *            The type of the four parameter of the method that this delegate encapsulates.
	 * @param <T5>
	 *            The type of the five parameter of the method that this delegate encapsulates.
	 */
	public static abstract class Action5<T1, T2, T3, T4, T5> {
		public abstract void apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);

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

	/**
	 * Encapsulates a method that has six parameters and does not return a value.
	 * 
	 * @param <T1>
	 *            The type of the first parameter of the method that this delegate encapsulates.
	 * @param <T2>
	 *            The type of the second parameter of the method that this delegate encapsulates.
	 * @param <T3>
	 *            The type of the third parameter of the method that this delegate encapsulates.
	 * @param <T4>
	 *            The type of the four parameter of the method that this delegate encapsulates.
	 * @param <T5>
	 *            The type of the five parameter of the method that this delegate encapsulates.
	 * @param <T6>
	 *            The type of the six parameter of the method that this delegate encapsulates.
	 */
	public static abstract class Action6<T1, T2, T3, T4, T5, T6> {
		public abstract void apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);

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

	/**
	 * Encapsulates a method that has seven parameters and does not return a value.
	 * 
	 * @param <T1>
	 *            The type of the first parameter of the method that this delegate encapsulates.
	 * @param <T2>
	 *            The type of the second parameter of the method that this delegate encapsulates.
	 * @param <T3>
	 *            The type of the third parameter of the method that this delegate encapsulates.
	 * @param <T4>
	 *            The type of the four parameter of the method that this delegate encapsulates.
	 * @param <T5>
	 *            The type of the five parameter of the method that this delegate encapsulates.
	 * @param <T6>
	 *            The type of the six parameter of the method that this delegate encapsulates.
	 * @param <T7>
	 *            The type of the seven parameter of the method that this delegate encapsulates.
	 */
	public static abstract class Action7<T1, T2, T3, T4, T5, T6, T7> {
		public abstract void apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7);

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

	/**
	 * Encapsulates a method that has eight parameters and does not return a value.
	 * 
	 * @param <T1>
	 *            The type of the first parameter of the method that this delegate encapsulates.
	 * @param <T2>
	 *            The type of the second parameter of the method that this delegate encapsulates.
	 * @param <T3>
	 *            The type of the third parameter of the method that this delegate encapsulates.
	 * @param <T4>
	 *            The type of the four parameter of the method that this delegate encapsulates.
	 * @param <T5>
	 *            The type of the five parameter of the method that this delegate encapsulates.
	 * @param <T6>
	 *            The type of the six parameter of the method that this delegate encapsulates.
	 * @param <T7>
	 *            The type of the seven parameter of the method that this delegate encapsulates.
	 * @param <T8>
	 *            The type of the eight parameter of the method that this delegate encapsulates.
	 */
	public static abstract class Action8<T1, T2, T3, T4, T5, T6, T7, T8> {
		public abstract void apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8);

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
