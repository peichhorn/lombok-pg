import lombok.Validate;
import lombok.Validate.*;

class ValidateOnConstructor {
  static class CheckedException extends Exception {
    private final String foo;
    
    public @Validate @java.lang.SuppressWarnings("all") CheckedException(@NotEmpty String message, @NotNull Throwable cause) {
      super(message, cause);
      if ((message == null))
          {
            throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "message", 1));
          }
      if (message.isEmpty())
          {
            throw new java.lang.IllegalArgumentException(java.lang.String.format("The validated object \'%s\' (argument #%s) is empty", "message", 1));
          }
      if ((cause == null))
          {
            throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "cause", 2));
          }
      foo = message;
    }
  }
  
  static class CheckedException2 extends Exception {
    public @Validate @java.lang.SuppressWarnings("all") CheckedException2(@NotEmpty String message, @NotNull Throwable cause) {
      super(message, cause);
      if ((message == null))
          {
            throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "message", 1));
          }
      if (message.isEmpty())
          {
            throw new java.lang.IllegalArgumentException(java.lang.String.format("The validated object \'%s\' (argument #%s) is empty", "message", 1));
          }
      if ((cause == null))
          {
            throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "cause", 2));
          }
    }
    
    public @Validate @java.lang.SuppressWarnings("all") CheckedException2(@NotEmpty String message) {
      this(message, null);
      if ((message == null))
          {
            throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "message", 1));
          }
      if (message.isEmpty())
          {
            throw new java.lang.IllegalArgumentException(java.lang.String.format("The validated object \'%s\' (argument #%s) is empty", "message", 1));
          }
    }
  }
  
  private final String s;
  
  public @Validate @java.lang.SuppressWarnings("all") ValidateOnConstructor(final @NotEmpty String s) {
    super();
    if ((s == null))
        {
          throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "s", 1));
        }
    if (s.isEmpty())
        {
          throw new java.lang.IllegalArgumentException(java.lang.String.format("The validated object \'%s\' (argument #%s) is empty", "s", 1));
        }
    this.s = s;
  }
}