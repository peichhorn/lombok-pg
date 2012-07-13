@lombok.Builder class BuilderBrokenExtension {
  public static @java.lang.SuppressWarnings("all") interface TextDef {
    public IdDef text(final String text);
  }
  public static @java.lang.SuppressWarnings("all") interface IdDef {
    public OptionalDef id(final int id);
  }
  public static @java.lang.SuppressWarnings("all") interface OptionalDef {
    public BuilderBrokenExtension build();
  }
  private static @java.lang.SuppressWarnings("all") class $Builder implements TextDef, IdDef, OptionalDef {
    private String text;
    private int id;
    public IdDef text(final String text) {
      this.text = text;
      return this;
    }
    public OptionalDef id(final int id) {
      this.id = id;
      return this;
    }
    public BuilderBrokenExtension build() {
      return new BuilderBrokenExtension(this);
    }
    private $Builder() {
      super();
    }
  }
  private final String text;
  private final int id;
  private @lombok.Builder.Extension void brokenExtension() {
    this.id = 42;
  }
  private @java.lang.SuppressWarnings("all") BuilderBrokenExtension(final $Builder builder) {
    super();
    this.text = builder.text;
    this.id = builder.id;
  }
  public static @java.lang.SuppressWarnings("all") TextDef builderBrokenExtension() {
    return new $Builder();
  }
}