import java.util.Iterator;
import java.lang.Iterable;

class YieldPlain {
	
	@java.lang.SuppressWarnings("all")
	public Iterator<String> simple() {
		
		class $YielderSimple implements java.util.Iterator<java.lang.String>, java.io.Closeable {
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.String $next;
			
			private $YielderSimple() {
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
				$state = 2;
			}
			
			private boolean getNext() {
				while (true) switch ($state) {
				case 0: 
					$state = 1;
				case 1: 
					$next = "A String";
					$state = 2;
					return true;
				case 2: 
				default: 
					return false;
				}
			}
		}
		return new $YielderSimple();
	}
	
	@java.lang.SuppressWarnings("all")
	public Iterator<Long> fib_while() {
		
		class $YielderFibWhile implements java.util.Iterator<java.lang.Long>, java.io.Closeable {
			private long a;
			private long b;
			private long c;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.Long $next;
			
			private $YielderFibWhile() {
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
			
			public void close() {
				$state = 5;
			}
			
			private boolean getNext() {
				while (true) switch ($state) {
				case 0: 
					$state = 1;
				case 1: 
					a = 0;
					b = 1;
				case 2: 
					$next = a;
					$state = 3;
					return true;
				case 3: 
					c = a + b;
					if (!(c < 0)) {
						$state = 4;
						continue;
					}
					$state = 5;
					continue;
				case 4: 
					a = b;
					b = c;
					$state = 2;
					continue;
				case 5: 
				default: 
					return false;
				}
			}
		}
		return new $YielderFibWhile();
	}
	
	@java.lang.SuppressWarnings("all")
	public Iterator<Long> fib_while_2() {
		
		class $YielderFibWhile2 implements java.util.Iterator<java.lang.Long>, java.io.Closeable {
			private long a;
			private long b;
			private long c;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.Long $next;
			
			private $YielderFibWhile2() {
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
			
			public void close() {
				$state = 4;
			}
			
			private boolean getNext() {
				while (true) switch ($state) {
				case 0: 
					$state = 1;
				case 1: 
					a = 0;
					b = 1;
				case 2: 
					if (!(b >= 0)) {
						$state = 4;
						continue;
					}
					$next = a;
					$state = 3;
					return true;
				case 3: 
					c = a + b;
					a = b;
					b = c;
					$state = 2;
					continue;
				case 4: 
				default: 
					return false;
				}
			}
		}
		return new $YielderFibWhile2();
	}
	
	@java.lang.SuppressWarnings("all")
	public Iterable<Long> fib_for() {
		
		class $YielderFibFor implements java.util.Iterator<java.lang.Long>, java.lang.Iterable<java.lang.Long>, java.io.Closeable {
			private long a;
			private long b;
			private long c;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.Long $next;
			
			private $YielderFibFor() {
			}
			
			public java.util.Iterator<java.lang.Long> iterator() {
				if ($state == 0) {
					$state = 1;
					return this;
				} else return new $YielderFibFor();
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
			
			public void close() {
				$state = 4;
			}
			
			private boolean getNext() {
				while (true) switch ($state) {
				case 0: 
					$state = 1;
				case 1: 
					a = 0;
					b = 1;
				case 2: 
					if (!(b >= 0)) {
						$state = 4;
						continue;
					}
					$next = a;
					$state = 3;
					return true;
				case 3: 
					c = a + b;
					a = b;
					b = c;
					$state = 2;
					continue;
				case 4: 
				default: 
					return false;
				}
			}
		}
		return new $YielderFibFor();
	}
	
	@java.lang.SuppressWarnings("all")
	public Iterable<String> complex_foreach(final Iterable<Object> objects) {
		
		class $YielderComplexForeach implements java.util.Iterator<java.lang.String>, java.lang.Iterable<java.lang.String>, java.io.Closeable {
			private Object object;
			private Class<?> c;
			@java.lang.SuppressWarnings("all")
			private java.util.Iterator<Object> $objectIter;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.String $next;
			
			private $YielderComplexForeach() {
			}
			
			public java.util.Iterator<java.lang.String> iterator() {
				if ($state == 0) {
					$state = 1;
					return this;
				} else return new $YielderComplexForeach();
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
				$state = 6;
			}
			
			private boolean getNext() {
				while (true) switch ($state) {
				case 0: 
					$state = 1;
				case 1: 
					$objectIter = objects.iterator();
				case 2: 
					if (!$objectIter.hasNext()) {
						$state = 5;
						continue;
					}
					object = $objectIter.next();
					if (!(object instanceof Class<?>)) {
						$state = 4;
						continue;
					}
					c = (Class<?>)object;
					$next = "A String";
					$state = 3;
					return true;
				case 3: 
					$next = c.getName();
					$state = 5;
					return true;
				case 4: 
					$next = object.toString();
					$state = 2;
					return true;
				case 5: 
					$next = "Another String";
					$state = 6;
					return true;
				case 6: 
				default: 
					return false;
				}
			}
		}
		return new $YielderComplexForeach();
	}
	
	@java.lang.SuppressWarnings("all")
	public Iterator<String> complex(final Iterator<Object> objects) {
		
		class $YielderComplex implements java.util.Iterator<java.lang.String>, java.io.Closeable {
			private Object object;
			private Class<?> c;
			private int $state;
			private boolean $hasNext;
			private boolean $nextDefined;
			private java.lang.String $next;
			
			private $YielderComplex() {
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
				$state = 6;
			}
			
			private boolean getNext() {
				while (true) switch ($state) {
				case 0: 
					$state = 1;
				case 1: 
					$next = "Another String";
					$state = 2;
					return true;
				case 2: 
					if (!(objects.hasNext())) {
						$state = 5;
						continue;
					}
					object = objects.next();
					if (!(object instanceof Class<?>)) {
						$state = 4;
						continue;
					}
					c = (Class<?>)object;
					$next = "A String";
					$state = 3;
					return true;
				case 3: 
					$next = c.getName();
					$state = 5;
					return true;
				case 4: 
					$next = object.toString();
					$state = 2;
					return true;
				case 5: 
					$next = "Another String";
					$state = 6;
					return true;
				case 6: 
				default: 
					return false;
				}
			}
		}
		return new $YielderComplex();
	}
}