import lombok.Builder;
import lombok.experimental.Accessors;

@Builder
@Accessors(prefix="_")
class BuilderAccessorsPrefix {
	private final String _name;
	private final String _surname;
	private Integer ignoreMeCauseAccessorsSaysSo;
}