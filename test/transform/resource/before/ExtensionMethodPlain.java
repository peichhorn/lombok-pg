import lombok.ExtensionMethod;
import java.util.Arrays;

@ExtensionMethod({Arrays.class, Objects.class})
class ExtensionMethodPlain {
	private void test1() {
		long[] values = new long[] { 2, 5, 7, 9 };
		values.sort();
	}
	
	private boolean test2(String s) {
		return s.isOneOf("for", "bar");
	}
	
	private boolean test3() {
		return this.isOneOf("for", "bar");
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