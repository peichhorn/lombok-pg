import lombok.AccessLevel;

import lombok.Bindable;
import lombok.BoundSetter;

@Bindable
class BindablePlain {
	@BoundSetter int i;
	@BoundSetter(AccessLevel.PUBLIC) String s;
	@BoundSetter(AccessLevel.PROTECTED) float f;
	@BoundSetter(AccessLevel.PACKAGE) Object o;
	@BoundSetter(AccessLevel.PRIVATE) double d;
}
