import java.lang.Iterable;
import java.util.List;
import lombok.Functions.Function1;

class YieldAndVal {
	
	
	@java.lang.SuppressWarnings("all")
	public static <S, T> Iterable<T> needsMoreVal(final Iterable<S> values, final Function1<S, List<T>> selector) {
		
		class $YielderNeedsMoreVal implements java.util.Iterator<T>, java.lang.Iterable<T>, java.io.Closeable {
			private S item;
			private java.util.List<T> subItems;
			private T subItem;
			@java.lang.SuppressWarnings("all")
			private java.util.Iterator<S> $itemIter;
			@java.lang.SuppressWarnings("all")
			private java.util.Iterator<T> $subItemIter;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private T $next;
			
			private $YielderNeedsMoreVal() {
				super();
			}
			
			public java.util.Iterator<T> iterator() {
				if ($state == 0) {
					$state = 1;
					return this;
				} else return new $YielderNeedsMoreVal();
			}
			
			public boolean hasNext() {
				if (!$nextDefined) {
					$hasNext = getNext();
					$nextDefined = true;
				}
				return $hasNext;
			}
			
			public T next() {
				if (!hasNext()) {
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
				while (true) switch ($state) {
				case 0: 
					$state = 1;
				
				case 1: 
					$itemIter = values.iterator();
				
				case 2: 
					if (!$itemIter.hasNext()) {
						$state = 4;
						continue;
					}
					item = $itemIter.next();
					subItems = selector.apply(item);
					if (!(subItems != null)) {
						$state = 2;
						continue;
					}
					$subItemIter = subItems.iterator();
				
				case 3: 
					if (!$subItemIter.hasNext()) {
						$state = 2;
						continue;
					}
					subItem = $subItemIter.next();
					$next = subItem;
					$state = 3;
					return true;
				
				case 4: 
				
				default: 
					return false;
				
				}
			}
		}
		return new $YielderNeedsMoreVal();
	}
}