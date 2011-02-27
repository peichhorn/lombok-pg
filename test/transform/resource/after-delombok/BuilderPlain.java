import java.util.Map;
import java.util.HashMap;

class BuilderPlain0 {
	private final String text;
	private final int id;
	
	public void builderPlain0() {
	}
}
class BuilderPlain1 {
	private final String text;
	private final int id;
	private String optionalVal1;
	private java.util.List<java.lang.Long> optionalVal2;
	private long optionalVal3;
	
	@lombok.BuilderExtension
	private void brokenExtension() {
		this.id = 42;
	}
	
	@java.lang.SuppressWarnings("all")
	private BuilderPlain1(final $Builder builder) {
		this.text = builder.text;
		this.id = builder.id;
		this.optionalVal1 = builder.optionalVal1;
		this.optionalVal2 = builder.optionalVal2;
	}
	
	@java.lang.SuppressWarnings("all")
	public static $TextDef builderPlain1() {
		return new $Builder();
	}
	
	public static interface $TextDef {
		$IdDef text(final String arg0);
		
		$OptionalDef idAndText(String id, String text);
	}
	
	public static interface $IdDef {
		$OptionalDef id(final int arg0);
	}
	
	public static interface $OptionalDef {
		$OptionalDef optionalVal1(final String arg0);
		
		$OptionalDef optionalVal2(final java.util.List<java.lang.Long> arg0);
		
		BuilderPlain1 build();
		
		$OptionalDef optionalVal1(Class<?> clazz);
	}
	
	private static class $Builder implements $TextDef, $IdDef, $OptionalDef {
		private String text;
		private int id;
		private String optionalVal1 = "default";
		private java.util.List<java.lang.Long> optionalVal2;
		
		public $IdDef text(final String arg0) {
			this.text = arg0;
			return this;
		}
		
		public $OptionalDef idAndText(String id, String text) {
			this.id = java.lang.Integer.valueOf(id);
			this.text = text;
			return this;
		}
		
		public $OptionalDef id(final int arg0) {
			this.id = arg0;
			return this;
		}
		
		public $OptionalDef optionalVal1(final String arg0) {
			this.optionalVal1 = arg0;
			return this;
		}
		
		public $OptionalDef optionalVal2(final java.util.List<java.lang.Long> arg0) {
			this.optionalVal2 = arg0;
			return this;
		}
		
		public BuilderPlain1 build() {
			return new BuilderPlain1(this);
		}
		
		public $OptionalDef optionalVal1(Class<?> clazz) {
			this.optionalVal1 = clazz.getSimpleName();
			return this;
		}
	}
}
class BuilderPlain2 {
	public static final int IGNORE = 2;
	private String optionalVal1;
	private java.util.List<java.lang.Long> optionalVal2;
	private Map<java.lang.String, java.lang.Long> optionalVal3;
	
	@java.lang.SuppressWarnings("all")
	private BuilderPlain2(final $Builder builder) {
		this.optionalVal1 = builder.optionalVal1;
		this.optionalVal2 = builder.optionalVal2;
		this.optionalVal3 = builder.optionalVal3;
	}
	
	@java.lang.SuppressWarnings("all")
	public static $OptionalDef builderPlain2() {
		return new $Builder();
	}
	
	public static interface $OptionalDef {
		$OptionalDef withOptionalVal1(final String arg0);
		
		$OptionalDef withOptionalVal2(final java.lang.Long arg0);
		
		$OptionalDef withOptionalVal2(final java.util.Collection<? extends java.lang.Long> arg0);
		
		$OptionalDef withOptionalVal3(final java.lang.String arg0, final java.lang.Long arg1);
		
		$OptionalDef withOptionalVal3(final java.util.Map<? extends java.lang.String, ? extends java.lang.Long> arg0);
		
		BuilderPlain2 build();
	}
	
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
		
		public BuilderPlain2 build() {
			return new BuilderPlain2(this);
		}
	}
}
class BuilderPlain3 {
	private final String text;
	private final int id;
	
	private void bar() throws Exception {
	}
	
	private static class Test {
		private String ignoreInnerClasses;
	}
	
	@java.lang.SuppressWarnings("all")
	private BuilderPlain3(final $Builder builder) {
		this.text = builder.text;
		this.id = builder.id;
	}
	
	@java.lang.SuppressWarnings("all")
	static $TextDef builderPlain3() {
		return new $Builder();
	}
	
	public static interface $TextDef {
		$IdDef text(final String arg0);
	}
	
	public static interface $IdDef {
		$OptionalDef id(final int arg0);
	}
	
	public static interface $OptionalDef {
		BuilderPlain3 build();
		
		java.lang.String toString();
		
		void bar() throws Exception;
	}
	
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
		
		public BuilderPlain3 build() {
			return new BuilderPlain3(this);
		}
		
		public java.lang.String toString() {
			return build().toString();
		}
		
		public void bar() throws Exception {
			build().bar();
		}
	}
}