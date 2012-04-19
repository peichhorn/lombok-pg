import javax.swing.*;
class SwingInvokeAndWaitPlain {
	private final JFrame frame = new JFrame();

	@lombok.SwingInvokeAndWait
	void test1() throws Exception {
		frame.setTitle("test1");
		frame.setVisible(true);
		test2(this);
		JDialog dialog = new JDialog();
		System.out.println("test1");
	}

	private static void test2(SwingInvokeAndWaitPlain o) {
		System.out.println(o);
	}
}