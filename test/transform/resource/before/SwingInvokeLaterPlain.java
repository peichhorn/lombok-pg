import javax.swing.*;
class SwingInvokeLaterPlain {
	private final JFrame frame = new JFrame();

	@lombok.SwingInvokeLater
	void test1() throws Exception {
		frame.setTitle("test1");
		frame.setVisible(true);
		test3(this);
		JDialog dialog = new JDialog(this);
		System.out.println("test1");
	}

	@lombok.SwingInvokeLater
	void test2(final @lombok.Validate.NotNull @lombok.Sanitize.Normalize String title) throws Exception {
		frame.setTitle(title);
		frame.setVisible(true);
	}

	private static void test3(SwingInvokeLaterPlain o) {
		System.out.println(o);
	}
}