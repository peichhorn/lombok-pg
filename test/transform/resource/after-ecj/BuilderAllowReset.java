import lombok.Builder;
@Builder(allowReset = true) class BuilderAllowRest {
  public static @java.lang.SuppressWarnings("all") interface FinalFieldDef {
    public AnotherFinalFieldDef finalField(final String finalField);
    public FinalFieldDef reset();
  }
  public static @java.lang.SuppressWarnings("all") interface AnotherFinalFieldDef {
    public OptionalDef anotherFinalField(final String anotherFinalField);
    public FinalFieldDef reset();
  }
  public static @java.lang.SuppressWarnings("all") interface OptionalDef {
    public OptionalDef initializedPrimitiveField(final int initializedPrimitiveField);
    public OptionalDef initializedField(final Boolean initializedField);
    public OptionalDef primitiveField(final double primitiveField);
    public OptionalDef field(final Float field);
    public BuilderAllowRest build();
    public FinalFieldDef reset();
  }
  private static @java.lang.SuppressWarnings("all") class $Builder implements FinalFieldDef, AnotherFinalFieldDef, OptionalDef {
    private String finalField;
    private String anotherFinalField;
    private int initializedPrimitiveField = $initializedPrimitiveFieldDefault();
    private Boolean initializedField = $initializedFieldDefault();
    private double primitiveField;
    private Float field;
    static int $initializedPrimitiveFieldDefault() {
      return 42;
    }
    static Boolean $initializedFieldDefault() {
      return Boolean.FALSE;
    }
    public AnotherFinalFieldDef finalField(final String finalField) {
      this.finalField = finalField;
      return this;
    }
    public OptionalDef anotherFinalField(final String anotherFinalField) {
      this.anotherFinalField = anotherFinalField;
      return this;
    }
    public OptionalDef initializedPrimitiveField(final int initializedPrimitiveField) {
      this.initializedPrimitiveField = initializedPrimitiveField;
      return this;
    }
    public OptionalDef initializedField(final Boolean initializedField) {
      this.initializedField = initializedField;
      return this;
    }
    public OptionalDef primitiveField(final double primitiveField) {
      this.primitiveField = primitiveField;
      return this;
    }
    public OptionalDef field(final Float field) {
      this.field = field;
      return this;
    }
    public BuilderAllowRest build() {
      return new BuilderAllowRest(this);
    }
    public FinalFieldDef reset() {
      this.finalField = null;
      this.anotherFinalField = null;
      this.initializedPrimitiveField = $initializedPrimitiveFieldDefault();
      this.initializedField = $initializedFieldDefault();
      this.primitiveField = 0.0d;
      this.field = null;
      return this;
    }
    private $Builder() {
      super();
    }
  }
  private final String finalField;
  private final String anotherFinalField;
  private int initializedPrimitiveField = $Builder.$initializedPrimitiveFieldDefault();
  private Boolean initializedField = $Builder.$initializedFieldDefault();
  private double primitiveField;
  private Float field;
  private @java.lang.SuppressWarnings("all") BuilderAllowRest(final $Builder builder) {
    super();
    this.finalField = builder.finalField;
    this.anotherFinalField = builder.anotherFinalField;
    this.initializedPrimitiveField = builder.initializedPrimitiveField;
    this.initializedField = builder.initializedField;
    this.primitiveField = builder.primitiveField;
    this.field = builder.field;
  }
  public static @java.lang.SuppressWarnings("all") FinalFieldDef builderAllowRest() {
    return new $Builder();
  }
}