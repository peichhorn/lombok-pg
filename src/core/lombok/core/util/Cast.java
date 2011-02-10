package lombok.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Cast {
	@SuppressWarnings("unchecked")
	public static <T> T[] uncheckedCast(Object[] o) {
		return (T[]) o;
	}

	@SuppressWarnings("unchecked")
	public static <T> T uncheckedCast(Object o) {
		return (T) o;
	}
}
