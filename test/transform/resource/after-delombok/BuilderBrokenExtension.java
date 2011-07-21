class BuilderBrokenExtension {
	private final String text;
	private final int id;
	
	private void brokenExtension() {
		this.id = 42;
	}
	
	@java.lang.SuppressWarnings("all")
	private BuilderBrokenExtension(final $Builder builder) {
		super();
		this.text = builder.text;
		this.id = builder.id;
	}
	
	@java.lang.SuppressWarnings("all")
	public static $TextDef builderBrokenExtension() {
		return new $Builder();
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface $TextDef {
		
		$IdDef text(final String text);
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface $IdDef {
		
		$OptionalDef id(final int id);
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface $OptionalDef {
		
		BuilderBrokenExtension build();
	}
	
	@java.lang.SuppressWarnings("all")
	private static class $Builder implements $TextDef, $IdDef, $OptionalDef {
		
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
		
		public BuilderBrokenExtension build() {
			return new BuilderBrokenExtension(this);
		}
		
		private $Builder() {
			super();
		}
	}
}