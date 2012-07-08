class BuilderCustomConstructor {
	private final String name;
	private final String surname;
	
	private BuilderCustomConstructor(final $Builder builder) {
		super();
		this.name = builder.name.trim();
		this.surname = builder.surname.trim();
	}
	
	@java.lang.SuppressWarnings("all")
	public static NameDef builderCustomConstructor() {
		return new $Builder();
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface NameDef {
		
		SurnameDef name(final String name);
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface SurnameDef {
		
		OptionalDef surname(final String surname);
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface OptionalDef {
		
		BuilderCustomConstructor build();
	}
	
	@java.lang.SuppressWarnings("all")
	private static class $Builder implements NameDef, SurnameDef, OptionalDef {
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
}