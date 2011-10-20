import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

class YieldNestedLoop<T, K, V> {
	private Map<T, Map<K, V>> map = new HashMap<T, Map<K, V>>();
	
	@java.lang.SuppressWarnings("all")
	public Iterable<V> values() {
		
		class $YielderValues implements java.util.Iterator<V>, java.lang.Iterable<V> {
			private Map.Entry<T, Map<K, V>> entry;
			private Map.Entry<K, V> subEntry;
			@java.lang.SuppressWarnings("all")
			private java.util.Iterator $entryIter;
			@java.lang.SuppressWarnings("all")
			private java.util.Iterator $subEntryIter;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private V $next;
			
			private $YielderValues() {
				super();
			}
			
			public java.util.Iterator<V> iterator() {
				return new $YielderValues();
			}
			
			public boolean hasNext() {
				if (!$nextDefined) {
					$hasNext = getNext();
					$nextDefined = true;
				}
				return $hasNext;
			}
			
			public V next() {
				if (!hasNext()) {
					throw new java.util.NoSuchElementException();
				}
				$nextDefined = false;
				return $next;
			}
			
			public void remove() {
				throw new java.lang.UnsupportedOperationException();
			}
			
			private boolean getNext() {
				while (true) switch ($state) {
				case 0: 
					$entryIter = map.entrySet().iterator();
				case 1: 
					if (!$entryIter.hasNext()) {
						$state = 3;
						continue;
					}
					entry = (Map.Entry<T, Map<K, V>>)$entryIter.next();
					$subEntryIter = entry.getValue().entrySet().iterator();
				case 2: 
					if (!$subEntryIter.hasNext()) {
						$state = 1;
						continue;
					}
					subEntry = (Map.Entry<K, V>)$subEntryIter.next();
					$next = subEntry.getValue();
					$state = 2;
					return true;
				case 3: 
				default: 
					return false;
				
				}
			}
		}
		return new $YielderValues();
	}
}