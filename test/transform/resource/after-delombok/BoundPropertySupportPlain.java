class BoundPropertySupportPlain {
	private volatile transient java.beans.PropertyChangeSupport $propertyChangeSupport;
	
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
			synchronized (this) {
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
	public void setI(final int i) {
		final int old = this.i;
		this.i = i;
		getPropertyChangeSupport().firePropertyChange(PROP_I, old, this.i);
	}
	
	@java.lang.SuppressWarnings("all")
	public void setS(final String s) {
		final String old = this.s;
		this.s = s;
		getPropertyChangeSupport().firePropertyChange(PROP_S, old, this.s);
	}
	
	@java.lang.SuppressWarnings("all")
	protected void setF(final float f) {
		final float old = this.f;
		this.f = f;
		getPropertyChangeSupport().firePropertyChange(PROP_F, old, this.f);
	}
	
	@java.lang.SuppressWarnings("all")
	void setO(final Object o) {
		final Object old = this.o;
		this.o = o;
		getPropertyChangeSupport().firePropertyChange(PROP_O, old, this.o);
	}
	
	@java.lang.SuppressWarnings("all")
	private void setD(final double d) {
		final double old = this.d;
		this.d = d;
		getPropertyChangeSupport().firePropertyChange(PROP_D, old, this.d);
	}
}