import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
class YieldNestedLoop<T, K, V> {
  private Map<T, Map<K, V>> map = new HashMap<T, Map<K, V>>();
  YieldNestedLoop() {
    super();
  }
  public @java.lang.SuppressWarnings("all") Iterable<V> values() {
    class $YielderValues implements java.util.Iterator<V>, java.lang.Iterable<V>, java.io.Closeable {
      private Map.Entry<T, Map<K, V>> entry;
      private Map.Entry<K, V> subEntry;
      private @java.lang.SuppressWarnings("all") java.util.Iterator<Map.Entry<T, Map<K, V>>> $entryIter;
      private @java.lang.SuppressWarnings("all") java.util.Iterator<Map.Entry<K, V>> $subEntryIter;
      private int $state;
      private boolean $hasNext;
      private boolean $nextDefined;
      private V $next;
      private $YielderValues() {
        super();
      }
      public java.util.Iterator<V> iterator() {
        if (($state == 0))
            {
              $state = 1;
              return this;
            }
        else
            return new $YielderValues();
      }
      public boolean hasNext() {
        if ((! $nextDefined))
            {
              $hasNext = getNext();
              $nextDefined = true;
            }
        return $hasNext;
      }
      public V next() {
        if ((! hasNext()))
            {
              throw new java.util.NoSuchElementException();
            }
        $nextDefined = false;
        return $next;
      }
      public void remove() {
        throw new java.lang.UnsupportedOperationException();
      }
      public void close() {
        $state = 4;
      }
      private boolean getNext() {
        while (true)          switch ($state) {
          case 0 : ;
              $state = 1;
          case 1 : ;
              $entryIter = map.entrySet().iterator();
          case 2 : ;
              if ((! $entryIter.hasNext()))
                  {
                    $state = 4;
                    continue ;
                  }
              entry = $entryIter.next();
              $subEntryIter = entry.getValue().entrySet().iterator();
          case 3 : ;
              if ((! $subEntryIter.hasNext()))
                  {
                    $state = 2;
                    continue ;
                  }
              subEntry = $subEntryIter.next();
              $next = subEntry.getValue();
              $state = 3;
              return true;
          case 4 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderValues();
  }
}