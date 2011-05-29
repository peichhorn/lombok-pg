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
package lombok.javac.handlers;

import java.lang.annotation.Annotation;
import java.util.Collection;

import lombok.javac.JavacNode;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

public final class Javac {
	private Javac() {
	}

	public static boolean isMethodCallValid(JavacNode node, String methodName, Class<?> clazz, String method) {
		Collection<String> importedStatements = node.getImportStatements();
		boolean wasImported = methodName.equals(clazz.getName() + "." + method);
		wasImported |= methodName.equals(clazz.getSimpleName() + "." + method) && importedStatements.contains(clazz.getName());
		wasImported |= methodName.equals(method) && importedStatements.contains(clazz.getName() + "." + method);
		return wasImported;
	}
	
	public static <T> List<T> remove(List<T> list, T elementToRemove) {
		ListBuffer<T> newList = ListBuffer.lb();
		for (T element : list) {
			if (elementToRemove != element) newList.append(element);
		}
		return newList.toList();
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

	public static JCClassDecl typeDeclFiltering(JavacNode typeNode, long filterFlags) {
		JCClassDecl typeDecl = null;
		if ((typeNode != null) && (typeNode.get() instanceof JCClassDecl)) typeDecl = (JCClassDecl)typeNode.get();
		if ((typeDecl != null) && ((typeDecl.mods.flags & filterFlags) != 0)) {
			typeDecl = null;
		}
		return typeDecl;
	}

	public static JCAnnotation getAnnotation(Class<? extends Annotation> expectedType, JCVariableDecl decl) {
		for (JCAnnotation ann : decl.mods.annotations) {
			if (matchesType(ann, expectedType)) {
				return ann;
			}
		}
		return null;
	}

	private static boolean matchesType(JCAnnotation ann, Class<?> expectedType) {
		return expectedType.getName().replace("$", ".").endsWith(ann.type.toString());
	}
}
