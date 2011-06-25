import java.util.Map;
import java.util.HashMap;

class BuilderExtensions {
	private final String text;
	private final int id;
	private String optionalVal1;
	private java.util.List<java.lang.Long> optionalVal2;
	private long optionalVal3;
	
	@java.lang.SuppressWarnings("all")
	private BuilderExtensions(final $Builder builder) {
		this.text = builder.text;
		this.id = builder.id;
		this.optionalVal1 = builder.optionalVal1;
		this.optionalVal2 = builder.optionalVal2;
	}
	
	@java.lang.SuppressWarnings("all")
	public static $TextDef builderExtensions() {
		return new $Builder();
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface $TextDef {
		$IdDef text(final String arg0);
		
		$OptionalDef idAndText(String id, String text);
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface $IdDef {
		$OptionalDef id(final int arg0);
	}
	
	@java.lang.SuppressWarnings("all")
	public static interface $OptionalDef {
		$OptionalDef optionalVal1(final String arg0);
		
		$OptionalDef optionalVal2(final java.util.List<java.lang.Long> arg0);
		
		BuilderExtensions build();
		
		$OptionalDef optionalVal1(Class<?> clazz);
	}
	
	@java.lang.SuppressWarnings("all")
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
		
		public BuilderExtensions build() {
			return new BuilderExtensions(this);
		}
		
		public $OptionalDef optionalVal1(Class<?> clazz) {
			this.optionalVal1 = clazz.getSimpleName();
			return this;
		}
	}
}