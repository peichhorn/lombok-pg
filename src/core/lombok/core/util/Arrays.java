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
package lombok.core.util;

import static java.util.Arrays.copyOf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Arrays {

	public static <T> T[] array(final T...elements) {
		return elements;
	}

	public static <T> T[] copy(final T[] array) {
		return copyOf(array, array.length);
	}

	public static boolean sameSize(final Object[] array1, final Object[] array2) {
		if ((array1 == null) || (array2 == null)) {
			return false;
		} else {
			return array1.length == array2.length;
		}
	}

	public static <T> T[] resize(final T[] array, final int newSize) {
		return copyOf(array, newSize);
	}

	public static boolean isEmpty(final Object[] array) {
		return array == null || array.length == 0;
	}

	public static boolean isNotEmpty(final Object[] array) {
		return array != null && array.length > 0;
	}
}
