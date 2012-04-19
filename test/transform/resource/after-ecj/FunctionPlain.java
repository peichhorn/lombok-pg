import lombok.Functions.Function1;
import lombok.Function;
import lombok.Validate.NotNull;
class FunctionPlain {
  public static @Function @java.lang.SuppressWarnings("all") lombok.Functions.Function1<String, java.lang.Boolean> startsWith(final @NotNull String _prefix) {
    return new lombok.Functions.Function1<String, java.lang.Boolean>() {
  x() {
    super();
  }
  public java.lang.Boolean apply(final @NotNull String string) {
    if ((string == null))
        {
          throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "string", 1));
        }
    if ((_prefix == null))
        {
          throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "_prefix", 2));
        }
    return string.startsWith(_prefix);
  }
};
  }
  public static @Function @java.lang.SuppressWarnings("all") lombok.Functions.Function1<java.lang.Float, java.lang.Float> sqrt() {
    return new lombok.Functions.Function1<java.lang.Float, java.lang.Float>() {
  x() {
    super();
  }
  public java.lang.Float apply(final java.lang.Float f) {
    return (float) Math.sqrt(f);
  }
};
  }
  public static @Function @java.lang.SuppressWarnings("all") <T>lombok.Functions.Function2<T, Function1<T, T>, java.lang.Void> notNull() {
    return new lombok.Functions.Function2<T, Function1<T, T>, java.lang.Void>() {
  x() {
    super();
  }
  public java.lang.Void apply(final T object, final Function1<T, T> notNullFunction) {
    if ((object != null))
        notNullFunction.apply(object);
    return null;
  }
};
  }
  public static @Function @java.lang.SuppressWarnings("all") lombok.Functions.Function2<float[], double[], int[]> testArrays() {
    return new lombok.Functions.Function2<float[], double[], int[]>() {
  x() {
    super();
  }
  public int[] apply(final float[] a, final double[] b) {
    return null;
  }
};
  }
  FunctionPlain() {
    super();
  }
}