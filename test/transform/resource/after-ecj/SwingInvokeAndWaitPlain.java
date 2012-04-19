import javax.swing.*;
class SwingInvokeAndWaitPlain {
  private final JFrame frame = new JFrame();
  SwingInvokeAndWaitPlain() {
    super();
  }
  @lombok.SwingInvokeAndWait @java.lang.SuppressWarnings("all") void test1() throws Exception {
    final java.lang.Runnable $test1Runnable = new java.lang.Runnable() {
      x() {
        super();
      }
      public @java.lang.Override void run() {
        frame.setTitle("test1");
        frame.setVisible(true);
        test2(SwingInvokeAndWaitPlain.this);
        JDialog dialog = new JDialog();
        System.out.println("test1");
      }
    };
    if (java.awt.EventQueue.isDispatchThread())
        {
          $test1Runnable.run();
        }
    else
        {
          try 
            {
              java.awt.EventQueue.invokeAndWait($test1Runnable);
            }
          catch (final java.lang.InterruptedException $ex1)             {
            }
          catch (final java.lang.reflect.InvocationTargetException $ex2)             {
              final java.lang.Throwable $cause = $ex2.getCause();
              if (($cause instanceof Exception))
                  throw (Exception) $cause;
              throw new java.lang.RuntimeException($cause);
            }
        }
  }
  private static void test2(SwingInvokeAndWaitPlain o) {
    System.out.println(o);
  }
}
