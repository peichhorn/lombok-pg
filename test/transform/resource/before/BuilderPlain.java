import java.util.Map;
import java.util.HashMap;

@lombok.Builder(prefix="with")
class BuilderPlain {
	public static final String DEFAULT = "default";
	public static final int IGNORE = 2;
	private String optionalVal1 = DEFAULT;
	private java.util.List<java.lang.Long> optionalVal2 = new java.util.ArrayList<java.lang.Long>();
	private Map<java.lang.String, java.lang.Long> optionalVal3 = new HashMap<java.lang.String, java.lang.Long>();
}