import lombok.Function;
import lombok.val;

public class FunctionAndVal {

	@Function
	private static String needsMoreVal() {
		val part = "String";
		return part;
	}
}