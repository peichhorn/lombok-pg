package lombok.javac.handlers;

import lombok.javac.JavacNode;

import com.sun.tools.javac.tree.JCTree.JCClassDecl;

public final class Javac {
	private Javac() {
	}
	
	public static JavacNode typeNodeOf(JavacNode node) {
		JavacNode typeNode = node;
		while ((typeNode != null) && !(typeNode.get() instanceof JCClassDecl)) {
			typeNode = typeNode.up();
		}
		if (typeNode == null) {
			throw new IllegalArgumentException();
		}
		return node;
	}
}
