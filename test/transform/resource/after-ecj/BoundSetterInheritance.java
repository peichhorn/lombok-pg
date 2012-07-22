import lombok.BoundSetter;
import javax.swing.JFrame;
@BoundSetter class BoundSetterInheritance extends JFrame {
  public static final java.lang.String PROP_PROPERTY1 = "property1";
  public static final java.lang.String PROP_PROPERTY2 = "property2";
  private String property1;
  private String property2;
  <clinit>() {
  }
  BoundSetterInheritance() {
    super();
  }
  public @java.lang.SuppressWarnings("all") void setProperty1(final String property1) {
    final String $old = this.property1;
    this.property1 = property1;
    firePropertyChange(PROP_PROPERTY1, $old, property1);
  }
  public @java.lang.SuppressWarnings("all") void setProperty2(final String property2) {
    final String $old = this.property2;
    this.property2 = property2;
    firePropertyChange(PROP_PROPERTY2, $old, property2);
  }
}