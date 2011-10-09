import java.lang.Iterable;

class YieldTryBlock {
	@java.lang.SuppressWarnings("all")
	public Iterable<String> test() {
		
		class $YielderTest implements java.util.Iterator<java.lang.String>, java.lang.Iterable<java.lang.String> {
			private boolean b;
			@java.lang.SuppressWarnings("unused")
			private RuntimeException e;
			private java.lang.Throwable $exception1;
			private int $id1;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.String $next;
			
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
				java.lang.Throwable $exception;
				while (true) {
					try {
						switch ($state) {
						case 0: 
							b = true;
						
						case 1: 
							$exception1 = null;
							$id1 = 6;
							$state = 2;
							if (b) {
								throw new RuntimeException();
							}
							$next = "bar";
							$state = 2;
							return true;
						
						case 2: 
							$state = 5;
							continue;
						
						case 3: 
							$next = "foo";
							$state = 4;
							return true;
						
						case 4: 
							$state = 5;
							continue;
						
						case 5: 
							{
								b = !b;
							}
							if ($exception1 != null) {
								$exception = $exception1;
								break;
							}
							$state = $id1;
							continue;
						
						case 6: 
							$state = 1;
							continue;
						
						case 7: 
						
						default: 
							return false;
						
						}
					} catch (final java.lang.Throwable $e) {
						$exception = $e;
						switch ($state) {
						case 2: 
							if ($exception instanceof RuntimeException) {
								e = (RuntimeException)$exception;
								$state = 3;
								continue;
							}
						
						case 3: 
						
						case 4: 
							$exception1 = $exception;
							$state = 5;
							continue;
						
						default: 
							$state = 7;
							java.util.ConcurrentModificationException $e = new java.util.ConcurrentModificationException();
							$e.initCause($exception);
							throw $e;
						
						}
					}
				}
			}
		}
		return new $YielderTest();
	}
}