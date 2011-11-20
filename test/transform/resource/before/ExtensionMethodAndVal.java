import java.util.ArrayList;
import lombok.ExtensionMethod;
import lombok.val;

@ExtensionMethod(ExtensionMethodAndVar.Objects.class)
class ExtensionMethodAndVar {

	public static Iterable<String> foobar() {
		return new ArrayList<String>();
	}

	private void test() {
		for (String s : foobar()) {
		}
		val handler = Object.class.getDeclaredMethods()[0];
		for (String s : foobar()) {
		}
	}

	static class Objects {
		public static <T>T orElse(T value, T orElse) {
			return value == null ? orElse : value;
		}
	}
}