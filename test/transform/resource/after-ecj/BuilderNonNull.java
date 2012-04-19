import lombok.Builder;
import lombok.NonNull;
@Builder class BuilderNonNull {
  public static @java.lang.SuppressWarnings("all") interface $NonNullValueDef {
    public $OptionalDef nonNullValue(final String nonNullValue);
  }
  public static @java.lang.SuppressWarnings("all") interface $OptionalDef {
    public $OptionalDef anotherValue(final String anotherValue);
    public BuilderNonNull build();
  }
  private static @java.lang.SuppressWarnings("all") class $Builder implements $NonNullValueDef, $OptionalDef {
    private String nonNullValue;
    private String anotherValue;
    public $OptionalDef nonNullValue(final String nonNullValue) {
      this.nonNullValue = nonNullValue;
      return this;
    }
    public $OptionalDef anotherValue(final String anotherValue) {
      this.anotherValue = anotherValue;
      return this;
    }
    public BuilderNonNull build() {
      return new BuilderNonNull(this);
    }
    private $Builder() {
      super();
    }
  }
  private @NonNull String nonNullValue;
  private String anotherValue;
  private @java.lang.SuppressWarnings("all") BuilderNonNull(final $Builder builder) {
    super();
    this.nonNullValue = builder.nonNullValue;
    this.anotherValue = builder.anotherValue;
  }
  public static @java.lang.SuppressWarnings("all") $NonNullValueDef builderNonNull() {
    return new $Builder();
  }
}