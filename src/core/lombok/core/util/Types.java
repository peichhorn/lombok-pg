package lombok.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Types {

	public static boolean isOneOf(Object o, Class<?>... clazzes) {
		if (clazzes != null) for (Class<?> clazz : clazzes) {
			if (clazz.isInstance(o)) return true;
		}
		return false;
	}

	public static boolean isNoneOf(Object o, Class<?>... clazzes) {
		if (clazzes != null) for (Class<?> clazz : clazzes) {
			if (clazz.isInstance(o)) return false;
		}
		return true;
	}
}
