import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
class BuilderGeneric<K extends Comparable<K>, V extends List<K>> {
	private final String foo;
	private final Map<K, V> bar = new HashMap<K, V>();
	
	@Builder.Extension
	private void foo(final Class<?> clazz) {
		this.foo = clazz.getSimpleName();
	}
}

@Builder
class DomainContainer<D> {
	private final int a;
	private final D domain;
}
