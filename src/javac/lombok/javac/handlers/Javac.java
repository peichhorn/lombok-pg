package lombok.javac.handlers;

import java.util.Collection;

import lombok.javac.JavacNode;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.util.ListBuffer;

public final class Javac {
	private Javac() {
	}
	
	public static boolean methodCallIsValid(JavacNode node, String methodName, Class<?> clazz, String method) {
		Collection<String> importedStatements = node.getImportStatements();
		boolean wasImported = methodName.equals(clazz.getName() + "." + method);
		wasImported |= methodName.equals(clazz.getSimpleName() + "." + method) && importedStatements.contains(clazz.getName());
		wasImported |= methodName.equals(method) && importedStatements.contains(clazz.getName() + "." + method);
		return wasImported;
	}
	
	public static void deleteMethodCallImports(JavacNode node, String methodName, Class<?> clazz, String method) {
		if (methodName.equals(method)) {
			deleteImport(node, clazz.getName() + "." + method, true);
		} else if (methodName.equals(clazz.getSimpleName() + "." + method)) {
			deleteImport(node, clazz.getName(), false);
		}
	}
	
	public static void deleteImport(JavacNode node, String name) {
		deleteImport(node, name, false);
	}
	
	public static void deleteImport(JavacNode node, String name, boolean deleteStatic) {
		if (!node.shouldDeleteLombokAnnotations()) return;
		ListBuffer<JCTree> newDefs = ListBuffer.lb();
		
		JCCompilationUnit unit = (JCCompilationUnit) node.top().get();
		
		for (JCTree def : unit.defs) {
			boolean delete = false;
			if (def instanceof JCImport) {
				JCImport imp0rt = (JCImport)def;
				delete = ((deleteStatic || !imp0rt.isStatic()) && imp0rt.qualid.toString().equals(name));
			}
			if (!delete) newDefs.append(def);
		}
		unit.defs = newDefs.toList();
	}
	
	public static JavacNode typeNodeOf(final JavacNode node) {
		JavacNode typeNode = node;
		while ((typeNode != null) && !(typeNode.get() instanceof JCClassDecl)) {
			typeNode = typeNode.up();
		}
		if (typeNode == null) {
			throw new IllegalArgumentException();
		}
		return typeNode;
	}
	
	public static JavacNode methodNodeOf(final JavacNode node) {
		JavacNode methodNode = node;
		while ((methodNode != null) && !(methodNode.get() instanceof JCMethodDecl)) {
			methodNode = methodNode.up();
		}
		if (methodNode == null) {
			throw new IllegalArgumentException();
		}
		return methodNode;
	}
	
	public static JCClassDecl typeDeclFiltering(JavacNode typeNode, long filterFlags) {
		JCClassDecl typeDecl = null;
		if ((typeNode != null) && (typeNode.get() instanceof JCClassDecl)) typeDecl = (JCClassDecl)typeNode.get();
		if ((typeDecl != null) && ((typeDecl.mods.flags & filterFlags) != 0)) {
			typeDecl = null;	
		}
		return typeDecl;
	}
}
