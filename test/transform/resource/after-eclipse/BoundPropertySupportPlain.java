import lombok.AccessLevel;
import lombok.BoundPropertySupport;
import lombok.BoundSetter;
@BoundPropertySupport class BoundPropertySupportPlain {
  public static final java.lang.String PROP_I = new java.lang.String("i");
  public static final java.lang.String PROP_S = new java.lang.String("s");
  public static final java.lang.String PROP_F = new java.lang.String("f");
  public static final java.lang.String PROP_O = new java.lang.String("o");
  public static final java.lang.String PROP_D = new java.lang.String("d");
  private final java.beans.PropertyChangeSupport propertySupport = new java.beans.PropertyChangeSupport(this);
  @BoundSetter int i;
  @BoundSetter(AccessLevel.PUBLIC) String s;
  @BoundSetter(AccessLevel.PROTECTED) float f;
  @BoundSetter(AccessLevel.PACKAGE) Object o;
  @BoundSetter(AccessLevel.PRIVATE) double d;
  <clinit>() {
  }
  public @java.lang.SuppressWarnings("all") void setI(final int i) {
    final int old = this.i;
    this.i = i;
    this.propertySupport.firePropertyChange(PROP_I, old, this.i);
  }
  public @java.lang.SuppressWarnings("all") void setS(final String s) {
    final String old = this.s;
    this.s = s;
    this.propertySupport.firePropertyChange(PROP_S, old, this.s);
  }
  protected @java.lang.SuppressWarnings("all") void setF(final float f) {
    final float old = this.f;
    this.f = f;
    this.propertySupport.firePropertyChange(PROP_F, old, this.f);
  }
  @java.lang.SuppressWarnings("all") void setO(final Object o) {
    final Object old = this.o;
    this.o = o;
    this.propertySupport.firePropertyChange(PROP_O, old, this.o);
  }
  private @java.lang.SuppressWarnings("all") void setD(final double d) {
    final double old = this.d;
    this.d = d;
    this.propertySupport.firePropertyChange(PROP_D, old, this.d);
  }
  public @java.lang.SuppressWarnings("all") void addPropertyChangeListener(final java.beans.PropertyChangeListener listener) {
    this.propertySupport.addPropertyChangeListener(listener);
  }
  public @java.lang.SuppressWarnings("all") void removePropertyChangeListener(final java.beans.PropertyChangeListener listener) {
    this.propertySupport.removePropertyChangeListener(listener);
  }
  BoundPropertySupportPlain() {
    super();
  }
}