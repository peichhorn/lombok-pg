@lombok.Builder(value=lombok.AccessLevel.PACKAGE, callMethods={"toString", "bar"})
class BuilderCallMethods {
	private final String text;
	private final int id;
	
	private void bar() throws Exception {
	}
	
	private static class Test {
		private String ignoreInnerClasses;
	}
}