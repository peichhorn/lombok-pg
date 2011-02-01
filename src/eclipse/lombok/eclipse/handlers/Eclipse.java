/*
 * Copyright © 2011 Philipp Eichhorn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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

	public static boolean isMethodCallValid(EclipseNode node, String methodName, Class<?> clazz, String method) {
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

	public static TypeDeclaration typeDeclFiltering(EclipseNode typeNode, long filterFlags) {
		TypeDeclaration typeDecl = null;
		if ((typeNode != null) && (typeNode.get() instanceof TypeDeclaration)) typeDecl = (TypeDeclaration)typeNode.get();
		if ((typeDecl != null) && ((typeDecl.modifiers & filterFlags) != 0)) {
			typeDecl = null;
		}
		return typeDecl;
	}
}
