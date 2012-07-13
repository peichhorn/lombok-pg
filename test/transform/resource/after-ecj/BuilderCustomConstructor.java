import lombok.Builder;
@Builder class BuilderCustomConstructor {
  public static @java.lang.SuppressWarnings("all") interface NameDef {
    public SurnameDef name(final String name);
  }
  public static @java.lang.SuppressWarnings("all") interface SurnameDef {
    public OptionalDef surname(final String surname);
  }
  public static @java.lang.SuppressWarnings("all") interface OptionalDef {
    public BuilderCustomConstructor build();
  }
  private static @java.lang.SuppressWarnings("all") class $Builder implements NameDef, SurnameDef, OptionalDef {
    private String name;
    private String surname;
    public SurnameDef name(final String name) {
      this.name = name;
      return this;
    }
    public OptionalDef surname(final String surname) {
      this.surname = surname;
      return this;
    }
    public BuilderCustomConstructor build() {
      return new BuilderCustomConstructor(this);
    }
    private $Builder() {
      super();
    }
  }
  private final String name;
  private final String surname;
  private BuilderCustomConstructor(final $Builder builder) {
    super();
    this.name = builder.name.trim();
    this.surname = builder.surname.trim();
  }
  public static @java.lang.SuppressWarnings("all") NameDef builderCustomConstructor() {
    return new $Builder();
  }
}