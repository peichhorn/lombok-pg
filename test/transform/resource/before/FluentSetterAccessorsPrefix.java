@lombok.experimental.Accessors(prefix = "_")
@lombok.NoArgsConstructor
class FluentSetterAccessorsPrefix<T, K> {
	@lombok.FluentSetter
	int _fluent;
}
