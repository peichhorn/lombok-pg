class BoundSetterPlain {
	private volatile transient java.beans.PropertyChangeSupport $propertyChangeSupport;
	private final java.lang.Object[] $propertyChangeSupportLock = new java.lang.Object[0];
	public static final java.lang.String PROP_I = "i";
	public static final java.lang.String PROP_S = "s";
	public static final java.lang.String PROP_F = "f";
	public static final java.lang.String PROP_O = "o";
	public static final java.lang.String PROP_D = "d";
	
	int i;
	String s;
	float f;
	Object o;
	double d;
	
	@java.lang.SuppressWarnings("all")
	private java.beans.PropertyChangeSupport getPropertyChangeSupport() {
		if (this.$propertyChangeSupport == null) {
			synchronized (this.$propertyChangeSupportLock) {
				if (this.$propertyChangeSupport == null) {
					this.$propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
				}
			}
		}
		return this.$propertyChangeSupport;
	}
	
	@java.lang.SuppressWarnings("all")
	public void addPropertyChangeListener(final java.beans.PropertyChangeListener listener) {
		getPropertyChangeSupport().addPropertyChangeListener(listener);
	}
	
	@java.lang.SuppressWarnings("all")
	public void removePropertyChangeListener(final java.beans.PropertyChangeListener listener) {
		getPropertyChangeSupport().removePropertyChangeListener(listener);
	}
	
	@java.lang.SuppressWarnings("all")
	public void firePropertyChange(final java.lang.String propertyName, final java.lang.Object oldValue, final java.lang.Object newValue) {
		getPropertyChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
	}
	
	@java.lang.SuppressWarnings("all")
	public void setI(final int i) {
		final int $old = this.i;
		this.i = i;
		firePropertyChange(PROP_I, $old, i);
	}
	
	@java.lang.SuppressWarnings("all")
	public void setS(final String s) {
		final String $old = this.s;
		this.s = s;
		firePropertyChange(PROP_S, $old, s);
	}
	
	@java.lang.SuppressWarnings("all")
	protected void setF(final float f) {
		final float $old = this.f;
		this.f = f;
		firePropertyChange(PROP_F, $old, f);
	}
	
	@java.lang.SuppressWarnings("all")
	void setO(final Object o) {
		final Object $old = this.o;
		this.o = o;
		firePropertyChange(PROP_O, $old, o);
	}
	
	@java.lang.SuppressWarnings("all")
	private void setD(final double d) {
		final double $old = this.d;
		this.d = d;
		firePropertyChange(PROP_D, $old, d);
	}
}