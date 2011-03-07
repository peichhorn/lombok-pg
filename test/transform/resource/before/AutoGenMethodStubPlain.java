import lombok.AutoGenMethodStub;

@AutoGenMethodStub
class AutoGenMethodStubPlain1 implements java.awt.event.MouseListener {
	public void mouseExited(java.awt.event.MouseEvent e) {
		System.out.println("defined");
	}
}

@AutoGenMethodStub(throwException=true)
class AutoGenMethodStubPlain2 implements java.awt.event.MouseListener {
	public void mouseExited(java.awt.event.MouseEvent e) {
		System.out.println("defined");
	}
}