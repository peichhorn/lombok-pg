import java.util.Iterator;
import java.lang.Iterable;

class YieldPlain {
  YieldPlain() {
    super();
  }

  public @java.lang.SuppressWarnings("all") Iterator<String> simple() {
    class $YielderSimple implements java.util.Iterator<String> {
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

      private boolean getNext() {
        while (true)          switch ($state) {
          case 0 : ;
              $next = "A String";
              $state = (+ 1);
              return true;
          case 1 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderSimple();
  }
  public @java.lang.SuppressWarnings("all") Iterable<String> testIfThenElse() {
    class $YielderTestIfThenElse implements java.util.Iterator<String>, java.lang.Iterable<String> {
      private boolean b;
      private int $state;
      private boolean $hasNext;
      private boolean $nextDefined;
      private String $next;
      private $YielderTestIfThenElse() {
        super();
      }
      public java.util.Iterator<String> iterator() {
        return new $YielderTestIfThenElse();
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
      private boolean getNext() {
        while (true)          switch ($state) {
          case 0 : ;
              b = true;
          case 1 : ;
              if ((! b))
                  {
                    $state = (+ 3);
                    continue ;
                  }
              $next = "foo";
              $state = (+ 2);
              return true;
          case 2 : ;
              $state = (+ 4);
              continue ;
          case 3 : ;
              $next = "bar";
              $state = (+ 4);
              return true;
          case 4 : ;
              b = (! b);
              $state = (+ 1);
              continue ;
          case 5 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderTestIfThenElse();
  }
  public @java.lang.SuppressWarnings("all") Iterator<Long> fib_while() {
    class $YielderFibWhile implements java.util.Iterator<Long> {
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

      private boolean getNext() {
        while (true)          switch ($state) {
          case 0 : ;
              a = 0;
              b = 1;
          case 1 : ;
              $next = a;
              $state = (+ 2);
              return true;
          case 2 : ;
              c = (a + b);
              if ((! (c < 0)))
                  {
                    $state = (+ 3);
                    continue ;
                  }
              $state = (+ 4);
              continue ;
          case 3 : ;
              a = b;
              b = c;
              $state = (+ 1);
              continue ;
          case 4 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderFibWhile();
  }

  public @java.lang.SuppressWarnings("all") Iterator<Long> fib_while_2() {
    class $YielderFibWhile2 implements java.util.Iterator<Long> {
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

      private boolean getNext() {
        while (true)          switch ($state) {
          case 0 : ;
              a = 0;
              b = 1;
          case 1 : ;
              if ((! (b >= 0)))
                  {
                    $state = (+ 3);
                    continue ;
                  }
              $next = a;
              $state = (+ 2);
              return true;
          case 2 : ;
              c = (a + b);
              a = b;
              b = c;
              $state = (+ 1);
              continue ;
          case 3 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderFibWhile2();
  }

  public @java.lang.SuppressWarnings("all") Iterable<Long> fib_for() {
    class $YielderFibFor implements java.util.Iterator<Long>, java.lang.Iterable<Long> {
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

      private boolean getNext() {
        while (true)          switch ($state) {
          case 0 : ;
              a = 0;
              b = 1;
          case 1 : ;
              if ((! (b >= 0)))
                  {
                    $state = (+ 3);
                    continue ;
                  }
              $next = a;
              $state = (+ 2);
              return true;
          case 2 : ;
              c = (a + b);
              a = b;
              b = c;
              $state = (+ 1);
              continue ;
          case 3 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderFibFor();
  }

  public @java.lang.SuppressWarnings("all") Iterable<String> complex_foreach(final Iterable<Object> objects) {
    class $YielderComplexForeach implements java.util.Iterator<String>, java.lang.Iterable<String> {
      private Object object;
      private Class<?> c;
      private java.util.Iterator $objectIter;
      private int $state;
      private boolean $hasNext;
      private boolean $nextDefined;
      private String $next;

      private $YielderComplexForeach() {
        super();
      }

      public java.util.Iterator<String> iterator() {
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

      private boolean getNext() {
        while (true)          switch ($state) {
          case 0 : ;
              $objectIter = objects.iterator();
          case 1 : ;
              if ((! $objectIter.hasNext()))
                  {
                    $state = (+ 6);
                    continue ;
                  }
              object = (Object) $objectIter.next();
              if ((! (object instanceof Class<?>)))
                  {
                    $state = (+ 4);
                    continue ;
                  }
              c = (Class<?>) object;
              $next = "A String";
              $state = (+ 2);
              return true;
          case 2 : ;
              $next = c.getName();
              $state = (+ 3);
              return true;
          case 3 : ;
              $state = (+ 6);
              continue ;
          case 4 : ;
              $next = object.toString();
              $state = (+ 5);
              return true;
          case 5 : ;
              $state = (+ 1);
              continue ;
          case 6 : ;
              $next = "Another String";
              $state = (+ 7);
              return true;
          case 7 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderComplexForeach();
  }

  public @java.lang.SuppressWarnings("all") Iterator<String> complex(final Iterator<Object> objects) {
    class $YielderComplex implements java.util.Iterator<String> {
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

      private boolean getNext() {
        while (true)          switch ($state) {
          case 0 : ;
              $next = "Another String";
              $state = (+ 1);
              return true;
          case 1 : ;
              if ((! objects.hasNext()))
                  {
                    $state = (+ 6);
                    continue ;
                  }
              object = objects.next();
              if ((! (object instanceof Class<?>)))
                  {
                    $state = (+ 4);
                    continue ;
                  }
              c = (Class<?>) object;
              $next = "A String";
              $state = (+ 2);
              return true;
          case 2 : ;
              $next = c.getName();
              $state = (+ 3);
              return true;
          case 3 : ;
              $state = (+ 6);
              continue ;
          case 4 : ;
              $next = object.toString();
              $state = (+ 5);
              return true;
          case 5 : ;
              $state = (+ 1);
              continue ;
          case 6 : ;
              $next = "Another String";
              $state = (+ 7);
              return true;
          case 7 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderComplex();
  }
}
