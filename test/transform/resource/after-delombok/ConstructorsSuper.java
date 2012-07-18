class RequiredArgsConstructor1 extends Exception {
	final int x;
	String name;
	
	@java.beans.ConstructorProperties({"x"})
	@java.lang.SuppressWarnings("all")
	public RequiredArgsConstructor1(final int x) {
		this.x = x;
	}
	
	@java.beans.ConstructorProperties({"arg0", "x"})
	@java.lang.SuppressWarnings("all")
	public RequiredArgsConstructor1(final java.lang.String arg0, final int x) {
		super(arg0);
		this.x = x;
	}
	
	@java.beans.ConstructorProperties({"arg0", "arg1", "x"})
	@java.lang.SuppressWarnings("all")
	public RequiredArgsConstructor1(final java.lang.String arg0, final java.lang.Throwable arg1, final int x) {
		super(arg0, arg1);
		this.x = x;
	}
	
	@java.beans.ConstructorProperties({"arg0", "x"})
	@java.lang.SuppressWarnings("all")
	public RequiredArgsConstructor1(final java.lang.Throwable arg0, final int x) {
		super(arg0);
		this.x = x;
	}
}
class AllArgsConstructor1 extends Exception {
	final int x;
	String name;
	
	@java.lang.SuppressWarnings("all")
	private AllArgsConstructor1(final int x, final String name) {
		this.x = x;
		this.name = name;
	}
	
	@java.lang.SuppressWarnings("all")
	public static AllArgsConstructor1 create(final int x, final String name) {
		return new AllArgsConstructor1(x, name);
	}
	
	@java.lang.SuppressWarnings("all")
	private AllArgsConstructor1(final java.lang.String arg0, final int x, final String name) {
		super(arg0);
		this.x = x;
		this.name = name;
	}
	
	@java.lang.SuppressWarnings("all")
	public static AllArgsConstructor1 create(final java.lang.String arg0, final int x, final String name) {
		return new AllArgsConstructor1(arg0, x, name);
	}
	
	@java.lang.SuppressWarnings("all")
	private AllArgsConstructor1(final java.lang.String arg0, final java.lang.Throwable arg1, final int x, final String name) {
		super(arg0, arg1);
		this.x = x;
		this.name = name;
	}
	
	@java.lang.SuppressWarnings("all")
	public static AllArgsConstructor1 create(final java.lang.String arg0, final java.lang.Throwable arg1, final int x, final String name) {
		return new AllArgsConstructor1(arg0, arg1, x, name);
	}
	
	@java.lang.SuppressWarnings("all")
	private AllArgsConstructor1(final java.lang.Throwable arg0, final int x, final String name) {
		super(arg0);
		this.x = x;
		this.name = name;
	}
	
	@java.lang.SuppressWarnings("all")
	public static AllArgsConstructor1 create(final java.lang.Throwable arg0, final int x, final String name) {
		return new AllArgsConstructor1(arg0, x, name);
	}
}
class NoArgsConstructor1 extends Exception {
	int x;
	String name;
	
	@java.lang.SuppressWarnings("all")
	public NoArgsConstructor1() {
	}
	
	@java.beans.ConstructorProperties({"arg0"})
	@java.lang.SuppressWarnings("all")
	public NoArgsConstructor1(final java.lang.String arg0) {
		super(arg0);
	}
	
	@java.beans.ConstructorProperties({"arg0", "arg1"})
	@java.lang.SuppressWarnings("all")
	public NoArgsConstructor1(final java.lang.String arg0, final java.lang.Throwable arg1) {
		super(arg0, arg1);
	}
	
	@java.beans.ConstructorProperties({"arg0"})
	@java.lang.SuppressWarnings("all")
	public NoArgsConstructor1(final java.lang.Throwable arg0) {
		super(arg0);
	}
}