import javax.swing.*;
class SwingInvokeLaterPlain {
	private final JFrame frame = new JFrame();

	@java.lang.SuppressWarnings("all")
	void test1() throws Exception {
		final java.lang.Runnable $test1Runnable = new java.lang.Runnable(){
			@java.lang.Override
			public void run() {
				frame.setTitle("test1");
				frame.setVisible(true);
				test2(SwingInvokeLaterPlain.this);
				JDialog dialog = new JDialog(SwingInvokeLaterPlain.this);
				System.out.println("test1");
			}
		};
		if (java.awt.EventQueue.isDispatchThread()) {
			$test1Runnable.run();
		} else {
			java.awt.EventQueue.invokeLater($test1Runnable);
		}
	}

	private static void test2(SwingInvokeLaterPlain o) {
		System.out.println(o);
	}
}