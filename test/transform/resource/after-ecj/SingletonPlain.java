@lombok.Singleton enum SingletonPlain1 {
  INSTANCE(),
  <clinit>() {
  }
  SingletonPlain1() {
    super();
  }
  public static @java.lang.SuppressWarnings("all") SingletonPlain1 getInstance() {
    return INSTANCE;
  }
}
@lombok.Singleton enum SingletonPlain2 {
  INSTANCE(),
  <clinit>() {
  }
  SingletonPlain2() {
    super();
  }
  public static @java.lang.SuppressWarnings("all") SingletonPlain2 getInstance() {
    return INSTANCE;
  }
}
@lombok.Singleton class SingletonPlain3 {
  public SingletonPlain3(String s) {
    super();
  }
}
@lombok.Singleton class SingletonPlain4 extends javax.swing.JButton {
  SingletonPlain4() {
    super();
  }
}
@lombok.Singleton(style = lombok.Singleton.Style.HOLDER) class SingletonPlain5 {
  private static @java.lang.SuppressWarnings("all") class SingletonPlain5Holder {
    private static final SingletonPlain5 INSTANCE = new SingletonPlain5();
    <clinit>() {
    }
  }
  SingletonPlain5() {
    super();
  }
  public static @java.lang.SuppressWarnings("all") SingletonPlain5 getInstance() {
    return SingletonPlain5Holder.INSTANCE;
  }
}