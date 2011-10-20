import java.lang.Iterable;

class YieldIfThenElse {
	
	@java.lang.SuppressWarnings("all")
	public Iterable<String> test() {
		
		class $YielderTest implements java.util.Iterator<java.lang.String>, java.lang.Iterable<java.lang.String> {
			private boolean b;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.String $next;
			
			private $YielderTest() {
				super();
			}
			
			public java.util.Iterator<java.lang.String> iterator() {
				return new $YielderTest();
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
			
			private boolean getNext() {
				while (true) switch ($state) {
				case 0: 
					b = true;
				case 1: 
					if (!(b)) {
						$state = 2;
						continue;
					}
					$next = "foo";
					$state = 3;
					return true;
				case 2: 
					$next = "bar";
					$state = 3;
					return true;
				case 3: 
					b = !b;
					$state = 1;
					continue;
				case 4: 
				default: 
					return false;
				}
			}
		}
		return new $YielderTest();
	}
}