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
package lombok.core.util;

import java.util.*;

import lombok.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Is {

	public static boolean empty(final String s) {
		if (s == null) return true;
		return s.isEmpty();
	}

	public static boolean empty(final Collection<?> collection) {
		return (collection == null) || collection.isEmpty();
	}

	public static boolean empty(final Object[] array) {
		return (array == null) || (array.length == 0);
	}

	public static boolean notEmpty(final String s) {
		return !empty(s);
	}

	public static boolean notEmpty(final Collection<?> collection) {
		return (collection != null) && !collection.isEmpty();
	}

	public static boolean notEmpty(final Object[] array) {
		return (array != null) && (array.length > 0);
	}

	public static boolean oneOf(final String s, final String... candidates) {
		for (String candidate : Each.elementIn(candidates)) {
			if (candidate.equals(s)) return true;
		}
		return false;
	}

	public static boolean oneOf(final Object o, final Class<?>... clazzes) {
		for (Class<?> clazz : Each.elementIn(clazzes)) {
			if (clazz.isInstance(o)) return true;
		}
		return false;
	}

	public static boolean noneOf(final Object o, final Class<?>... clazzes) {
		for (Class<?> clazz : Each.elementIn(clazzes)) {
			if (clazz.isInstance(o)) return false;
		}
		return true;
	}
}
