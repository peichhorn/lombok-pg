import java.util.Map;
import java.util.HashMap;

class BuilderPlain {
	public static final String DEFAULT = "default";
	public static final int IGNORE = 2;
	private String optionalVal1 = $Builder.$optionalVal1Default();
	private java.util.List<java.lang.Long> optionalVal2 = $Builder.$optionalVal2Default();
	private Map<java.lang.String, java.lang.Long> optionalVal3 = $Builder.$optionalVal3Default();
	
	@java.lang.SuppressWarnings("all")
	private BuilderPlain(final $Builder builder) {
		this.optionalVal1 = builder.optionalVal1;
		this.optionalVal2 = builder.optionalVal2;
		this.optionalVal3 = builder.optionalVal3;
	}
	
	@java.lang.SuppressWarnings("all")
	public static OptionalDef builderPlain() {
		return new $Builder();
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface OptionalDef {
		OptionalDef withOptionalVal1(final String optionalVal1);
		
		OptionalDef withOptionalVal2(final java.lang.Long arg0);
		
		OptionalDef withOptionalVal2(final java.util.Collection<? extends java.lang.Long> arg0);
		
		OptionalDef withOptionalVal3(final java.lang.String arg0, final java.lang.Long arg1);
		
		OptionalDef withOptionalVal3(final java.util.Map<? extends java.lang.String, ? extends java.lang.Long> arg0);
		
		BuilderPlain build();
	}
	
	@java.lang.SuppressWarnings("all")
	private static class $Builder implements OptionalDef {
		private String optionalVal1 = $optionalVal1Default();
		private java.util.List<java.lang.Long> optionalVal2 = $optionalVal2Default();
		private Map<java.lang.String, java.lang.Long> optionalVal3 = $optionalVal3Default();
		
		static String $optionalVal1Default() {
			return DEFAULT;
		}
		
		static java.util.List<java.lang.Long> $optionalVal2Default() {
			return new java.util.ArrayList<java.lang.Long>();
		}
		
		static Map<java.lang.String, java.lang.Long> $optionalVal3Default() {
			return new HashMap<java.lang.String, java.lang.Long>();
		}
		
		public OptionalDef withOptionalVal1(final String optionalVal1) {
			this.optionalVal1 = optionalVal1;
			return this;
		}
		
		public OptionalDef withOptionalVal2(final java.lang.Long arg0) {
			this.optionalVal2.add(arg0);
			return this;
		}
		
		public OptionalDef withOptionalVal2(final java.util.Collection<? extends java.lang.Long> arg0) {
			this.optionalVal2.addAll(arg0);
			return this;
		}
		
		public OptionalDef withOptionalVal3(final java.lang.String arg0, final java.lang.Long arg1) {
			this.optionalVal3.put(arg0, arg1);
			return this;
		}
		
		public OptionalDef withOptionalVal3(final java.util.Map<? extends java.lang.String, ? extends java.lang.Long> arg0) {
			this.optionalVal3.putAll(arg0);
			return this;
		}
		
		public BuilderPlain build() {
			return new BuilderPlain(this);
		}
		
		private $Builder() {
		}
	}
}