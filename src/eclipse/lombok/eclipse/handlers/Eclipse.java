package lombok.eclipse.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;

import lombok.eclipse.EclipseNode;

public final class Eclipse {
	private Eclipse() {
	}
	
	public static boolean methodCallIsValid(EclipseNode node, String methodName, Class<?> clazz, String method) {
		Collection<String> importedStatements = node.getImportStatements();
		boolean wasImported = methodName.equals(clazz.getName() + "." + method);
		wasImported |= methodName.equals(clazz.getSimpleName() + "." + method) && importedStatements.contains(clazz.getName());
		wasImported |= methodName.equals(method) && importedStatements.contains(clazz.getName() + "." + method);
		return wasImported;
	}
	
	public static void deleteMethodCallImports(EclipseNode node, String methodName, Class<?> clazz, String method) {
		if (methodName.equals(method)) {
			deleteImport(node, clazz.getName() + "." + method, true);
		} else if (methodName.equals(clazz.getSimpleName() + "." + method)) {
			deleteImport(node, clazz.getName(), false);
		}
	}
	
	public static void deleteImport(EclipseNode node, String name) {
		deleteImport(node, name, false);
	}
	
	public static void deleteImport(EclipseNode node, String name, boolean deleteStatic) {
		CompilationUnitDeclaration unit = (CompilationUnitDeclaration) node.top().get();
		List<ImportReference> newImports = new ArrayList<ImportReference>();
		for (ImportReference imp0rt : unit.imports) {
			boolean delete = ((deleteStatic || !imp0rt.isStatic()) && imp0rt.toString().equals(name));
			if (!delete) newImports.add(imp0rt);
		}
		unit.imports = newImports.toArray(new ImportReference[newImports.size()]);
	}
	
	public static EclipseNode typeNodeOf(final EclipseNode node) {
		EclipseNode typeNode = node;
		while ((typeNode != null) && !(typeNode.get() instanceof TypeDeclaration)) {
			typeNode = typeNode.up();
		}
		if (typeNode == null) {
			throw new IllegalArgumentException();
		}
		return typeNode;
	}
}
