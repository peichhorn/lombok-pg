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
				test3(SwingInvokeLaterPlain.this);
				JDialog dialog = new JDialog();
				System.out.println("test1");
			}
		};
		if (java.awt.EventQueue.isDispatchThread()) {
			$test1Runnable.run();
		} else {
			java.awt.EventQueue.invokeLater($test1Runnable);
		}
	}

	@java.lang.SuppressWarnings("all")
	void test2(final String title) throws Exception {
		if (title == null) {
			throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "title", 1));
		}
		final String sanitizedTitle = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFKC);
		final java.lang.Runnable $test2Runnable = new java.lang.Runnable(){
			@java.lang.Override
			public void run() {
				frame.setTitle(sanitizedTitle);
				frame.setVisible(true);
			}
		};
		if (java.awt.EventQueue.isDispatchThread()) {
			$test2Runnable.run();
		} else {
			java.awt.EventQueue.invokeLater($test2Runnable);
		}
	}

	private static void test3(SwingInvokeLaterPlain o) {
		System.out.println(o);
	}
}