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
	
	static class Objects {
		public static boolean isOneOf(Object object, Object... possibleValues) {
			if (possibleValues != null) for (Object possibleValue : possibleValues) {
				if (object.equals(possibleValue)) return true;
			}
			return false;
		}
	}
}