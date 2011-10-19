import java.lang.Iterable;
class YieldTryBlock {
  YieldTryBlock() {
    super();
  }
  public @java.lang.SuppressWarnings("all") Iterable<String> test() {
    class $YielderTest implements java.util.Iterator<String>, java.lang.Iterable<String> {
      private boolean b;
      private @java.lang.SuppressWarnings("unused") RuntimeException e;
      private java.lang.Throwable $yieldException1;
      private int $id1;
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
        java.lang.Throwable $yieldException;
        while (true)          {
            try 
              {
                switch ($state) {
                case 0 : ;
                    b = true;
                case 1 : ;
                    $yieldException1 = null;
                    $id1 = 6;
                    $state = 2;
                    if (b)
                        {
                          throw new RuntimeException();
                        }
                    $next = "bar";
                    $state = 2;
                    return true;
                case 2 : ;
                    $state = 5;
                    continue ;
                case 3 : ;
                    $next = "foo";
                    $state = 4;
                    return true;
                case 4 : ;
                    $state = 5;
                    continue ;
                case 5 : ;
                    {
                      b = (! b);
                    }
                    if (($yieldException1 != null))
                        {
                          $yieldException = $yieldException1;
                          break ;
                        }
                    $state = $id1;
                    continue ;
                case 6 : ;
                    $state = 1;
                    continue ;
                case 7 : ;
                default : ;
                    return false;
                }
              }
            catch (final java.lang.Throwable $yieldExceptionCaught)               {
                $yieldException = $yieldExceptionCaught;
                switch ($state) {
                case 2 : ;
                    if (($yieldException instanceof RuntimeException))
                        {
                          e = (RuntimeException) $yieldException;
                          $state = 3;
                          continue ;
                        }
                case 3 : ;
                case 4 : ;
                    $yieldException1 = $yieldException;
                    $state = 5;
                    continue ;
                default : ;
                    $state = 7;
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