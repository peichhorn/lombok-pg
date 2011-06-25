import java.util.Map;
import java.util.HashMap;

class BuilderPlain {
	public static final int IGNORE = 2;
	private String optionalVal1;
	private java.util.List<java.lang.Long> optionalVal2;
	private Map<java.lang.String, java.lang.Long> optionalVal3;
	
	@java.lang.SuppressWarnings("all")
	private BuilderPlain(final $Builder builder) {
		this.optionalVal1 = builder.optionalVal1;
		this.optionalVal2 = builder.optionalVal2;
		this.optionalVal3 = builder.optionalVal3;
	}
	
	@java.lang.SuppressWarnings("all")
	public static $OptionalDef builderPlain() {
		return new $Builder();
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface $OptionalDef {
		$OptionalDef withOptionalVal1(final String arg0);
		
		$OptionalDef withOptionalVal2(final java.lang.Long arg0);
		
		$OptionalDef withOptionalVal2(final java.util.Collection<? extends java.lang.Long> arg0);
		
		$OptionalDef withOptionalVal3(final java.lang.String arg0, final java.lang.Long arg1);
		
		$OptionalDef withOptionalVal3(final java.util.Map<? extends java.lang.String, ? extends java.lang.Long> arg0);
		
		BuilderPlain build();
	}
	
	@java.lang.SuppressWarnings("all")
	private static class $Builder implements $OptionalDef {
		private String optionalVal1;
		private java.util.List<java.lang.Long> optionalVal2 = new java.util.ArrayList<java.lang.Long>();
		private Map<java.lang.String, java.lang.Long> optionalVal3 = new HashMap<java.lang.String, java.lang.Long>();
		
		public $OptionalDef withOptionalVal1(final String arg0) {
			this.optionalVal1 = arg0;
			return this;
		}
		
		public $OptionalDef withOptionalVal2(final java.lang.Long arg0) {
			this.optionalVal2.add(arg0);
			return this;
		}
		
		public $OptionalDef withOptionalVal2(final java.util.Collection<? extends java.lang.Long> arg0) {
			this.optionalVal2.addAll(arg0);
			return this;
		}
		
		public $OptionalDef withOptionalVal3(final java.lang.String arg0, final java.lang.Long arg1) {
			this.optionalVal3.put(arg0, arg1);
			return this;
		}
		
		public $OptionalDef withOptionalVal3(final java.util.Map<? extends java.lang.String, ? extends java.lang.Long> arg0) {
			this.optionalVal3.putAll(arg0);
			return this;
		}
		
		public BuilderPlain build() {
			return new BuilderPlain(this);
		}
	}
}