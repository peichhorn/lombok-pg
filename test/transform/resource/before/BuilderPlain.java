import java.util.Map;
import java.util.HashMap;

@lombok.Builder
class BuilderPlain0 {
	private final String text;
	private final int id;
	
	public void builderPlain0() {
	}
}
@lombok.Builder(exclude={"optionalVal3"}, convenientMethods=false)
class BuilderPlain1 {
	private final String text;
	private final int id;
	private String optionalVal1 = "default";
	private java.util.List<java.lang.Long> optionalVal2;
	private long optionalVal3;
	
	@lombok.Builder.Extension
	private void idAndText(String id, String text) {
		this.id = java.lang.Integer.valueOf(id);
		this.text = text;
	}
	
	@lombok.Builder.Extension
	private void brokenExtension() {
		this.id = 42;
	}
	
	@lombok.Builder.Extension
	private void optionalVal1(Class<?> clazz) {
		this.optionalVal1 = clazz.getSimpleName();
	}
}
@lombok.Builder(prefix="with")
class BuilderPlain2 {
	public static final int IGNORE = 2;
	private String optionalVal1;
	private java.util.List<java.lang.Long> optionalVal2 = new java.util.ArrayList<java.lang.Long>();
	private Map<java.lang.String, java.lang.Long> optionalVal3 = new HashMap<java.lang.String, java.lang.Long>();
}
@lombok.Builder(value=lombok.AccessLevel.PACKAGE, callMethods={"toString", "bar"})
class BuilderPlain3 {
	private final String text;
	private final int id;
	
	private void bar() throws Exception {
	}
	
	private static class Test {
		private String ignoreInnerClasses;
	}
}