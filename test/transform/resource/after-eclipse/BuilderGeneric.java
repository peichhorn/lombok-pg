import java.util.Map;
import java.util.HashMap;

@lombok.Builder
class BuilderGeneric<K, V> {
	private final String foo;
	private final Map<K, V> dictionary = new HashMap<K, V>();
}