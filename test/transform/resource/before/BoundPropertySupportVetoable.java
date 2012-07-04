import lombok.AccessLevel;
import lombok.BoundSetter;

class BoundPropertySupportVetoable {
	@BoundSetter(vetoable = true) private String name;
	@BoundSetter(throwVetoException = true) private String surname;
}
