import java.lang.Iterable;
class YieldIfThenElse {
  YieldIfThenElse() {
    super();
  }
  public @java.lang.SuppressWarnings("all") Iterable<String> test() {
    class $YielderTest implements java.util.Iterator<String>, java.lang.Iterable<String> {
      private boolean b;
      private int $state;
      private boolean $hasNext;
      private boolean $nextDefined;
      private String $next;
      private $YielderTest() {
        super();
      }
      public java.util.Iterator<String> iterator() {
        return new $YielderTest();
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
                    $state = 2;
                    continue ;
                  }
              $next = "foo";
              $state = 3;
              return true;
          case 2 : ;
              $next = "bar";
              $state = 3;
              return true;
          case 3 : ;
              b = (! b);
              $state = 1;
              continue ;
          case 4 : ;
          default : ;
              return false;
          }
      }
    }
    return new $YielderTest();
  }
}