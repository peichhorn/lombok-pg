import lombok.AccessLevel;
import lombok.BoundSetter;

class BoundSetterVetoable {
	@BoundSetter(vetoable = true) private String name;
	@BoundSetter(throwVetoException = true) private String surname;
}
