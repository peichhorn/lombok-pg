import javax.swing.*;
class SwingInvokeLaterPlain {
	private final JFrame frame = new JFrame();

	@lombok.SwingInvokeLater
	void test1() throws Exception {
		frame.setTitle("test1");
		frame.setVisible(true);
		test2(this);
		JDialog dialog = new JDialog(this);
		System.out.println("test1");
	}

	private static void test2(SwingInvokeLaterPlain o) {
		System.out.println(o);
	}
}