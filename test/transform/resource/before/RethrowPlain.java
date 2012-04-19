class RethrowPlain {

	@lombok.Rethrow(value = java.io.FileNotFoundException.class, as = java.lang.IllegalArgumentException.class)
	void testRethrowAs() {
		throw new java.io.FileNotFoundException();
	}

	@lombok.Rethrow(value = InterruptedException.class)
	void testRethrowAsRuntimeException() {
		throw new InterruptedException();
	}
	
	@lombok.Rethrow(as = java.lang.IllegalArgumentException.class, message = "meh.")
	void testRethrowEveryExceptionAsSpecifiedException(final @lombok.Validate.NotEmpty String arg) {
		System.out.println("code throws all kinds of Exceptions");
	}

	@lombok.Rethrows({
		@lombok.Rethrow(value = java.io.FileNotFoundException.class, as = java.lang.IllegalArgumentException.class),
		@lombok.Rethrow(value = java.io.IOException.class, as = java.lang.RuntimeException.class)
	})
	void testFullyCustomizedRethrow(boolean b) {
		if (b) {
			throw new java.io.FileNotFoundException();
		} else {
			throw new java.io.IOException();
		}
	}
	
	@lombok.Rethrow(as = java.lang.IllegalArgumentException.class, message = "$arg meh.")
	void testExceptionsInSanitizeAlsoGetRethrown(final @lombok.Sanitize.With("filterArg") String arg) {
		System.out.println("code throws all kinds of Exceptions");
	}
	
	String filterArg(final String arg) throws Exception {
		throw new Exception();
	}
}