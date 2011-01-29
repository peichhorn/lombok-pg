package lombok.core.util;

public final class Types {
	private Types() {
	}
	
	public static boolean isOneOf(Object o, Class<?>... clazzes) {
		if (clazzes != null) for (Class<?> clazz : clazzes) {
			if (clazz.isInstance(o)) return true; 
		}
		return false;
	}
}
