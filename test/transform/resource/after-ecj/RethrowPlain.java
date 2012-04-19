class RethrowPlain {
  RethrowPlain() {
    super();
  }
  @lombok.Rethrow(value = java.io.FileNotFoundException.class,as = java.lang.IllegalArgumentException.class) @java.lang.SuppressWarnings("all") void testRethrowAs() {
    try 
      {
        throw new java.io.FileNotFoundException();
      }
    catch (final java.io.FileNotFoundException $e1)       {
        throw new java.lang.IllegalArgumentException($e1);
      }
  }
  @lombok.Rethrow(value = InterruptedException.class) @java.lang.SuppressWarnings("all") void testRethrowAsRuntimeException() {
    try 
      {
        throw new InterruptedException();
      }
    catch (final java.lang.InterruptedException $e1)       {
        throw new java.lang.RuntimeException($e1);
      }
  }
  @lombok.Rethrow(as = java.lang.IllegalArgumentException.class,message = "meh.") @java.lang.SuppressWarnings("all") void testRethrowEveryExceptionAsSpecifiedException(final @lombok.Validate.NotEmpty String arg) {
    try 
      {
        if ((arg == null))
            {
              throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "arg", 1));
            }
        if (arg.isEmpty())
            {
              throw new java.lang.IllegalArgumentException(java.lang.String.format("The validated object \'%s\' (argument #%s) is empty", "arg", 1));
            }
        System.out.println("code throws all kinds of Exceptions");
      }
    catch (final java.lang.RuntimeException $e1)       {
        throw $e1;
      }
    catch (final java.lang.Exception $e2)       {
        throw new java.lang.IllegalArgumentException(java.lang.String.format("meh."), $e2);
      }
  }
  @lombok.Rethrows({@lombok.Rethrow(value = java.io.FileNotFoundException.class,as = java.lang.IllegalArgumentException.class), @lombok.Rethrow(value = java.io.IOException.class,as = java.lang.RuntimeException.class)}) @java.lang.SuppressWarnings("all") void testFullyCustomizedRethrow(boolean b) {
    try 
      {
        if (b)
            {
              throw new java.io.FileNotFoundException();
            }
        else
            {
              throw new java.io.IOException();
            }
      }
    catch (final java.io.FileNotFoundException $e1)       {
        throw new java.lang.IllegalArgumentException($e1);
      }
    catch (final java.io.IOException $e2)       {
        throw new java.lang.RuntimeException($e2);
      }
  }
  @lombok.Rethrow(as = java.lang.IllegalArgumentException.class,message = "$arg meh.") @java.lang.SuppressWarnings("all") void testExceptionsInSanitizeAlsoGetRethrown(final @lombok.Sanitize.With("filterArg") String arg) {
    try 
      {
        final String sanitizedArg = filterArg(arg);
        System.out.println("code throws all kinds of Exceptions");
      }
    catch (final java.lang.RuntimeException $e1)       {
        throw $e1;
      }
    catch (final java.lang.Exception $e2)       {
        throw new java.lang.IllegalArgumentException(java.lang.String.format("%s meh.", arg), $e2);
      }
  }
  String filterArg(final String arg) throws Exception {
    throw new Exception();
  }
}