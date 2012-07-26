class BuilderAccessorsPrefix {
	private final String _name;
	private final String _surname;
	private Integer ignoreMeCauseAccessorsSaysSo;
	
	@java.lang.SuppressWarnings("all")
	private BuilderAccessorsPrefix(final $Builder builder) {
		this._name = builder.name;
		this._surname = builder.surname;
	}
	
	@java.lang.SuppressWarnings("all")
	public static NameDef builderAccessorsPrefix() {
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
		
		BuilderAccessorsPrefix build();
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
		
		public BuilderAccessorsPrefix build() {
			return new BuilderAccessorsPrefix(this);
		}
		
		private $Builder() {
		}
	}
}