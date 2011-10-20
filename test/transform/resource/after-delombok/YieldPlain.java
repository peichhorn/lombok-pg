import java.util.Iterator;
import java.lang.Iterable;

class YieldPlain {
	
	@java.lang.SuppressWarnings("all")
	public Iterator<String> simple() {
		
		class $YielderSimple implements java.util.Iterator<java.lang.String> {
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.String $next;
			
			private $YielderSimple() {
				super();
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
					$next = "A String";
					$state = 1;
					return true;
				case 1: 
				default: 
					return false;
				}
			}
		}
		return new $YielderSimple();
	}
	
	@java.lang.SuppressWarnings("all")
	public Iterator<Long> fib_while() {
		
		class $YielderFibWhile implements java.util.Iterator<java.lang.Long> {
			private long a;
			private long b;
			private long c;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.Long $next;
			
			private $YielderFibWhile() {
				super();
			}
			
			public boolean hasNext() {
				if (!$nextDefined) {
					$hasNext = getNext();
					$nextDefined = true;
				}
				return $hasNext;
			}
			
			public java.lang.Long next() {
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
					a = 0;
					b = 1;
				case 1: 
					$next = a;
					$state = 2;
					return true;
				case 2: 
					c = a + b;
					if (!(c < 0)) {
						$state = 3;
						continue;
					}
					$state = 4;
					continue;
				case 3: 
					a = b;
					b = c;
					$state = 1;
					continue;
				case 4: 
				default: 
					return false;
				}
			}
		}
		return new $YielderFibWhile();
	}
	
	@java.lang.SuppressWarnings("all")
	public Iterator<Long> fib_while_2() {
		
		class $YielderFibWhile2 implements java.util.Iterator<java.lang.Long> {
			private long a;
			private long b;
			private long c;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.Long $next;
			
			private $YielderFibWhile2() {
				super();
			}
			
			public boolean hasNext() {
				if (!$nextDefined) {
					$hasNext = getNext();
					$nextDefined = true;
				}
				return $hasNext;
			}
			
			public java.lang.Long next() {
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
					a = 0;
					b = 1;
				case 1: 
					if (!(b >= 0)) {
						$state = 3;
						continue;
					}
					$next = a;
					$state = 2;
					return true;
				case 2: 
					c = a + b;
					a = b;
					b = c;
					$state = 1;
					continue;
				case 3: 
				default: 
					return false;
				}
			}
		}
		return new $YielderFibWhile2();
	}
	
	@java.lang.SuppressWarnings("all")
	public Iterable<Long> fib_for() {
		
		class $YielderFibFor implements java.util.Iterator<java.lang.Long>, java.lang.Iterable<java.lang.Long> {
			private long a;
			private long b;
			private long c;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.Long $next;
			
			private $YielderFibFor() {
				super();
			}
			
			public java.util.Iterator<java.lang.Long> iterator() {
				return new $YielderFibFor();
			}
			
			public boolean hasNext() {
				if (!$nextDefined) {
					$hasNext = getNext();
					$nextDefined = true;
				}
				return $hasNext;
			}
			
			public java.lang.Long next() {
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
					a = 0;
					b = 1;
				case 1: 
					if (!(b >= 0)) {
						$state = 3;
						continue;
					}
					$next = a;
					$state = 2;
					return true;
				case 2: 
					c = a + b;
					a = b;
					b = c;
					$state = 1;
					continue;
				case 3: 
				default: 
					return false;
				}
			}
		}
		return new $YielderFibFor();
	}
	
	@java.lang.SuppressWarnings("all")
	public Iterable<String> complex_foreach(final Iterable<Object> objects) {
		
		class $YielderComplexForeach implements java.util.Iterator<java.lang.String>, java.lang.Iterable<java.lang.String> {
			private Object object;
			private Class<?> c;
			@java.lang.SuppressWarnings("all")
			private java.util.Iterator $objectIter;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.String $next;
			
			private $YielderComplexForeach() {
				super();
			}
			
			public java.util.Iterator<java.lang.String> iterator() {
				return new $YielderComplexForeach();
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
					$objectIter = objects.iterator();
				case 1: 
					if (!$objectIter.hasNext()) {
						$state = 4;
						continue;
					}
					object = (Object)$objectIter.next();
					if (!(object instanceof Class<?>)) {
						$state = 3;
						continue;
					}
					c = (Class<?>)object;
					$next = "A String";
					$state = 2;
					return true;
				case 2: 
					$next = c.getName();
					$state = 4;
					return true;
				case 3: 
					$next = object.toString();
					$state = 1;
					return true;
				case 4: 
					$next = "Another String";
					$state = 5;
					return true;
				case 5: 
				default: 
					return false;
				}
			}
		}
		return new $YielderComplexForeach();
	}
	
	@java.lang.SuppressWarnings("all")
	public Iterator<String> complex(final Iterator<Object> objects) {
		
		class $YielderComplex implements java.util.Iterator<java.lang.String> {
			private Object object;
			private Class<?> c;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.String $next;
			
			private $YielderComplex() {
				super();
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
					$next = "Another String";
					$state = 1;
					return true;
				case 1: 
					if (!(objects.hasNext())) {
						$state = 4;
						continue;
					}
					object = objects.next();
					if (!(object instanceof Class<?>)) {
						$state = 3;
						continue;
					}
					c = (Class<?>)object;
					$next = "A String";
					$state = 2;
					return true;
				case 2: 
					$next = c.getName();
					$state = 4;
					return true;
				case 3: 
					$next = object.toString();
					$state = 1;
					return true;
				case 4: 
					$next = "Another String";
					$state = 5;
					return true;
				case 5: 
				default: 
					return false;
				}
			}
		}
		return new $YielderComplex();
	}
}