import javax.swing.JFrame;

class BoundSetterInheritance extends JFrame {
	public static final java.lang.String PROP_PROPERTY1 = "property1";
	public static final java.lang.String PROP_PROPERTY2 = "property2";
	
	private String property1;
	private String property2;
	
	@java.lang.SuppressWarnings("all")
	public void setProperty1(final String property1) {
		final String $old = this.property1;
		this.property1 = property1;
		firePropertyChange(PROP_PROPERTY1, $old, property1);
	}
	
	@java.lang.SuppressWarnings("all")
	public void setProperty2(final String property2) {
		final String $old = this.property2;
		this.property2 = property2;
		firePropertyChange(PROP_PROPERTY2, $old, property2);
	}
}