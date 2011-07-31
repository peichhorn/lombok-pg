class FluentSetterPlain {
	@lombok.Setter
	int nonfluent;
	@lombok.FluentSetter
	int fluent;
	@lombok.Setter(value=lombok.AccessLevel.PRIVATE)
	int nonfluent_accessLevel;
	@lombok.FluentSetter(value=lombok.AccessLevel.PRIVATE)
	int fluent_accessLevel;	
}