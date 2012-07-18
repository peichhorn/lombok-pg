class BuilderCallMethods {
	private final String text;
	private final int id;
	
	private void bar() throws Exception {
	}
	
	private static class Test {
		private String ignoreInnerClasses;
	}
	
	@java.lang.SuppressWarnings("all")
	private BuilderCallMethods(final $Builder builder) {
		this.text = builder.text;
		this.id = builder.id;
	}
	
	@java.lang.SuppressWarnings("all")
	static TextDef builderCallMethods() {
		return new $Builder();
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface TextDef {
		IdDef text(final String text);
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface IdDef {
		OptionalDef id(final int id);
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface OptionalDef {
		BuilderCallMethods build();
		
		java.lang.String toString();
		
		void bar() throws Exception;
	}
	
	@java.lang.SuppressWarnings("all")
	private static class $Builder implements TextDef, IdDef, OptionalDef {
		private String text;
		private int id;
		
		public IdDef text(final String text) {
			this.text = text;
			return this;
		}
		
		public OptionalDef id(final int id) {
			this.id = id;
			return this;
		}
		
		public BuilderCallMethods build() {
			return new BuilderCallMethods(this);
		}
		
		public java.lang.String toString() {
			return build().toString();
		}
		
		public void bar() throws Exception {
			build().bar();
		}
		
		private $Builder() {
		}
	}
}