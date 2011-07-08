import java.util.Arrays;

class ExtensionMethodPlain {
	private void test1() {
		long[] values = new long[]{2, 5, 7, 9};
		java.util.Arrays.sort(java.util.Arrays.copyOf(values, 3));
	}
	
	private boolean test2(String s) {
		return ExtensionMethodPlain.Objects.isOneOf(s, "for", "bar");
	}
	
	private boolean test3() {
		return ExtensionMethodPlain.Objects.isOneOf(this, "for", "bar");
	}
	
	private boolean test4(String s) {
		return Objects.isOneOf(s, "for", "bar");
	}
	
	boolean test5(final Iterable<String> paths, final String path) {
		for (final String p : paths) {
			if (ExtensionMethodPlain.Strings.matchesIgnoreCase(path, ExtensionMethodPlain.Strings.escapeToJavaRegex(p))) {
				return true;
			}
		}
		return false;
	}
	
	static class Objects {
		public static boolean isOneOf(Object object, Object... possibleValues) {
			if (possibleValues != null) for (Object possibleValue : possibleValues) {
				if (object.equals(possibleValue)) return true;
			}
			return false;
		}
	}
	
	static class Strings {
		public static boolean matchesIgnoreCase(String s, String p) {
			return false;
		}
		
		public static String escapeToJavaRegex(String s) {
			return s;
		}
	}
}