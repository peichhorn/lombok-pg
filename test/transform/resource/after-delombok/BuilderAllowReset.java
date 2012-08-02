class BuilderAllowRest {
	private final String finalField;
	private int initializedPrimitiveField = $Builder.$initializedPrimitiveFieldDefault();
	private Boolean initializedField = $Builder.$initializedFieldDefault();
	private double primitiveField;
	private Float field;
	
	@java.lang.SuppressWarnings("all")
	private BuilderAllowRest(final $Builder builder) {
		
		this.finalField = builder.finalField;
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
		
		OptionalDef finalField(final String finalField);
		
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
	private static class $Builder implements FinalFieldDef, OptionalDef {
		private String finalField;
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
		
		public OptionalDef finalField(final String finalField) {
			this.finalField = finalField;
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