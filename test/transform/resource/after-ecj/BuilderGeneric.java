import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
@Builder class BuilderGeneric<K extends Comparable<K>, V extends List<K>> {
  public static @java.lang.SuppressWarnings("all") interface FooDef<K extends Comparable<K>, V extends List<K>> {
    public OptionalDef<K, V> foo(final String foo);
    public @java.lang.SuppressWarnings("all") OptionalDef<K, V> foo(final Class<?> clazz);
  }
  public static @java.lang.SuppressWarnings("all") interface OptionalDef<K extends Comparable<K>, V extends List<K>> {
    public OptionalDef<K, V> bar(final K arg0, final V arg1);
    public OptionalDef<K, V> bar(final java.util.Map<? extends K, ? extends V> arg0);
    public BuilderGeneric<K, V> build();
  }
  private static @java.lang.SuppressWarnings("all") class $Builder<K extends Comparable<K>, V extends List<K>> implements FooDef<K, V>, OptionalDef<K, V> {
    private String foo;
    private Map<K, V> bar = $barDefault();
    static <K extends Comparable<K>, V extends List<K>>Map<K, V> $barDefault() {
      return new HashMap<K, V>();
    }
    public OptionalDef<K, V> foo(final String foo) {
      this.foo = foo;
      return this;
    }
    public OptionalDef<K, V> bar(final K arg0, final V arg1) {
      this.bar.put(arg0, arg1);
      return this;
    }
    public OptionalDef<K, V> bar(final java.util.Map<? extends K, ? extends V> arg0) {
      this.bar.putAll(arg0);
      return this;
    }
    public BuilderGeneric<K, V> build() {
      return new BuilderGeneric<K, V>(this);
    }
    private $Builder() {
      super();
    }
    public @java.lang.SuppressWarnings("all") OptionalDef<K, V> foo(final Class<?> clazz) {
      this.foo = clazz.getSimpleName();
      return this;
    }
  }
  private final String foo;
  private final Map<K, V> bar = $Builder.$barDefault();
  private @java.lang.SuppressWarnings("all") BuilderGeneric(final $Builder<K, V> builder) {
    super();
    this.foo = builder.foo;
    this.bar.putAll(builder.bar);
  }
  public static @java.lang.SuppressWarnings("all") <K extends Comparable<K>, V extends List<K>>FooDef<K, V> builderGeneric() {
    return new $Builder<K, V>();
  }
}