import lombok.ExtensionMethod;
import java.util.Arrays;

@ExtensionMethod({Arrays.class, ExtensionMethodPlain.Objects.class})
class ExtensionMethodPlain {
	private void test1() {
		long[] values = new long[] { 2, 5, 7, 9 };
		values.copyOf(3).sort();
	}
	
	private boolean test2(String s) {
		return s.isOneOf("for", "bar");
	}
	
	private boolean test3() {
		return this.isOneOf("for", "bar");
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