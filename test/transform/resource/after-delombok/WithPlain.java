import java.awt.Dimension;
import javax.swing.JScrollPane;

class WithPlain {
	public void test1() {
		int i = 42;
		String s;
		java.util.List<String> list1 = new java.util.ArrayList<String>();
		list1.add("Hello");
		list1.add("World");
		java.util.List<String> list2 = java.util.Collections.unmodifiableList(list1);
		list1.add("Hello");
		list1.add("World");
		s = list1.toString();
		list1.add("Hello");
		list1.add("World");
		list2 = list1;
		boolean b = true;
	}
	
	public void test2() {
		final javax.swing.JFrame $with0 = new javax.swing.JFrame();
		$with0.setTitle("Application");
		$with0.setResizable(false);
		resize($with0);
		$with0.setFrameIcon(ICON);
		$with0.setLayout(new java.awt.BorderLayout());
		$with0.add(createButton(), java.awt.BorderLayout.SOUTH);
		frames.add($with0);
		$with0.pack();
		$with0.setVisible(true);
		final JScrollPane $with1 = new JScrollPane(tree);
		$with1.setMinimumSize(new Dimension(200, 200));
		$with1.setPreferredSize($with1.getMinimumSize());
		pane.setLeftComponent($with1);
	}
	
	public void test3() {
		java.util.List<javax.swing.JFrame> frames = new java.util.ArrayList<javax.swing.JFrame>();
		final javax.swing.JFrame $with2 = new javax.swing.JFrame();
		$with2.setTitle("Application");
		$with2.setResizable(false);
		resize($with2, frames);
		$with2.setFrameIcon(ICON);
		$with2.setLayout(new java.awt.BorderLayout());
		$with2.add(createButton(), java.awt.BorderLayout.SOUTH);
		frames.add($with2);
		$with2.pack();
		$with2.setVisible(true);
		javax.swing.JFrame frame = $with2;
	}
	
	public javax.swing.JFrame test4() {
		final javax.swing.JFrame $with3 = new javax.swing.JFrame();
		$with3.setVisible(true);
		return $with3;
	}
}
