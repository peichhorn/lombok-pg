import lombok.Builder;
import lombok.experimental.Accessors;
@Builder @Accessors(prefix = "_") class BuilderAccessorsPrefix {
  public static @java.lang.SuppressWarnings("all") interface NameDef {
    public SurnameDef name(final String name);
  }
  public static @java.lang.SuppressWarnings("all") interface SurnameDef {
    public OptionalDef surname(final String surname);
  }
  public static @java.lang.SuppressWarnings("all") interface OptionalDef {
    public BuilderAccessorsPrefix build();
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
    public BuilderAccessorsPrefix build() {
      return new BuilderAccessorsPrefix(this);
    }
    private $Builder() {
      super();
    }
  }
  private final String _name;
  private final String _surname;
  private Integer ignoreMeCauseAccessorsSaysSo;
  private @java.lang.SuppressWarnings("all") BuilderAccessorsPrefix(final $Builder builder) {
    super();
    this._name = builder.name;
    this._surname = builder.surname;
  }
  public static @java.lang.SuppressWarnings("all") NameDef builderAccessorsPrefix() {
    return new $Builder();
  }
}