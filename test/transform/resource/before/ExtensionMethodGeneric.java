import lombok.ExtensionMethod;

@ExtensionMethod(ExtensionMethodGeneric.Objects.class)
class ExtensionMethodGeneric {
	
	private void test6() {
		String foo = null;
		String s = foo.orElse("bar");
	}
	
	static class Objects {
		public static <T> T orElse(T value, T orElse) {
			return value == null ? orElse : value;
		}
	}
}