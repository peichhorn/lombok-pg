class ListenerSupportPlain1 {
	private final java.util.List<java.awt.event.KeyListener> $registeredKeyListener = new java.util.concurrent.CopyOnWriteArrayList<java.awt.event.KeyListener>();
	
	@java.lang.SuppressWarnings("all")
	public void addKeyListener(final java.awt.event.KeyListener l) {
		if (!$registeredKeyListener.contains(l)) {
			$registeredKeyListener.add(l);
		}
	}
	
	@java.lang.SuppressWarnings("all")
	public void removeKeyListener(final java.awt.event.KeyListener l) {
		$registeredKeyListener.remove(l);
	}
	
	@java.lang.SuppressWarnings("all")
	protected void fireKeyTyped(final java.awt.event.KeyEvent arg0) {
		for (java.awt.event.KeyListener l : $registeredKeyListener) {
			l.keyTyped(arg0);
		}
	}

	@java.lang.SuppressWarnings("all")
	protected void fireKeyPressed(final java.awt.event.KeyEvent arg0) {
		for (java.awt.event.KeyListener l : $registeredKeyListener) {
			l.keyPressed(arg0);
		}
	}
	
	@java.lang.SuppressWarnings("all")
	protected void fireKeyReleased(final java.awt.event.KeyEvent arg0) {
		for (java.awt.event.KeyListener l : $registeredKeyListener) {
			l.keyReleased(arg0);
		}
	}
	
	private final java.util.List<java.awt.event.MouseWheelListener> $registeredMouseWheelListener = new java.util.concurrent.CopyOnWriteArrayList<java.awt.event.MouseWheelListener>();
	
	@java.lang.SuppressWarnings("all")
	public void addMouseWheelListener(final java.awt.event.MouseWheelListener l) {
		if (!$registeredMouseWheelListener.contains(l)) {
			$registeredMouseWheelListener.add(l);
		}
	}
	
	@java.lang.SuppressWarnings("all")
	public void removeMouseWheelListener(final java.awt.event.MouseWheelListener l) {
		$registeredMouseWheelListener.remove(l);
	}
	
	@java.lang.SuppressWarnings("all")
	protected void fireMouseWheelMoved(final java.awt.event.MouseWheelEvent arg0) {
		for (java.awt.event.MouseWheelListener l : $registeredMouseWheelListener) {
			l.mouseWheelMoved(arg0);
		}
	}
}
class ListenerSupportPlain2 {
}
class ListenerSupportPlain3 {
	private final java.util.List<javax.swing.event.MouseInputListener> $registeredMouseInputListener = new java.util.concurrent.CopyOnWriteArrayList<javax.swing.event.MouseInputListener>();
	
	@java.lang.SuppressWarnings("all")
	public void addMouseInputListener(final javax.swing.event.MouseInputListener l) {
		if (!$registeredMouseInputListener.contains(l)) {
			$registeredMouseInputListener.add(l);
		}
	}
	
	@java.lang.SuppressWarnings("all")
	public void removeMouseInputListener(final javax.swing.event.MouseInputListener l) {
		$registeredMouseInputListener.remove(l);
	}
	
	@java.lang.SuppressWarnings("all")
	protected void fireMouseClicked(final java.awt.event.MouseEvent arg0) {
		for (javax.swing.event.MouseInputListener l : $registeredMouseInputListener) {
			l.mouseClicked(arg0);
		}
	}
	
	@java.lang.SuppressWarnings("all")
	protected void fireMousePressed(final java.awt.event.MouseEvent arg0) {
		for (javax.swing.event.MouseInputListener l : $registeredMouseInputListener) {
			l.mousePressed(arg0);
		}
	}
	
	@java.lang.SuppressWarnings("all")
	protected void fireMouseReleased(final java.awt.event.MouseEvent arg0) {
		for (javax.swing.event.MouseInputListener l : $registeredMouseInputListener) {
			l.mouseReleased(arg0);
		}
	}
	
	@java.lang.SuppressWarnings("all")
	protected void fireMouseEntered(final java.awt.event.MouseEvent arg0) {
		for (javax.swing.event.MouseInputListener l : $registeredMouseInputListener) {
			l.mouseEntered(arg0);
		}
	}
	
	@java.lang.SuppressWarnings("all")
	protected void fireMouseExited(final java.awt.event.MouseEvent arg0) {
		for (javax.swing.event.MouseInputListener l : $registeredMouseInputListener) {
			l.mouseExited(arg0);
		}
	}
	
	@java.lang.SuppressWarnings("all")
	protected void fireMouseDragged(final java.awt.event.MouseEvent arg0) {
		for (javax.swing.event.MouseInputListener l : $registeredMouseInputListener) {
			l.mouseDragged(arg0);
		}
	}
	
	@java.lang.SuppressWarnings("all")
	protected void fireMouseMoved(final java.awt.event.MouseEvent arg0) {
		for (javax.swing.event.MouseInputListener l : $registeredMouseInputListener) {
			l.mouseMoved(arg0);
		}
	}
}