import lombok.Validate;
import lombok.Validate.*;

class ValidateOnConstructor {
	private final String s;
	
	@Validate
	public ValidateOnConstructor(@NotEmpty final String s) {
		this.s = s;
	}
	
	static class CheckedException extends Exception {
		private final String foo;
		
		@Validate
		public CheckedException(@NotEmpty String message, @NotNull Throwable cause) {
			super(message, cause);
			foo = message;
		}
	}
}