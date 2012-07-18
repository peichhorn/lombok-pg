import java.lang.Iterable;

class YieldIfThenElse {
	
	@java.lang.SuppressWarnings("all")
	public Iterable<String> test() {
		
		class $YielderTest implements java.util.Iterator<java.lang.String>, java.lang.Iterable<java.lang.String>, java.io.Closeable {
			private boolean b;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.String $next;
			
			private $YielderTest() {
			}
			
			public java.util.Iterator<java.lang.String> iterator() {
				if ($state == 0) {
					$state = 1;
					return this;
				} else return new $YielderTest();
			}
			
			public boolean hasNext() {
				if (!$nextDefined) {
					$hasNext = getNext();
					$nextDefined = true;
				}
				return $hasNext;
			}
			
			public java.lang.String next() {
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
				$state = 5;
			}
			
			private boolean getNext() {
				while (true) switch ($state) {
				case 0: 
					$state = 1;
				case 1: 
					b = true;
				case 2: 
					if (!(b)) {
						$state = 3;
						continue;
					}
					$next = "foo";
					$state = 4;
					return true;
				case 3: 
					$next = "bar";
					$state = 4;
					return true;
				case 4: 
					b = !b;
					$state = 2;
					continue;
				case 5: 
				default: 
					return false;
				}
			}
		}
		return new $YielderTest();
	}
}