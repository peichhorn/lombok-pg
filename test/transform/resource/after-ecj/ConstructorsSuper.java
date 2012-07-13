@lombok.RequiredArgsConstructor(callSuper = true) class RequiredArgsConstructor1 extends Exception {
  final int x;
  String name;
  public @java.beans.ConstructorProperties({"x"}) @java.lang.SuppressWarnings("all") RequiredArgsConstructor1(final int x) {
    super();
    this.x = x;
  }
  public @java.beans.ConstructorProperties({"arg0", "x"}) @java.lang.SuppressWarnings("all") RequiredArgsConstructor1(final java.lang.String arg0, final int x) {
    super(arg0);
    this.x = x;
  }
  public @java.beans.ConstructorProperties({"arg0", "x"}) @java.lang.SuppressWarnings("all") RequiredArgsConstructor1(final java.lang.Throwable arg0, final int x) {
    super(arg0);
    this.x = x;
  }
  public @java.beans.ConstructorProperties({"arg0", "arg1", "x"}) @java.lang.SuppressWarnings("all") RequiredArgsConstructor1(final java.lang.String arg0, final java.lang.Throwable arg1, final int x) {
    super(arg0, arg1);
    this.x = x;
  }
}
@lombok.AllArgsConstructor(callSuper = true,staticName = "create") class AllArgsConstructor1 extends Exception {
  final int x;
  String name;
  private @java.lang.SuppressWarnings("all") AllArgsConstructor1(final int x, final String name) {
    super();
    this.x = x;
    this.name = name;
  }
  public static @java.lang.SuppressWarnings("all") AllArgsConstructor1 create(final int x, final String name) {
    return new AllArgsConstructor1(x, name);
  }
  private @java.lang.SuppressWarnings("all") AllArgsConstructor1(final java.lang.String arg0, final int x, final String name) {
    super(arg0);
    this.x = x;
    this.name = name;
  }
  public static @java.lang.SuppressWarnings("all") AllArgsConstructor1 create(final java.lang.String arg0, final int x, final String name) {
    return new AllArgsConstructor1(arg0, x, name);
  }
  private @java.lang.SuppressWarnings("all") AllArgsConstructor1(final java.lang.Throwable arg0, final int x, final String name) {
    super(arg0);
    this.x = x;
    this.name = name;
  }
  public static @java.lang.SuppressWarnings("all") AllArgsConstructor1 create(final java.lang.Throwable arg0, final int x, final String name) {
    return new AllArgsConstructor1(arg0, x, name);
  }
  private @java.lang.SuppressWarnings("all") AllArgsConstructor1(final java.lang.String arg0, final java.lang.Throwable arg1, final int x, final String name) {
    super(arg0, arg1);
    this.x = x;
    this.name = name;
  }
  public static @java.lang.SuppressWarnings("all") AllArgsConstructor1 create(final java.lang.String arg0, final java.lang.Throwable arg1, final int x, final String name) {
    return new AllArgsConstructor1(arg0, arg1, x, name);
  }
}
@lombok.NoArgsConstructor(callSuper = true) class NoArgsConstructor1 extends Exception {
  int x;
  String name;
  public @java.lang.SuppressWarnings("all") NoArgsConstructor1() {
    super();
  }
  public @java.beans.ConstructorProperties({"arg0"}) @java.lang.SuppressWarnings("all") NoArgsConstructor1(final java.lang.String arg0) {
    super(arg0);
  }
  public @java.beans.ConstructorProperties({"arg0"}) @java.lang.SuppressWarnings("all") NoArgsConstructor1(final java.lang.Throwable arg0) {
    super(arg0);
  }
  public @java.beans.ConstructorProperties({"arg0", "arg1"}) @java.lang.SuppressWarnings("all") NoArgsConstructor1(final java.lang.String arg0, final java.lang.Throwable arg1) {
    super(arg0, arg1);
  }
}