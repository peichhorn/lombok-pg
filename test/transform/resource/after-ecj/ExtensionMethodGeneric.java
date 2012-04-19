import lombok.ExtensionMethod;
@ExtensionMethod(ExtensionMethodGeneric.Objects.class) class ExtensionMethodGeneric {
  static class Objects {
    Objects() {
      super();
    }
    public static <T>T orElse(T value, T orElse) {
      return ((value == null) ? orElse : value);
    }
  }
  ExtensionMethodGeneric() {
    super();
  }
  private void test6() {
    String foo = null;
    String s = ExtensionMethodGeneric.Objects.orElse(foo, "bar");
  }
}