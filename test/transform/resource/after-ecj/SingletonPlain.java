@lombok.Singleton enum SingletonPlain1 {
  INSTANCE(),
  SingletonPlain1() {
    super();
  }
}
@lombok.Singleton enum SingletonPlain2 {
  INSTANCE(),
  SingletonPlain2() {
    super();
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