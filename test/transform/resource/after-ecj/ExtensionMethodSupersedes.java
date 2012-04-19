import java.text.Normalizer;
import java.text.Normalizer.Form;
import lombok.ExtensionMethod;
@ExtensionMethod(ExtensionMethodSupersedes.Strings.class) class ExtensionMethodSupersedes {
  static class Strings {
    Strings() {
      super();
    }
    public static boolean matches(final String value, final CharSequence regex) {
      return ExtensionMethodSupersedes.Strings.matches(value, Normalizer.normalize(regex, Form.NFKC));
    }
  }
  ExtensionMethodSupersedes() {
    super();
  }
  private void test6() {
    ExtensionMethodSupersedes.Strings.matches("foobar", "^f.*ar$");
  }
}