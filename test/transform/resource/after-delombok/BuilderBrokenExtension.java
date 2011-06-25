class BuilderBrokenExtension {
	private final String text;
	private final int id;
	
	@lombok.Builder.Extension
	private void brokenExtension() {
		this.id = 42;
	}
	
	@java.lang.SuppressWarnings("all")
	private BuilderBrokenExtension(final $Builder builder) {
		this.text = builder.text;
		this.id = builder.id;
	}
	
	@java.lang.SuppressWarnings("all")
	public static $TextDef builderBrokenExtension() {
		return new $Builder();
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface $TextDef {
		
		$IdDef text(final String arg0);
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface $IdDef {
		
		$OptionalDef id(final int arg0);
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface $OptionalDef {
		
		BuilderBrokenExtension build();
	}
	
	@java.lang.SuppressWarnings("all")
	private static class $Builder implements $TextDef, $IdDef, $OptionalDef {
		
		private String text;
		private int id;
		
		public $IdDef text(final String arg0) {
			this.text = arg0;
			return this;
		}
		
		public $OptionalDef id(final int arg0) {
			this.id = arg0;
			return this;
		}
		
		public BuilderBrokenExtension build() {
			return new BuilderBrokenExtension(this);
		}
	}
}