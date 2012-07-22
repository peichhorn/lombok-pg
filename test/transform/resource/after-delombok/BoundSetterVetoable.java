class BoundSetterVetoable {
	private volatile transient java.beans.PropertyChangeSupport $propertyChangeSupport;
	private final java.lang.Object[] $propertyChangeSupportLock = new java.lang.Object[0];
	private volatile transient java.beans.VetoableChangeSupport $vetoableChangeSupport;
	private final java.lang.Object[] $vetoableChangeSupportLock = new java.lang.Object[0];
	public static final java.lang.String PROP_NAME = "name";
	public static final java.lang.String PROP_SURNAME = "surname";
	
	private String name;
	private String surname;
	
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
	private java.beans.VetoableChangeSupport getVetoableChangeSupport() {
		if (this.$vetoableChangeSupport == null) {
			synchronized (this.$vetoableChangeSupportLock) {
				if (this.$vetoableChangeSupport == null) {
					this.$vetoableChangeSupport = new java.beans.VetoableChangeSupport(this);
				}
			}
		}
		return this.$vetoableChangeSupport;
	}
	
	@java.lang.SuppressWarnings("all")
	public void addVetoableChangeListener(final java.beans.VetoableChangeListener listener) {
		getVetoableChangeSupport().addVetoableChangeListener(listener);
	}
	
	@java.lang.SuppressWarnings("all")
	public void removeVetoableChangeListener(final java.beans.VetoableChangeListener listener) {
		getVetoableChangeSupport().removeVetoableChangeListener(listener);
	}
	
	@java.lang.SuppressWarnings("all")
	public void fireVetoableChange(final java.lang.String propertyName, final java.lang.Object oldValue, final java.lang.Object newValue) throws java.beans.PropertyVetoException {
		getVetoableChangeSupport().fireVetoableChange(propertyName, oldValue, newValue);
	}
	
	@java.lang.SuppressWarnings("all")
	public void setName(final String name) {
		final String $old = this.name;
		try {
			fireVetoableChange(PROP_NAME, $old, name);
		} catch (final java.beans.PropertyVetoException $e) {
			return;
		}
		this.name = name;
		firePropertyChange(PROP_NAME, $old, name);
	}
	
	@java.lang.SuppressWarnings("all")
	public void setSurname(final String surname) throws java.beans.PropertyVetoException {
		final String $old = this.surname;
		fireVetoableChange(PROP_SURNAME, $old, surname);
		this.surname = surname;
		firePropertyChange(PROP_SURNAME, $old, surname);
	}
}