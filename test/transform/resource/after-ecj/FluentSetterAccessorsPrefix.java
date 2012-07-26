@lombok.experimental.Accessors(prefix = "_") @lombok.NoArgsConstructor class FluentSetterAccessorsPrefix<T, K> {
  @lombok.FluentSetter int _fluent;
  public @java.lang.SuppressWarnings("all") FluentSetterAccessorsPrefix<T, K> fluent(final int fluent) {
    this._fluent = fluent;
    return this;
  }
  public @java.lang.SuppressWarnings("all") FluentSetterAccessorsPrefix() {
    super();
  }
}