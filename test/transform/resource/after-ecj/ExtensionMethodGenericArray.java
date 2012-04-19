import lombok.ExtensionMethod;
@ExtensionMethod(ExtensionMethodGenericArray.Objects.class) class ExtensionMethodGenericArray {
  static class Objects {
    Objects() {
      super();
    }
    public static <T>T[] orElse(T[] value, T[] orElse) {
      return ((value == null) ? orElse : value);
    }
  }
  ExtensionMethodGenericArray() {
    super();
  }
  private void test7() {
    String[] foo = null;
    String[] s = ExtensionMethodGenericArray.Objects.orElse(foo, new String[0]);
  }
}