import lombok.Functions.Function1;
import lombok.Function;

class FunctionPlain {
  
  public static @Function @java.lang.SuppressWarnings("all") lombok.Functions.Function1<java.lang.Float, java.lang.Float> sqrt() {
    return new lombok.Functions.Function1<java.lang.Float, java.lang.Float>() {
  public java.lang.Float apply(final java.lang.Float f) {
    return (float) Math.sqrt(f);
  }
};
  }
  
  public static @Function @java.lang.SuppressWarnings("all") <T>lombok.Functions.Function2<T, Function1<T, Void>, java.lang.Void> notNull() {
    return new lombok.Functions.Function2<T, Function1<T, Void>, java.lang.Void>() {
  public java.lang.Void apply(final T object, final Function1<T, Void> notNullFunction) {
    if ((object != null))
        notNullFunction.apply(object);
    return null;
  }
};
  }
  
  FunctionPlain() {
    super();
  }
  
  public void test() {
    Float foo = 1.618F;
    notNull(foo, sqrt());
  }
}