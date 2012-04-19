@lombok.Builder(value = lombok.AccessLevel.PACKAGE,callMethods = {"toString", "bar"}) class BuilderCallMethods {
  private static class Test {
    private String ignoreInnerClasses;
    private Test() {
      super();
    }
  }
  public static @java.lang.SuppressWarnings("all") interface $TextDef {
    public $IdDef text(final String text);
  }
  public static @java.lang.SuppressWarnings("all") interface $IdDef {
    public $OptionalDef id(final int id);
  }
  public static @java.lang.SuppressWarnings("all") interface $OptionalDef {
    public BuilderCallMethods build();
    public java.lang.String toString();
    public void bar() throws Exception;
  }
  private static @java.lang.SuppressWarnings("all") class $Builder implements $TextDef, $IdDef, $OptionalDef {
    private String text;
    private int id;
    public $IdDef text(final String text) {
      this.text = text;
      return this;
    }
    public $OptionalDef id(final int id) {
      this.id = id;
      return this;
    }
    public BuilderCallMethods build() {
      return new BuilderCallMethods(this);
    }
    public java.lang.String toString() {
      return build().toString();
    }
    public void bar() throws Exception {
      build().bar();
    }
    private $Builder() {
      super();
    }
  }
  private final String text;
  private final int id;
  private @java.lang.SuppressWarnings("all") BuilderCallMethods(final $Builder builder) {
    super();
    this.text = builder.text;
    this.id = builder.id;
  }
  static @java.lang.SuppressWarnings("all") $TextDef builderCallMethods() {
    return new $Builder();
  }
  private void bar() throws Exception {
  }
}