import lombok.Function;
import lombok.val;
public class FunctionAndVal {
  private static @Function @java.lang.SuppressWarnings("all") lombok.Functions.Function0<String> needsMoreVal() {
    return new lombok.Functions.Function0<String>() {
  x() {
    super();
  }
  public String apply() {
    final @val java.lang.String part = "String";
    return part;
  }
};
  }
  public FunctionAndVal() {
    super();
  }
}