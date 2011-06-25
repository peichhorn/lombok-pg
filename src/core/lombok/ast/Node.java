package lombok.ast;

public abstract class Node {
	private Node parent;

	public <T extends Node> T child(T node) {
		if (node != null) node.parent = this;
		return node;
	}

	public Node up() {
		return parent;
	}

	@SuppressWarnings("unchecked")
	public <T extends Node> T upTo(Class<T> type) {
		Node node = this;
		while(!type.isInstance(node)) {
			node = node.up();
		}
		return (T)node;
	}

	public abstract <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p);
}
