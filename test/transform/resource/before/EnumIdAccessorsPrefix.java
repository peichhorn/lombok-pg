import lombok.EnumId;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

class EnumIdAccessorsPrefix {
	@Accessors(prefix="_")
	@RequiredArgsConstructor
	public enum Status {
		WAITING(0),
		READY(1),
		SKIPPED(-1),
		COMPLETED(5);
		
		@EnumId
		@Getter
		private final int _code;
	}
}