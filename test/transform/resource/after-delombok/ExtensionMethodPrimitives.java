class ExtensionMethodPrimitives {

	private void test(final byte b) {
		int i = ExtensionMethodPrimitives.Primitives.toInt(b);
	}

	static class Primitives {
		public static int toInt(final byte in) {
			return in & 255;
		}
	}
}