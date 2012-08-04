class BuilderAllowRest {
	private final String finalField;
	private final String anotherFinalField;
	private int initializedPrimitiveField = $Builder.$initializedPrimitiveFieldDefault();
	private Boolean initializedField = $Builder.$initializedFieldDefault();
	private double primitiveField;
	private Float field;
	
	@java.lang.SuppressWarnings("all")
	private BuilderAllowRest(final $Builder builder) {
		
		this.finalField = builder.finalField;
		this.anotherFinalField = builder.anotherFinalField;
		this.initializedPrimitiveField = builder.initializedPrimitiveField;
		this.initializedField = builder.initializedField;
		this.primitiveField = builder.primitiveField;
		this.field = builder.field;
	}
	
	@java.lang.SuppressWarnings("all")
	public static FinalFieldDef builderAllowRest() {
		return new $Builder();
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface FinalFieldDef {
		
		AnotherFinalFieldDef finalField(final String finalField);
		
		FinalFieldDef reset();
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface AnotherFinalFieldDef {
		
		OptionalDef anotherFinalField(final String anotherFinalField);
		
		FinalFieldDef reset();
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface OptionalDef {
		
		OptionalDef initializedPrimitiveField(final int initializedPrimitiveField);
		
		OptionalDef initializedField(final Boolean initializedField);
		
		OptionalDef primitiveField(final double primitiveField);
		
		OptionalDef field(final Float field);
		
		BuilderAllowRest build();
		
		FinalFieldDef reset();
	}
	
	@java.lang.SuppressWarnings("all")
	private static class $Builder implements FinalFieldDef, AnotherFinalFieldDef, OptionalDef {
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
			this.primitiveField = 0;
			this.field = null;
			return this;
		}
		
		private $Builder() {
			
		}
	}
}