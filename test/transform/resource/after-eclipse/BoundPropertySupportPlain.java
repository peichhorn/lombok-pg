import lombok.AccessLevel;
import lombok.BoundPropertySupport;
import lombok.BoundSetter;
@BoundPropertySupport class BoundPropertySupportPlain {
  public static final java.lang.String PROP_I = "i";
  public static final java.lang.String PROP_S = "s";
  public static final java.lang.String PROP_F = "f";
  public static final java.lang.String PROP_O = "o";
  public static final java.lang.String PROP_D = "d";
  private volatile transient java.beans.PropertyChangeSupport $propertyChangeSupport;
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
    getPropertyChangeSupport().firePropertyChange(PROP_I, old, this.i);
  }
  public @java.lang.SuppressWarnings("all") void setS(final String s) {
    final String old = this.s;
    this.s = s;
    getPropertyChangeSupport().firePropertyChange(PROP_S, old, this.s);
  }
  protected @java.lang.SuppressWarnings("all") void setF(final float f) {
    final float old = this.f;
    this.f = f;
    getPropertyChangeSupport().firePropertyChange(PROP_F, old, this.f);
  }
  @java.lang.SuppressWarnings("all") void setO(final Object o) {
    final Object old = this.o;
    this.o = o;
    getPropertyChangeSupport().firePropertyChange(PROP_O, old, this.o);
  }
  private @java.lang.SuppressWarnings("all") void setD(final double d) {
    final double old = this.d;
    this.d = d;
    getPropertyChangeSupport().firePropertyChange(PROP_D, old, this.d);
  }
  private @java.lang.SuppressWarnings("all") java.beans.PropertyChangeSupport getPropertyChangeSupport() {
    if ((this.$propertyChangeSupport == null))
        {
          synchronized (this)
            {
              if ((this.$propertyChangeSupport == null))
                  {
                    this.$propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
                  }
            }
        }
    return this.$propertyChangeSupport;
  }
  public @java.lang.SuppressWarnings("all") void addPropertyChangeListener(final java.beans.PropertyChangeListener listener) {
    getPropertyChangeSupport().addPropertyChangeListener(listener);
  }
  public @java.lang.SuppressWarnings("all") void removePropertyChangeListener(final java.beans.PropertyChangeListener listener) {
    getPropertyChangeSupport().removePropertyChangeListener(listener);
  }
  BoundPropertySupportPlain() {
    super();
  }
}