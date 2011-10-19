import static lombok.Yield.yield;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

class YieldNestedLoop<T, K, V> {
	private Map<T, Map<K, V>> map = new HashMap<T, Map<K, V>>();
	
	public Iterable<V> values() {
		for (Map.Entry<T, Map<K, V>> entry : map.entrySet()) {
			for (Map.Entry<K, V> subEntry : entry.getValue().entrySet()) {
				yield(subEntry.getValue());
			}
		}
	}
}
