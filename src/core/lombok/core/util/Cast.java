package lombok.core.util;

public class Cast {
	@SuppressWarnings("unchecked")
	public static <T> T[] uncheckedCast(Object[] o) {
		return (T[]) o;
	}

	@SuppressWarnings("unchecked")
	public static <T> T uncheckedCast(Object o) {
		return (T) o;
	}
}
