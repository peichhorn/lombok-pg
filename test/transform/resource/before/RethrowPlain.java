class RethrowPlain {

	@lombok.Rethrow(value = java.io.FileNotFoundException.class, as = java.lang.IllegalArgumentException.class)
	void testRethrowAs() {
		System.out.println("code that throws FileNotFoundException");
	}

	@lombok.Rethrow(value = java.lang.InterruptedException.class)
	void testRethrowAsRuntimeException() {
		System.out.println("code that might throw InterruptedException due to cancelation");
	}
	
	@lombok.Rethrow(as = java.lang.IllegalArgumentException.class, message = "meh.")
	void testRethrowEveryExceptionAsSpecifiedException(final @lombok.Validate.NotEmpty String arg) {
		System.out.println("code throws all kinds of Exceptions");
	}

	@lombok.Rethrows({
		@lombok.Rethrow(value = java.io.FileNotFoundException.class, as = java.lang.IllegalArgumentException.class),
		@lombok.Rethrow(value = java.io.IOException.class, as = java.lang.RuntimeException.class)
	})
	void testFullyCustomizedRethrow() {
		System.out.println("code that throws FileNotFoundException and IOException");
	}
	
	@lombok.Rethrow(as = java.lang.IllegalArgumentException.class, message = "$arg meh.")
	void testExceptionsInSanitizeAlsoGetRethrown(final @lombok.Sanitize.With("filterArg") String arg) {
		System.out.println("code throws all kinds of Exceptions");
	}
	
	String filterArg(final String arg) throws Exception {
		throw new Exception();
	}
}