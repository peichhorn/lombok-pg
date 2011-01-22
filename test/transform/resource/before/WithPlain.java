import static lombok.With.with;

import java.awt.Dimension;

import javax.swing.JScrollPane;
class WithPlain {
	public void test1() {
		int i = 42;
		String s;
		java.util.List<String> list1 = new java.util.ArrayList<String>();
		java.util.List<String> list2 = java.util.Collections.unmodifiableList(lombok.With.with(list1,
			add("Hello"),
			add("World")));
		s = lombok.With.with(list1,
			add("Hello"),
			add("World")).toString();
		list2 = lombok.With.with(list1,
				add("Hello"),
				add("World"));
		boolean b = true;
	}
	
	public void test2() {
		with(new javax.swing.JFrame(),
			setTitle("Application"),
			setResizable(false),
			this.resize(_),
			setFrameIcon(this.ICON),
			setLayout(new java.awt.BorderLayout()),
			add(this.createButton(), java.awt.BorderLayout.SOUTH),
			frames.add(_),
			pack(),
			setVisible(true)
		);
		with(new JScrollPane(tree),
			setMinimumSize(new Dimension(200, 200)),
			setPreferredSize(getMinimumSize()),
			pane.setLeftComponent(_)
		);
	}
	
	public void test3() {
		java.util.List<javax.swing.JFrame> frames = new java.util.ArrayList<javax.swing.JFrame>();
		javax.swing.JFrame frame = with(new javax.swing.JFrame(),
			setTitle("Application"),
			setResizable(false),
			this.resize(_, frames),
			setFrameIcon(this.ICON),
			setLayout(new java.awt.BorderLayout()),
			add(this.createButton(), java.awt.BorderLayout.SOUTH),
			frames.add(_),
			pack(),
			setVisible(true)
		);
	}
	
	public javax.swing.JFrame test4() {
		return with(new javax.swing.JFrame(),
			setVisible(true));
	}
}
