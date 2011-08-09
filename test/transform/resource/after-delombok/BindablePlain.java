class BindablePlain {
	
	int i;
	String s;
	float f;
	Object o;
	double d;
	private final java.beans.PropertyChangeSupport propertySupport = new java.beans.PropertyChangeSupport(this);
	
	@java.lang.SuppressWarnings("all")
	public void addPropertyChangeListener(final java.beans.PropertyChangeListener listener) {
		this.propertySupport.addPropertyChangeListener(listener);
	}
	
	@java.lang.SuppressWarnings("all")
	public void removePropertyChangeListener(final java.beans.PropertyChangeListener listener) {
		this.propertySupport.removePropertyChangeListener(listener);
	}
	public static final java.lang.String PROP_I = new java.lang.String("i");
	
	@java.lang.SuppressWarnings("all")
	public void setI(final int i) {
		final int old = this.i;
		this.i = i;
		this.propertySupport.firePropertyChange(PROP_I, old, this.i);
	}
	public static final java.lang.String PROP_S = new java.lang.String("s");
	
	@java.lang.SuppressWarnings("all")
	public void setS(final String s) {
		final String old = this.s;
		this.s = s;
		this.propertySupport.firePropertyChange(PROP_S, old, this.s);
	}
	public static final java.lang.String PROP_F = new java.lang.String("f");
	
	@java.lang.SuppressWarnings("all")
	protected void setF(final float f) {
		final float old = this.f;
		this.f = f;
		this.propertySupport.firePropertyChange(PROP_F, old, this.f);
	}
	public static final java.lang.String PROP_O = new java.lang.String("o");
	
	@java.lang.SuppressWarnings("all")
	void setO(final Object o) {
		final Object old = this.o;
		this.o = o;
		this.propertySupport.firePropertyChange(PROP_O, old, this.o);
	}
	public static final java.lang.String PROP_D = new java.lang.String("d");
	
	@java.lang.SuppressWarnings("all")
	private void setD(final double d) {
		final double old = this.d;
		this.d = d;
		this.propertySupport.firePropertyChange(PROP_D, old, this.d);
	}
}