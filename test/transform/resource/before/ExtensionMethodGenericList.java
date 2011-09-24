import lombok.ExtensionMethod;
import java.util.Arrays;
import java.util.List;

@ExtensionMethod(ExtensionMethodGenericList.Objects.class)
class ExtensionMethodGenericList {

	private void test8() {
		List<String> foo = null;
		List<String> s = foo.orElse(Arrays.asList("bar"));
	}
	
	static class Objects {
		public static <T> List<T> orElse(List<T> value, List<T> orElse) {
			return value == null ? orElse : value;
		}
		
	}
}