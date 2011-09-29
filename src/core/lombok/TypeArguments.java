package lombok;

import java.lang.reflect.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TypeArguments {
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
