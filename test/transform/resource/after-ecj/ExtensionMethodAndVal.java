import java.util.ArrayList;
import lombok.ExtensionMethod;
import lombok.val;
@ExtensionMethod(ExtensionMethodAndVar.Objects.class) class ExtensionMethodAndVar {
  static class Objects {
    Objects() {
      super();
    }
    public static <T>T orElse(T value, T orElse) {
      return ((value == null) ? orElse : value);
    }
  }
  ExtensionMethodAndVar() {
    super();
  }
  public static Iterable<String> foobar() {
    return new ArrayList<String>();
  }
  private void test() {
    for (String s : foobar()) 
      {
      }
    final @val java.lang.reflect.Method handler = Object.class.getDeclaredMethods()[0];
    for (String s : foobar()) 
      {
      }
  }
}