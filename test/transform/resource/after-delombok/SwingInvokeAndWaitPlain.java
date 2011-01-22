import javax.swing.*;
class SwingInvokeAndWaitPlain {
	private final JFrame frame = new JFrame();

	void test1() throws Exception {
		final java.lang.Runnable $test1Runnable = new java.lang.Runnable(){
			@java.lang.Override
			public void run() {
				frame.setTitle("test1");
				frame.setVisible(true);
				test2(SwingInvokeAndWaitPlain.this);
				JDialog dialog = new JDialog(SwingInvokeAndWaitPlain.this);
				System.out.println("test1");
			}
		};
		if (java.awt.EventQueue.isDispatchThread()) {
			$test1Runnable.run();
		} else {
			try {
				java.awt.EventQueue.invokeAndWait($test1Runnable);
			} catch (final java.lang.InterruptedException $ex1) {
			} catch (final java.lang.reflect.InvocationTargetException $ex2) {
				if ($ex2.getCause() != null) throw new java.lang.RuntimeException($ex2.getCause());
			}
		}
	}

	private static void test2(SwingInvokeAndWaitPlain o) {
		System.out.println(o);
	}
}