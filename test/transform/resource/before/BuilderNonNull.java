import lombok.Builder;
import lombok.NonNull;

@Builder
class BuilderNonNull {
	@NonNull
	private String nonNullValue;
	private String anotherValue;
}