class ExtensionMethodGeneric {
	
	private void test6() {
		String foo = null;
		String s = ExtensionMethodGeneric.Objects.orElse(foo, "bar");
	}
	
	static class Objects {
		public static <T>T orElse(T value, T orElse) {
			return value == null ? orElse : value;
		}
	}
}