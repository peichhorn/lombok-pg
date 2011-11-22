import lombok.NonNull;

class BuilderNonNull {
	@NonNull
	private String nonNullValue;
	private String anotherValue;
	
	@java.lang.SuppressWarnings("all")
	private BuilderNonNull(final $Builder builder) {
		super();
		this.nonNullValue = builder.nonNullValue;
		this.anotherValue = builder.anotherValue;
	}
	
	@java.lang.SuppressWarnings("all")
	public static $NonNullValueDef builderNonNull() {
		return new $Builder();
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface $NonNullValueDef {
		
		$OptionalDef nonNullValue(final String nonNullValue);
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface $OptionalDef {
		
		$OptionalDef anotherValue(final String anotherValue);
		
		BuilderNonNull build();
	}
	
	@java.lang.SuppressWarnings("all")
	private static class $Builder implements $NonNullValueDef, $OptionalDef {
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
}