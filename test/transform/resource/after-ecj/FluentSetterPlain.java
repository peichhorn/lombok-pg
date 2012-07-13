class FluentSetterPlain {
  @lombok.Setter int nonfluent;
  @lombok.FluentSetter int fluent;
  @lombok.Setter(value = lombok.AccessLevel.PRIVATE) int nonfluent_accessLevel;
  @lombok.FluentSetter(value = lombok.AccessLevel.PRIVATE) int fluent_accessLevel;
  FluentSetterPlain() {
    super();
  }
  public @java.lang.SuppressWarnings("all") void setNonfluent(final int nonfluent) {
    this.nonfluent = nonfluent;
  }
  public @java.lang.SuppressWarnings("all") FluentSetterPlain fluent(final int fluent) {
    this.fluent = fluent;
    return this;
  }
  private @java.lang.SuppressWarnings("all") void setNonfluent_accessLevel(final int nonfluent_accessLevel) {
    this.nonfluent_accessLevel = nonfluent_accessLevel;
  }
  private @java.lang.SuppressWarnings("all") FluentSetterPlain fluent_accessLevel(final int fluent_accessLevel) {
    this.fluent_accessLevel = fluent_accessLevel;
    return this;
  }
}