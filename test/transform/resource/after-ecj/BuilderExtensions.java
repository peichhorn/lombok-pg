@lombok.Builder(exclude = {"optionalVal3"},convenientMethods = false) class BuilderExtensions {
  public static @java.lang.SuppressWarnings("all") interface TextDef {
    public IdDef text(final String text);
    public @java.lang.SuppressWarnings("all") OptionalDef idAndText(final String id, final String text);
  }
  public static @java.lang.SuppressWarnings("all") interface IdDef {
    public OptionalDef id(final int id);
  }
  public static @java.lang.SuppressWarnings("all") interface OptionalDef {
    public OptionalDef optionalVal1(final String optionalVal1);
    public OptionalDef optionalVal2(final java.util.List<java.lang.Long> optionalVal2);
    public BuilderExtensions build();
    public @java.lang.SuppressWarnings("all") OptionalDef optionalVal1(final Class<?> clazz);
  }
  private static @java.lang.SuppressWarnings("all") class $Builder implements TextDef, IdDef, OptionalDef {
    private String text;
    private int id;
    private String optionalVal1 = "default";
    private java.util.List<java.lang.Long> optionalVal2;
    public IdDef text(final String text) {
      this.text = text;
      return this;
    }
    public OptionalDef id(final int id) {
      this.id = id;
      return this;
    }
    public OptionalDef optionalVal1(final String optionalVal1) {
      this.optionalVal1 = optionalVal1;
      return this;
    }
    public OptionalDef optionalVal2(final java.util.List<java.lang.Long> optionalVal2) {
      this.optionalVal2 = optionalVal2;
      return this;
    }
    public BuilderExtensions build() {
      return new BuilderExtensions(this);
    }
    private $Builder() {
      super();
    }
    public @java.lang.SuppressWarnings("all") OptionalDef idAndText(final String id, final String text) {
      this.id = java.lang.Integer.valueOf(id);
      this.text = text;
      return this;
    }
    public @java.lang.SuppressWarnings("all") OptionalDef optionalVal1(final Class<?> clazz) {
      this.optionalVal1 = clazz.getSimpleName();
      return this;
    }
  }
  private final String text;
  private final int id;
  private String optionalVal1 = "default";
  private java.util.List<java.lang.Long> optionalVal2;
  private long optionalVal3;
  private @java.lang.SuppressWarnings("all") BuilderExtensions(final $Builder builder) {
    super();
    this.text = builder.text;
    this.id = builder.id;
    this.optionalVal1 = builder.optionalVal1;
    this.optionalVal2 = builder.optionalVal2;
  }
  public static @java.lang.SuppressWarnings("all") TextDef builderExtensions() {
    return new $Builder();
  }
}