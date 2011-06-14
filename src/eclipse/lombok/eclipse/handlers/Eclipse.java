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

import static lombok.core.util.Arrays.isNotEmpty;
import static lombok.eclipse.Eclipse.setGeneratedBy;
import static lombok.eclipse.handlers.EclipseHandlerUtil.createSuppressWarningsAll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.core.AST.Kind;
import lombok.eclipse.EclipseNode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Eclipse {

	public static void setGeneratedByAndCopyPos(ASTNode target, ASTNode source) {
		setGeneratedBy(target, source);
		copyPosTo(target, source);
	}

	public static void injectType(EclipseNode typeNode, TypeDeclaration type) {
		type.annotations = createSuppressWarningsAll(type, type.annotations);
		TypeDeclaration parent = (TypeDeclaration) typeNode.get();

		if (parent.memberTypes == null) {
			parent.memberTypes = new TypeDeclaration[]{ type };
		} else {
			TypeDeclaration[] newArray = new TypeDeclaration[parent.memberTypes.length + 1];
			System.arraycopy(parent.memberTypes, 0, newArray, 0, parent.memberTypes.length);
			newArray[parent.memberTypes.length] = type;
			parent.memberTypes = newArray;
		}
		typeNode.add(type, Kind.TYPE);
	}

	public static void copyPosTo(ASTNode target, ASTNode source) {
		target.sourceStart = source.sourceStart;
		target.sourceEnd = source.sourceEnd;
		if (target instanceof AbstractMethodDeclaration) {
			((AbstractMethodDeclaration)target).bodyStart = source.sourceStart;
			((AbstractMethodDeclaration)target).bodyEnd = source.sourceEnd;
		} else if (target instanceof TypeDeclaration) {
			((TypeDeclaration)target).bodyStart = source.sourceStart;
			((TypeDeclaration)target).bodyEnd = source.sourceEnd;
		} else if (target instanceof AbstractVariableDeclaration) {
			target.sourceStart = target.sourceEnd = 0;
			((AbstractVariableDeclaration)target).declarationSourceEnd  = -1;
		}
		if (target instanceof Expression) {
			((Expression)target).statementEnd = source.sourceEnd;
		}
		if (target instanceof Annotation) {
			((Annotation)target).declarationSourceEnd = source.sourceEnd;
		}
	}

	public static String getMethodName(MessageSend methodCall) {
		String methodName = (methodCall.receiver instanceof ThisReference) ? "" : methodCall.receiver + ".";
		methodName += new String(methodCall.selector);
		return methodName;
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

	public static boolean hasAnnotations(TypeDeclaration decl) {
		return (decl != null) && isNotEmpty(decl.annotations);
	}

	public static boolean hasAnnotations(AbstractVariableDeclaration decl) {
		return (decl != null) && isNotEmpty(decl.annotations);
	}

	public static Annotation getAnnotation(Class<? extends java.lang.annotation.Annotation> expectedType, AbstractVariableDeclaration decl) {
		if (hasAnnotations(decl)) for (Annotation ann : decl.annotations) {
			if (matchesType(ann, expectedType)) {
				return ann;
			}
		}
		return null;
	}

	private static boolean matchesType(Annotation ann, Class<?> expectedType) {
		return expectedType.getName().replace("$", ".").endsWith(ann.type.toString());
	}
}
