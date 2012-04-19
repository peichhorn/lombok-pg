import lombok.ExtensionMethod;
import java.util.Arrays;
import java.util.List;
@ExtensionMethod(ExtensionMethodGenericList.Objects.class) class ExtensionMethodGenericList {
  static class Objects {
    Objects() {
      super();
    }
    public static <T>List<T> orElse(List<T> value, List<T> orElse) {
      return ((value == null) ? orElse : value);
    }
  }
  ExtensionMethodGenericList() {
    super();
  }
  private void test8() {
    List<String> foo = null;
    List<String> s = ExtensionMethodGenericList.Objects.orElse(foo, Arrays.asList("bar"));
  }
}