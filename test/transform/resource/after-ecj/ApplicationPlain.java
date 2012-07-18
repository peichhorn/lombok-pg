import java.lang.instrument.Instrumentation;
class ApplicationPlain1 implements lombok.Application {
  ApplicationPlain1() {
    super();
  }
  public void runApp(String[] args) throws Throwable {
  }
  public static @java.lang.SuppressWarnings("all") void main(final java.lang.String[] args) throws java.lang.Throwable {
    new ApplicationPlain1().runApp(args);
  }
}
class ApplicationPlain2 implements lombok.Application {
  ApplicationPlain2() {
    super();
  }
  public static @java.lang.SuppressWarnings("all") void main(final java.lang.String[] args) throws java.lang.Throwable {
    new ApplicationPlain2().runApp(args);
  }
}