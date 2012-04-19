@lombok.ListenerSupport({java.awt.event.KeyListener.class, java.awt.event.MouseWheelListener.class}) class ListenerSupportPlain1 {
  private final java.util.List<java.awt.event.KeyListener> $registeredKeyListener = new java.util.concurrent.CopyOnWriteArrayList<java.awt.event.KeyListener>();
  private final java.util.List<java.awt.event.MouseWheelListener> $registeredMouseWheelListener = new java.util.concurrent.CopyOnWriteArrayList<java.awt.event.MouseWheelListener>();
  public @java.lang.SuppressWarnings("all") void addKeyListener(final java.awt.event.KeyListener l) {
    if ((! $registeredKeyListener.contains(l)))
        $registeredKeyListener.add(l);
  }
  public @java.lang.SuppressWarnings("all") void removeKeyListener(final java.awt.event.KeyListener l) {
    $registeredKeyListener.remove(l);
  }
  protected @java.lang.SuppressWarnings("all") void fireKeyPressed(final java.awt.event.KeyEvent arg0) {
    for (java.awt.event.KeyListener l : $registeredKeyListener) 
      l.keyPressed(arg0);
  }
  protected @java.lang.SuppressWarnings("all") void fireKeyReleased(final java.awt.event.KeyEvent arg0) {
    for (java.awt.event.KeyListener l : $registeredKeyListener) 
      l.keyReleased(arg0);
  }
  protected @java.lang.SuppressWarnings("all") void fireKeyTyped(final java.awt.event.KeyEvent arg0) {
    for (java.awt.event.KeyListener l : $registeredKeyListener) 
      l.keyTyped(arg0);
  }
  public @java.lang.SuppressWarnings("all") void addMouseWheelListener(final java.awt.event.MouseWheelListener l) {
    if ((! $registeredMouseWheelListener.contains(l)))
        $registeredMouseWheelListener.add(l);
  }
  public @java.lang.SuppressWarnings("all") void removeMouseWheelListener(final java.awt.event.MouseWheelListener l) {
    $registeredMouseWheelListener.remove(l);
  }
  protected @java.lang.SuppressWarnings("all") void fireMouseWheelMoved(final java.awt.event.MouseWheelEvent arg0) {
    for (java.awt.event.MouseWheelListener l : $registeredMouseWheelListener) 
      l.mouseWheelMoved(arg0);
  }
  ListenerSupportPlain1() {
    super();
  }
}
@lombok.ListenerSupport(java.lang.String.class) class ListenerSupportPlain2 {
  ListenerSupportPlain2() {
    super();
  }
}
@lombok.ListenerSupport(javax.swing.event.MouseInputListener.class) class ListenerSupportPlain3 {
  private final java.util.List<javax.swing.event.MouseInputListener> $registeredMouseInputListener = new java.util.concurrent.CopyOnWriteArrayList<javax.swing.event.MouseInputListener>();
  public @java.lang.SuppressWarnings("all") void addMouseInputListener(final javax.swing.event.MouseInputListener l) {
    if ((! $registeredMouseInputListener.contains(l)))
        $registeredMouseInputListener.add(l);
  }
  public @java.lang.SuppressWarnings("all") void removeMouseInputListener(final javax.swing.event.MouseInputListener l) {
    $registeredMouseInputListener.remove(l);
  }
  protected @java.lang.SuppressWarnings("all") void fireMouseClicked(final java.awt.event.MouseEvent arg0) {
    for (javax.swing.event.MouseInputListener l : $registeredMouseInputListener) 
      l.mouseClicked(arg0);
  }
  protected @java.lang.SuppressWarnings("all") void fireMouseEntered(final java.awt.event.MouseEvent arg0) {
    for (javax.swing.event.MouseInputListener l : $registeredMouseInputListener) 
      l.mouseEntered(arg0);
  }
  protected @java.lang.SuppressWarnings("all") void fireMouseExited(final java.awt.event.MouseEvent arg0) {
    for (javax.swing.event.MouseInputListener l : $registeredMouseInputListener) 
      l.mouseExited(arg0);
  }
  protected @java.lang.SuppressWarnings("all") void fireMousePressed(final java.awt.event.MouseEvent arg0) {
    for (javax.swing.event.MouseInputListener l : $registeredMouseInputListener) 
      l.mousePressed(arg0);
  }
  protected @java.lang.SuppressWarnings("all") void fireMouseReleased(final java.awt.event.MouseEvent arg0) {
    for (javax.swing.event.MouseInputListener l : $registeredMouseInputListener) 
      l.mouseReleased(arg0);
  }
  protected @java.lang.SuppressWarnings("all") void fireMouseDragged(final java.awt.event.MouseEvent arg0) {
    for (javax.swing.event.MouseInputListener l : $registeredMouseInputListener) 
      l.mouseDragged(arg0);
  }
  protected @java.lang.SuppressWarnings("all") void fireMouseMoved(final java.awt.event.MouseEvent arg0) {
    for (javax.swing.event.MouseInputListener l : $registeredMouseInputListener) 
      l.mouseMoved(arg0);
  }
  ListenerSupportPlain3() {
    super();
  }
}