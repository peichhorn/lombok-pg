import java.util.Iterator;
import java.lang.Iterable;
class YieldPlain {
  YieldPlain() {
    super();
  }
  public @java.lang.SuppressWarnings("all") Iterator<String> simple() {
    class $YielderSimple implements java.util.Iterator<String>, java.io.Closeable {
      private int $state;
      private boolean $hasNext;
      private boolean $nextDefined;
      private String $next;
      private $YielderSimple() {
        super();
      }
      public boolean hasNext() {
        if ((! $nextDefined))
            {
              $hasNext = getNext();
              $nextDefined = true;
            }
        return $hasNext;
      }
      public String next() {
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
        $state = 2;
      }
      private boolean getNext() {
        while (true)          switch ($state) {
          case 0 : ;
              $state = 1;
          case 1 : ;
              $next = "A String";
              $state = 2;
              return true;
          case 2 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderSimple();
  }
  public @java.lang.SuppressWarnings("all") Iterator<Long> fib_while() {
    class $YielderFibWhile implements java.util.Iterator<Long>, java.io.Closeable {
      private long a;
      private long b;
      private long c;
      private int $state;
      private boolean $hasNext;
      private boolean $nextDefined;
      private Long $next;
      private $YielderFibWhile() {
        super();
      }
      public boolean hasNext() {
        if ((! $nextDefined))
            {
              $hasNext = getNext();
              $nextDefined = true;
            }
        return $hasNext;
      }
      public Long next() {
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
        $state = 5;
      }
      private boolean getNext() {
        while (true)          switch ($state) {
          case 0 : ;
              $state = 1;
          case 1 : ;
              a = 0;
              b = 1;
          case 2 : ;
              $next = a;
              $state = 3;
              return true;
          case 3 : ;
              c = (a + b);
              if ((! (c < 0)))
                  {
                    $state = 4;
                    continue ;
                  }
              $state = 5;
              continue ;
          case 4 : ;
              a = b;
              b = c;
              $state = 2;
              continue ;
          case 5 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderFibWhile();
  }
  public @java.lang.SuppressWarnings("all") Iterator<Long> fib_while_2() {
    class $YielderFibWhile2 implements java.util.Iterator<Long>, java.io.Closeable {
      private long a;
      private long b;
      private long c;
      private int $state;
      private boolean $hasNext;
      private boolean $nextDefined;
      private Long $next;
      private $YielderFibWhile2() {
        super();
      }
      public boolean hasNext() {
        if ((! $nextDefined))
            {
              $hasNext = getNext();
              $nextDefined = true;
            }
        return $hasNext;
      }
      public Long next() {
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
              a = 0;
              b = 1;
          case 2 : ;
              if ((! (b >= 0)))
                  {
                    $state = 4;
                    continue ;
                  }
              $next = a;
              $state = 3;
              return true;
          case 3 : ;
              c = (a + b);
              a = b;
              b = c;
              $state = 2;
              continue ;
          case 4 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderFibWhile2();
  }
  public @java.lang.SuppressWarnings("all") Iterable<Long> fib_for() {
    class $YielderFibFor implements java.util.Iterator<Long>, java.lang.Iterable<Long>, java.io.Closeable {
      private long a;
      private long b;
      private long c;
      private int $state;
      private boolean $hasNext;
      private boolean $nextDefined;
      private Long $next;
      private $YielderFibFor() {
        super();
      }
      public java.util.Iterator<Long> iterator() {
        if (($state == 0))
            {
              $state = 1;
              return this;
            }
        else
            return new $YielderFibFor();
      }
      public boolean hasNext() {
        if ((! $nextDefined))
            {
              $hasNext = getNext();
              $nextDefined = true;
            }
        return $hasNext;
      }
      public Long next() {
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
              a = 0;
              b = 1;
          case 2 : ;
              if ((! (b >= 0)))
                  {
                    $state = 4;
                    continue ;
                  }
              $next = a;
              $state = 3;
              return true;
          case 3 : ;
              c = (a + b);
              a = b;
              b = c;
              $state = 2;
              continue ;
          case 4 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderFibFor();
  }
  public @java.lang.SuppressWarnings("all") Iterable<String> complex_foreach(final Iterable<Object> objects) {
    class $YielderComplexForeach implements java.util.Iterator<String>, java.lang.Iterable<String>, java.io.Closeable {
      private Object object;
      private Class<?> c;
      private @java.lang.SuppressWarnings("all") java.util.Iterator $objectIter;
      private int $state;
      private boolean $hasNext;
      private boolean $nextDefined;
      private String $next;
      private $YielderComplexForeach() {
        super();
      }
      public java.util.Iterator<String> iterator() {
        if (($state == 0))
            {
              $state = 1;
              return this;
            }
        else
            return new $YielderComplexForeach();
      }
      public boolean hasNext() {
        if ((! $nextDefined))
            {
              $hasNext = getNext();
              $nextDefined = true;
            }
        return $hasNext;
      }
      public String next() {
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
        $state = 6;
      }
      private boolean getNext() {
        while (true)          switch ($state) {
          case 0 : ;
              $state = 1;
          case 1 : ;
              $objectIter = objects.iterator();
          case 2 : ;
              if ((! $objectIter.hasNext()))
                  {
                    $state = 5;
                    continue ;
                  }
              object = (Object) $objectIter.next();
              if ((! (object instanceof Class<?>)))
                  {
                    $state = 4;
                    continue ;
                  }
              c = (Class<?>) object;
              $next = "A String";
              $state = 3;
              return true;
          case 3 : ;
              $next = c.getName();
              $state = 5;
              return true;
          case 4 : ;
              $next = object.toString();
              $state = 2;
              return true;
          case 5 : ;
              $next = "Another String";
              $state = 6;
              return true;
          case 6 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderComplexForeach();
  }
  public @java.lang.SuppressWarnings("all") Iterator<String> complex(final Iterator<Object> objects) {
    class $YielderComplex implements java.util.Iterator<String>, java.io.Closeable {
      private Object object;
      private Class<?> c;
      private int $state;
      private boolean $hasNext;
      private boolean $nextDefined;
      private String $next;
      private $YielderComplex() {
        super();
      }
      public boolean hasNext() {
        if ((! $nextDefined))
            {
              $hasNext = getNext();
              $nextDefined = true;
            }
        return $hasNext;
      }
      public String next() {
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
        $state = 6;
      }
      private boolean getNext() {
        while (true)          switch ($state) {
          case 0 : ;
              $state = 1;
          case 1 : ;
              $next = "Another String";
              $state = 2;
              return true;
          case 2 : ;
              if ((! objects.hasNext()))
                  {
                    $state = 5;
                    continue ;
                  }
              object = objects.next();
              if ((! (object instanceof Class<?>)))
                  {
                    $state = 4;
                    continue ;
                  }
              c = (Class<?>) object;
              $next = "A String";
              $state = 3;
              return true;
          case 3 : ;
              $next = c.getName();
              $state = 5;
              return true;
          case 4 : ;
              $next = object.toString();
              $state = 2;
              return true;
          case 5 : ;
              $next = "Another String";
              $state = 6;
              return true;
          case 6 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderComplex();
  }
}