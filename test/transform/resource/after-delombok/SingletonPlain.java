enum SingletonPlain1 {
	INSTANCE;
	
	@java.lang.SuppressWarnings("all")
	public static SingletonPlain1 getInstance() {
		return INSTANCE;
	}
}
enum SingletonPlain2 {
	INSTANCE;
	SingletonPlain2() {
	}
	
	@java.lang.SuppressWarnings("all")
	public static SingletonPlain2 getInstance() {
		return INSTANCE;
	}
}
class SingletonPlain3 {
	public SingletonPlain3(String s) {
	}
}
class SingletonPlain4 extends javax.swing.JButton {
}
class SingletonPlain5 {
	@java.lang.SuppressWarnings("all")
	private static class SingletonPlain5Holder {
		private static final SingletonPlain5 INSTANCE = new SingletonPlain5();
	}
	
	@java.lang.SuppressWarnings("all")
	public static SingletonPlain5 getInstance() {
		return SingletonPlain5Holder.INSTANCE;
	}
}