import java.text.Normalizer;
import java.text.Normalizer.Form;

class ExtensionMethodSupersedes {
	
	private void test6() {
		ExtensionMethodSupersedes.Strings.matches("foobar", "^f.*ar$");
	}
	
	static class Strings {
		public static boolean matches(final String value, final CharSequence regex) {
			return value.matches(Normalizer.normalize(regex, Form.NFKC));
		}
	}
}