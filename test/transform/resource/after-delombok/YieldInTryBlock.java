import java.lang.Iterable;

class YieldTryBlock {
	
	@java.lang.SuppressWarnings("all")
	public Iterable<String> test() {
		
		class $YielderTest implements java.util.Iterator<java.lang.String>, java.lang.Iterable<java.lang.String> {
			private boolean b;
			@java.lang.SuppressWarnings("unused")
			private RuntimeException e;
			private java.lang.Throwable $yieldException1;
			private int $state1;
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
				java.lang.Throwable $yieldException;
				while (true) {
					try {
						switch ($state) {
						case 0: 
							b = true;
						case 1: 
							$yieldException1 = null;
							$state1 = 1;
							$state = 2;
						case 2: 
							if (b) {
								throw new RuntimeException();
							}
							$next = "bar";
							$state = 4;
							return true;
						case 3: 
							$next = "foo";
							$state = 4;
							return true;
						case 4: 
							{
								b = !b;
							}
							if ($yieldException1 != null) {
								$yieldException = $yieldException1;
								break;
							}
							$state = $state1;
							continue;
						
						case 5: 
						
						default: 
							return false;
						}
					} catch (final java.lang.Throwable $yieldExceptionCaught) {
						$yieldException = $yieldExceptionCaught;
						switch ($state) {
						case 2: 
							if ($yieldException instanceof RuntimeException) {
								e = (RuntimeException)$yieldException;
								$state = 3;
								continue;
							}
						case 3: 
							$yieldException1 = $yieldException;
							$state = 4;
							continue;
						default: 
							$state = 5;
							java.util.ConcurrentModificationException $yieldExceptionUnhandled = new java.util.ConcurrentModificationException();
							$yieldExceptionUnhandled.initCause($yieldException);
							throw $yieldExceptionUnhandled;
						}
					}
				}
			}
		}
		return new $YielderTest();
	}
}