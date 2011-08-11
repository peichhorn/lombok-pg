import lombok.AccessLevel;
import lombok.BoundPropertySupport;
import lombok.BoundSetter;

@BoundPropertySupport
class BoundPropertySupportPlain {
	@BoundSetter int i;
	@BoundSetter(AccessLevel.PUBLIC) String s;
	@BoundSetter(AccessLevel.PROTECTED) float f;
	@BoundSetter(AccessLevel.PACKAGE) Object o;
	@BoundSetter(AccessLevel.PRIVATE) double d;
}
