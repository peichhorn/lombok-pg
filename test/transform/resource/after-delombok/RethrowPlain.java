class RethrowPlain {

	@java.lang.SuppressWarnings("all")
	void testRethrowAs() {
		try {
			System.out.println("code that throws FileNotFoundException");
		} catch (java.io.FileNotFoundException $e1) {
			throw new java.lang.IllegalArgumentException($e1);
		}
	}

	@java.lang.SuppressWarnings("all")
	void testRethrowAsRuntimeException() {
		try {
			System.out.println("code that might throw InterruptedException due to cancelation");
		} catch (java.lang.InterruptedException $e1) {
			throw new java.lang.RuntimeException($e1);
		}
	}

	@java.lang.SuppressWarnings("all")
	void testRethrowEveryExceptionAsSpecifiedException(final String arg) {
		try {
			System.out.println("code throws all kinds of Exceptions");
		} catch (java.lang.Exception $e1) {
			throw new java.lang.IllegalArgumentException("meh.", $e1);
		}
	}

	@java.lang.SuppressWarnings("all")
	void testFullyCustomizedRethrow() {
		try {
			System.out.println("code that throws FileNotFoundException and IOException");
		} catch (java.io.FileNotFoundException $e1) {
			throw new java.lang.IllegalArgumentException($e1);
		} catch (java.io.IOException $e2) {
			throw new java.lang.RuntimeException($e2);
		}
	}
}