class FluentSetterGeneric<T, K> {
  @lombok.FluentSetter int fluent;
  FluentSetterGeneric() {
    super();
  }
  public @java.lang.SuppressWarnings("all") FluentSetterGeneric<T, K> fluent(final int fluent) {
    this.fluent = fluent;
    return this;
  }
}
