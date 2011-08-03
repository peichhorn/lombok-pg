import java.util.Map;
import java.util.HashMap;

@lombok.Builder(exclude={"optionalVal3"}, convenientMethods=false)
class BuilderExtensions {
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
	private void optionalVal1(final Class<?> clazz) {
		this.optionalVal1 = clazz.getSimpleName();
	}
}