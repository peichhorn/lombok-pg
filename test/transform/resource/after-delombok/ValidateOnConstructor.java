import lombok.Validate.*;

class ValidateOnConstructor {
	private final String s;
	
	@java.lang.SuppressWarnings("all")
	public ValidateOnConstructor(final String s) {
		if (s == null) {
			throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "s", 1));
		}
		if (s.isEmpty()) {
			throw new java.lang.IllegalArgumentException(java.lang.String.format("The validated object \'%s\' (argument #%s) is empty", "s", 1));
		}
		this.s = s;
	}
	
	static class CheckedException extends Exception {
		private final String foo;
		
		@java.lang.SuppressWarnings("all")
		public CheckedException(String message, Throwable cause) {
			super(message, cause);
			if (message == null) {
				throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "message", 1));
			}
			if (message.isEmpty()) {
				throw new java.lang.IllegalArgumentException(java.lang.String.format("The validated object \'%s\' (argument #%s) is empty", "message", 1));
			}
			if (cause == null) {
				throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "cause", 2));
			}
			foo = message;
		}
	}
}