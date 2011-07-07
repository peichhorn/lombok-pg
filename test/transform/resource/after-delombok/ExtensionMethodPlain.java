import java.util.Arrays;

class ExtensionMethodPlain {
	private void test1() {
		long[] values = new long[] { 2, 5, 7, 9 };
		Arrays.sort(values);
	}
	
	private boolean test2(String s) {
		return .isOneOf(s, "for", "bar");
	}
	
	private boolean test3() {
		return Objects.isOneOf(this, "for", "bar");
	}
	
	private static class Objects {
		public static boolean isOneOf(Object object, Object... possibleValues) {
			if (possibleValues != null) for (Object possibleValue : possibleValues) {
				if (object.equals(possibleValue)) return true;
			}
			return false;
		}
	}
}