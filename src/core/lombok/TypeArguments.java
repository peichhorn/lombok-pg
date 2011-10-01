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

import java.lang.reflect.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TypeArguments {
	/**
	 * Determines the {@link Class} of a type argument of a given class
	 * by using the super type token pattern.
	 */
	public static Class<?> getClassFor(final Class<?> clazz, final int typeArgumentIndex) {
		final Type type = getTypeFor(clazz, typeArgumentIndex);
		final Class<?> result = getClassFor(type);
		return result == null ? Object.class : result;
	}

	private static Type getTypeFor(final Class<?> clazz, final int index) {
		final Type superClass = clazz.getGenericSuperclass();
		if (!(superClass instanceof ParameterizedType))
			return null;
		final Type[] typeArguments = ((ParameterizedType) superClass).getActualTypeArguments();
		return (index >= typeArguments.length) ? null : typeArguments[index];
	}

	private static Class<?> getClassFor(final Type type) {
		Class<?> clazz = null;
		if (type instanceof Class) {
			clazz = (Class<?>) type;
		} else if (type instanceof ParameterizedType) {
			clazz = getClassFor(((ParameterizedType) type).getRawType());
		} else if (type instanceof GenericArrayType) {
			final Type componentType = ((GenericArrayType) type).getGenericComponentType();
			final Class<?> componentClass = getClassFor(componentType);
			if (componentClass != null) {
				clazz = Array.newInstance(componentClass, 0).getClass();
			}
		}
		return clazz;
	}
}
