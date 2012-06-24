import lombok.ExtensionMethod;
@ExtensionMethod(ExtensionMethodPrimitives.Primitives.class) class ExtensionMethodPrimitives {
  static class Primitives {
    Primitives() {
      super();
    }
    public static int toInt(final byte in) {
      return (in & 0xff);
    }
  }
  ExtensionMethodPrimitives() {
    super();
  }
  private void test(final byte b) {
    int i = ExtensionMethodPrimitives.Primitives.toInt(b);
  }
}