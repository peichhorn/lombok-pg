import java.io.FileInputStream;
import java.io.FileNotFoundException;

import lombok.DoPrivileged;
import lombok.Sanitize;

class DoPrivilegedPlain {
  private boolean b = true;

  DoPrivilegedPlain() {
    super();
  }

  @lombok.DoPrivileged @java.lang.SuppressWarnings("all") int test1() {
    try 
      {
        return java.security.AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<java.lang.Integer>() {
  public java.lang.Integer run() {
    System.out.println("Test");
    return 0;
  }
});
      }
    catch (final java.security.PrivilegedActionException $ex)       {
        final java.lang.Throwable $cause = $ex.getCause();
        throw new java.lang.RuntimeException($cause);
      }
  }

  @lombok.DoPrivileged @java.lang.SuppressWarnings("all") void test2() {
    try 
      {
        java.security.AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<java.lang.Void>() {
  public java.lang.Void run() {
    if (b)
        {
          return null;
        }
    System.out.println("Test");
    return null;
  }
});
      }
    catch (final java.security.PrivilegedActionException $ex)       {
        final java.lang.Throwable $cause = $ex.getCause();
        throw new java.lang.RuntimeException($cause);
      }
  }
  
  String cleanFilename(String filename) {
    filename = filename.replace("\\", "/").toLowerCase();
    if (filename.startsWith("c:/windows/system32"))
        {
          throw new IllegalArgumentException("Trying to access forbidden file");
        }
    return filename;
  }

  @DoPrivileged @java.lang.SuppressWarnings("all") int test3(final @lombok.Validate.NotEmpty @Sanitize.With("cleanFilename") String filename) throws FileNotFoundException {
    if ((filename == null))
        {
          throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "filename", 1));
        }
    if (filename.isEmpty())
        {
          throw new java.lang.IllegalArgumentException(java.lang.String.format("The validated object \'%s\' (argument #%s) is empty", "filename", 1));
        }
    final String sanitizedFilename = cleanFilename(filename);
    try 
      {
        return java.security.AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<java.lang.Integer>() {
  public java.lang.Integer run() throws FileNotFoundException {
    FileInputStream fis = new FileInputStream(sanitizedFilename);
    int i = fis.read();
    fis.close();
    return i;
  }
});
      }
    catch (final java.security.PrivilegedActionException $ex)       {
        final java.lang.Throwable $cause = $ex.getCause();
        if (($cause instanceof FileNotFoundException))
            throw (FileNotFoundException) $cause;
        throw new java.lang.RuntimeException($cause);
      }
  }

  @DoPrivileged @Sanitize @java.lang.SuppressWarnings("all") int test4(final @Sanitize.With("cleanFilename") String filename) throws FileNotFoundException {
    final String sanitizedFilename = cleanFilename(filename);
    try 
      {
        return java.security.AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<java.lang.Integer>() {
  public java.lang.Integer run() throws FileNotFoundException {
    FileInputStream fis = new FileInputStream(sanitizedFilename);
    int i = fis.read();
    fis.close();
    return i;
  }
});
      }
    catch (final java.security.PrivilegedActionException $ex)       {
        final java.lang.Throwable $cause = $ex.getCause();
        if (($cause instanceof FileNotFoundException))
            throw (FileNotFoundException) $cause;
        throw new java.lang.RuntimeException($cause);
      }
  }
}