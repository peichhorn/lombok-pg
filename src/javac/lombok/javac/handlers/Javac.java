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

import static lombok.javac.handlers.JavacHandlerUtil.chainDotsString;

import java.util.*;

import lombok.*;
import lombok.core.AST.Kind;
import lombok.javac.JavacNode;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Javac {
	public static void injectType(final JavacNode typeNode, final JCClassDecl type) {
		JCClassDecl typeDecl = (JCClassDecl) typeNode.get();
		addSuppressWarningsAll(type.mods, typeNode, type.pos);
		typeDecl.defs = typeDecl.defs.append(type);
		typeNode.add(type, Kind.TYPE);
	}

	public static void injectInitializer(final JavacNode typeNode, final JCBlock initializerBlock) {
		JCClassDecl typeDecl = (JCClassDecl) typeNode.get();
		typeDecl.defs = typeDecl.defs.append(initializerBlock);
		typeNode.add(initializerBlock, Kind.INITIALIZER);
	}

	public static void addSuppressWarningsAll(final JCModifiers mods, final JavacNode node, final int pos) {
		TreeMaker maker = node.getTreeMaker();
		JCExpression suppressWarningsType = chainDotsString(node, "java.lang.SuppressWarnings").setPos(pos);
		JCExpression allLiteral = maker.Literal("all").setPos(pos);
		ListBuffer<JCAnnotation> newAnnotations = ListBuffer.lb();
		for (JCAnnotation annotation : mods.annotations) {
			if (!annotation.annotationType.toString().endsWith("SuppressWarnings")) {
				newAnnotations.append(annotation);
			}
		}
		newAnnotations.append((JCAnnotation) maker.Annotation(suppressWarningsType, List.of(allLiteral)).setPos(pos));
		mods.annotations = newAnnotations.toList();
	}

	public static boolean isMethodCallValid(final JavacNode node, final String methodName, final Class<?> clazz, final String method) {
		Collection<String> importedStatements = node.getImportStatements();
		boolean wasImported = methodName.equals(clazz.getName() + "." + method);
		wasImported |= methodName.equals(clazz.getSimpleName() + "." + method) && importedStatements.contains(clazz.getName());
		wasImported |= methodName.equals(method) && importedStatements.contains(clazz.getName() + "." + method);
		return wasImported;
	}

	public static <T> List<T> remove(final List<T> list, final T elementToRemove) {
		ListBuffer<T> newList = ListBuffer.lb();
		for (T element : list) {
			if (elementToRemove != element) newList.append(element);
		}
		return newList.toList();
	}

	/**
	 * Removes the interface from javac's AST (it remains in lombok's AST), then removes any import statement that
	 * imports this exact interface (not star imports).
	 */
	public static void markInterfaceAsProcessed(final JavacNode typeNode, final Class<?> interfazeType) {
		JCClassDecl typeDecl = null;
		if (typeNode.get() instanceof JCClassDecl) typeDecl = (JCClassDecl) typeNode.get();
		if (typeDecl != null) {
			ListBuffer<JCExpression> newImplementing = ListBuffer.lb();
			for (JCExpression exp : typeDecl.implementing) {
				if (!(exp.toString().equals(interfazeType.getName()) || exp.toString().equals(interfazeType.getSimpleName()))) newImplementing.append(exp);
			}
			typeDecl.implementing = newImplementing.toList();
		}
	}

	public static void deleteMethodCallImports(final JavacNode node, final String methodName, final Class<?> clazz, final String method) {
		if (methodName.equals(method)) {
			deleteImport(node, clazz.getName() + "." + method, true);
		} else if (methodName.equals(clazz.getSimpleName() + "." + method)) {
			deleteImport(node, clazz);
		}
	}

	public static void deleteImport(final JavacNode node, final Class<?> clazz) {
		deleteImport(node, clazz.getName());
	}

	public static void deleteImport(final JavacNode node, final String name) {
		deleteImport(node, name, false);
	}

	public static void deleteImport(final JavacNode node, final String name, final boolean deleteStatic) {
		if (!node.shouldDeleteLombokAnnotations()) return;
		String adjustedName = name.replace("$", ".");
		JCCompilationUnit unit = (JCCompilationUnit) node.top().get();
		ListBuffer<JCTree> newDefs = ListBuffer.lb();
		for (JCTree def : unit.defs) {
			boolean delete = false;
			if (def instanceof JCImport) {
				JCImport imp0rt = (JCImport) def;
				delete = ((deleteStatic || !imp0rt.isStatic()) && imp0rt.qualid.toString().equals(adjustedName));
			}
			if (!delete) newDefs.append(def);
		}
		unit.defs = newDefs.toList();
	}

	public static JavacNode methodNodeOf(final JavacNode node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		JavacNode typeNode = node;
		while ((typeNode != null) && !(typeNode.get() instanceof JCMethodDecl)) {
			typeNode = typeNode.up();
		}
		return typeNode;
	}

	public static JavacNode typeNodeOf(final JavacNode node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		JavacNode typeNode = node;
		while ((typeNode != null) && !(typeNode.get() instanceof JCClassDecl)) {
			typeNode = typeNode.up();
		}
		return typeNode;
	}

	public static JCClassDecl typeDeclFiltering(final JavacNode typeNode, final long filterFlags) {
		JCClassDecl typeDecl = null;
		if ((typeNode != null) && (typeNode.get() instanceof JCClassDecl)) typeDecl = (JCClassDecl) typeNode.get();
		if ((typeDecl != null) && ((typeDecl.mods.flags & filterFlags) != 0)) {
			typeDecl = null;
		}
		return typeDecl;
	}

	public static JCAnnotation getAnnotation(final Class<? extends java.lang.annotation.Annotation> expectedType, final JCModifiers mods) {
		return getAnnotation(expectedType.getName(), mods);
	}

	public static JCAnnotation getAnnotation(final String typeName, final JCModifiers mods) {
		for (JCAnnotation ann : mods.annotations) {
			if (matchesType(ann, typeName)) return ann;
		}
		return null;
	}

	public static boolean matchesType(final JCAnnotation ann, final String typeName) {
		return typeName.replace("$", ".").endsWith(ann.annotationType.toString());
	}
}
