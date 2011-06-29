class RethrowPlain {
  RethrowPlain() {
    super();
  }
  @lombok.Rethrow(value = java.io.FileNotFoundException.class,as = java.lang.IllegalArgumentException.class) @java.lang.SuppressWarnings("all") void testRethrowAs() {
    try 
      {
        System.out.println("code that throws FileNotFoundException");
      }
    catch (final java.io.FileNotFoundException $e1)       {
        throw new java.lang.IllegalArgumentException($e1);
      }
  }
  @lombok.Rethrow(value = java.lang.InterruptedException.class) @java.lang.SuppressWarnings("all") void testRethrowAsRuntimeException() {
    try 
      {
        System.out.println("code that might throw InterruptedException due to cancelation");
      }
    catch (final java.lang.InterruptedException $e1)       {
        throw new java.lang.RuntimeException($e1);
      }
  }
  @lombok.Rethrow(as = java.lang.IllegalArgumentException.class,message = "meh.") @java.lang.SuppressWarnings("all") void testRethrowEveryExceptionAsSpecifiedException(final String arg) {
    try 
      {
        System.out.println("code throws all kinds of Exceptions");
      }
    catch (final java.lang.RuntimeException $e1)       {
        throw $e1;
      }
    catch (final java.lang.Exception $e2)       {
        throw new java.lang.IllegalArgumentException("meh.", $e2);
      }
  }
  @lombok.Rethrows({@lombok.Rethrow(value = java.io.FileNotFoundException.class,as = java.lang.IllegalArgumentException.class), @lombok.Rethrow(value = java.io.IOException.class,as = java.lang.RuntimeException.class)}) @java.lang.SuppressWarnings("all") void testFullyCustomizedRethrow() {
    try 
      {
        System.out.println("code that throws FileNotFoundException and IOException");
      }
    catch (final java.io.FileNotFoundException $e1)       {
        throw new java.lang.IllegalArgumentException($e1);
      }
    catch (final java.io.IOException $e2)       {
        throw new java.lang.RuntimeException($e2);
      }
  }
}