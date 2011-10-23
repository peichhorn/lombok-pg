import java.text.Normalizer;
import java.text.Normalizer.Form;

import lombok.ExtensionMethod;

@ExtensionMethod(ExtensionMethodSupersedes.Strings.class)
class ExtensionMethodSupersedes {
	
	private void test6() {
		"foobar".matches("^f.*ar$");
	}
	
	static class Strings {
		public static boolean matches(final String value, final CharSequence regex) {
			return value.matches(Normalizer.normalize(regex, Form.NFKC));
		}
	}
}