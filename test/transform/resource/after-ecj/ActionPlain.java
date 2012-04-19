import lombok.Actions.Action1;
import lombok.Action;
import lombok.Validate.NotNull;
class ActionPlain {
  public static @Action @java.lang.SuppressWarnings("all") lombok.Actions.Action0 testAction0() {
    return new lombok.Actions.Action0() {
  x() {
    super();
  }
  public void apply() {
    System.out.println("Action0");
  }
};
  }
  public static @Action @java.lang.SuppressWarnings("all") lombok.Actions.Action1<String> startsWith(final String _prefix) {
    return new lombok.Actions.Action1<String>() {
  x() {
    super();
  }
  public void apply(final String string) {
    string.startsWith(_prefix);
  }
};
  }
  public static @Action @java.lang.SuppressWarnings("all") lombok.Actions.Action1<java.lang.Float> sqrt() {
    return new lombok.Actions.Action1<java.lang.Float>() {
  x() {
    super();
  }
  public void apply(final java.lang.Float f) {
    Math.sqrt(f);
  }
};
  }
  public static @Action @java.lang.SuppressWarnings("all") <T>lombok.Actions.Action2<T, Action1<T>> notNull() {
    return new lombok.Actions.Action2<T, Action1<T>>() {
  x() {
    super();
  }
  public void apply(final T object, final @NotNull Action1<T> notNullAction) {
    if ((notNullAction == null))
        {
          throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "notNullAction", 2));
        }
    if ((object != null))
        notNullAction.apply(object);
  }
};
  }
  public static @Action @java.lang.SuppressWarnings("all") lombok.Actions.Action2<float[], double[]> testArrays() {
    return new lombok.Actions.Action2<float[], double[]>() {
  x() {
    super();
  }
  public void apply(final float[] a, final double[] b) {
    return ;
  }
};
  }
  ActionPlain() {
    super();
  }
}