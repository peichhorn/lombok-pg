/*
 * Copyright © 2011-2012 Philipp Eichhorn
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

import static lombok.eclipse.handlers.EclipseHandlerUtil.*;

import java.lang.reflect.Method;
import java.util.*;

import lombok.*;
import lombok.core.AST.*;
import lombok.core.util.Each;
import lombok.core.util.Is;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Eclipse {

	public static void setGeneratedByAndCopyPos(final ASTNode target, final ASTNode source, final ASTNode position) {
		setGeneratedBy(target, source);
		copyPosTo(target, position);
	}

	public static void injectType(final EclipseNode typeNode, final TypeDeclaration type) {
		type.annotations = createSuppressWarningsAll(type, type.annotations);
		TypeDeclaration parent = (TypeDeclaration) typeNode.get();

		if (parent.memberTypes == null) {
			parent.memberTypes = new TypeDeclaration[] { type };
		} else {
			TypeDeclaration[] newArray = new TypeDeclaration[parent.memberTypes.length + 1];
			System.arraycopy(parent.memberTypes, 0, newArray, 0, parent.memberTypes.length);
			newArray[parent.memberTypes.length] = type;
			parent.memberTypes = newArray;
		}
		typeNode.add(type, Kind.TYPE);
	}

	public static void injectInitializer(EclipseNode typeNode, Initializer initializerBlock) {
		TypeDeclaration parent = (TypeDeclaration) typeNode.get();
		if (parent.fields == null) {
			parent.fields = new FieldDeclaration[] { initializerBlock };
		} else {
			FieldDeclaration[] newArray = new FieldDeclaration[parent.fields.length + 1];
			System.arraycopy(parent.fields, 0, newArray, 0, parent.fields.length);
			newArray[parent.fields.length] = initializerBlock;
			parent.fields = newArray;
		}
		typeNode.add(initializerBlock, Kind.INITIALIZER);
	}

	public static void copyPosTo(final ASTNode target, final ASTNode source) {
		if (source == null) return;
		if (source instanceof AbstractMethodDeclaration) {
			target.sourceStart = ((AbstractMethodDeclaration) source).bodyStart;
			target.sourceEnd = ((AbstractMethodDeclaration) source).bodyEnd;
		} else if (source instanceof TypeDeclaration) {
			target.sourceStart = ((TypeDeclaration) source).bodyStart;
			target.sourceEnd = ((TypeDeclaration) source).bodyEnd;
		} else {
			target.sourceStart = source.sourceStart;
			target.sourceEnd = source.sourceEnd;
		}
		if (target instanceof AbstractMethodDeclaration) {
			((AbstractMethodDeclaration) target).bodyStart = target.sourceStart;
			((AbstractMethodDeclaration) target).bodyEnd = target.sourceEnd;
			if (source instanceof AbstractMethodDeclaration) {
				((AbstractMethodDeclaration) target).declarationSourceStart = ((AbstractMethodDeclaration) source).declarationSourceStart;
				((AbstractMethodDeclaration) target).declarationSourceEnd = ((AbstractMethodDeclaration) source).declarationSourceEnd;
			} else {
				((AbstractMethodDeclaration) target).declarationSourceStart = target.sourceStart;
				((AbstractMethodDeclaration) target).declarationSourceEnd = target.sourceEnd;
			}
		} else if (target instanceof TypeDeclaration) {
			((TypeDeclaration) target).bodyStart = target.sourceStart;
			((TypeDeclaration) target).bodyEnd = target.sourceEnd;
			if (source instanceof TypeDeclaration) {
				((TypeDeclaration) target).declarationSourceStart = ((TypeDeclaration) source).declarationSourceStart;
				((TypeDeclaration) target).declarationSourceEnd = ((TypeDeclaration) source).declarationSourceEnd;
			} else {
				((TypeDeclaration) target).declarationSourceStart = target.sourceStart;
				((TypeDeclaration) target).declarationSourceEnd = target.sourceEnd;
			}
		} else if (target instanceof Initializer) {
			((Initializer) target).declarationSourceStart = target.sourceStart;
			((Initializer) target).declarationSourceEnd = target.sourceEnd;
		} else if (target instanceof FieldDeclaration) {
			target.sourceStart = 0;
			target.sourceEnd = 0;
			((AbstractVariableDeclaration) target).declarationSourceEnd = -1;
		}
		if (target instanceof Expression) {
			((Expression) target).statementEnd = target.sourceEnd;
		}
		if (target instanceof QualifiedAllocationExpression) {
			if (((QualifiedAllocationExpression) target).anonymousType != null) {
				((QualifiedAllocationExpression) target).anonymousType.bodyEnd = target.sourceEnd + 2;
				((QualifiedAllocationExpression) target).anonymousType.sourceEnd = 0;
				target.sourceStart -= 4;
			}
		}
		if (target instanceof Annotation) {
			((Annotation) target).declarationSourceEnd = target.sourceEnd;
		}
	}

	public static String getMethodName(final MessageSend methodCall) {
		String methodName = (methodCall.receiver instanceof ThisReference) ? "" : methodCall.receiver + ".";
		methodName += new String(methodCall.selector);
		return methodName;
	}

	public static boolean isMethodCallValid(final EclipseNode node, final String methodName, final Class<?> clazz, final String method) {
		Collection<String> importedStatements = node.getImportStatements();
		boolean wasImported = methodName.equals(clazz.getName() + "." + method);
		wasImported |= methodName.equals(clazz.getSimpleName() + "." + method) && importedStatements.contains(clazz.getName());
		wasImported |= methodName.equals(method) && importedStatements.contains(clazz.getName() + "." + method);
		return wasImported;
	}

	public static void deleteMethodCallImports(final EclipseNode node, final String methodName, final Class<?> clazz, final String method) {
		if (methodName.equals(method)) {
			deleteImport(node, clazz.getName() + "." + method, true);
		} else if (methodName.equals(clazz.getSimpleName() + "." + method)) {
			deleteImport(node, clazz.getName(), false);
		}
	}

	public static void deleteImport(final EclipseNode node, final String name) {
		deleteImport(node, name, false);
	}

	public static void deleteImport(final EclipseNode node, final String name, final boolean deleteStatic) {
		CompilationUnitDeclaration unit = (CompilationUnitDeclaration) node.top().get();
		List<ImportReference> newImports = new ArrayList<ImportReference>();
		for (ImportReference imp0rt : Each.elementIn(unit.imports)) {
			boolean delete = ((deleteStatic || !imp0rt.isStatic()) && imp0rt.toString().equals(name));
			if (!delete) newImports.add(imp0rt);
		}
		unit.imports = newImports.toArray(new ImportReference[newImports.size()]);
	}

	public static EclipseNode methodNodeOf(final EclipseNode node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		EclipseNode typeNode = node;
		while ((typeNode != null) && !(typeNode.get() instanceof AbstractMethodDeclaration)) {
			typeNode = typeNode.up();
		}
		return typeNode;
	}

	public static EclipseNode typeNodeOf(final EclipseNode node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}
		EclipseNode typeNode = node;
		while ((typeNode != null) && !(typeNode.get() instanceof TypeDeclaration)) {
			typeNode = typeNode.up();
		}
		return typeNode;
	}

	public static TypeDeclaration typeDeclFiltering(final EclipseNode typeNode, final long filterFlags) {
		TypeDeclaration typeDecl = null;
		if ((typeNode != null) && (typeNode.get() instanceof TypeDeclaration)) typeDecl = (TypeDeclaration) typeNode.get();
		if ((typeDecl != null) && ((typeDecl.modifiers & filterFlags) != 0)) {
			typeDecl = null;
		}
		return typeDecl;
	}

	public static boolean hasAnnotations(final TypeDeclaration decl) {
		return (decl != null) && Is.notEmpty(decl.annotations);
	}

	public static Annotation getAnnotation(final Class<? extends java.lang.annotation.Annotation> expectedType, final Annotation[] annotations) {
		return getAnnotation(expectedType.getName(), annotations);
	}

	public static Annotation getAnnotation(final String typeName, final Annotation[] annotations) {
		for (Annotation ann : Each.elementIn(annotations)) {
			if (matchesType(ann, typeName)) return ann;
		}
		return null;
	}

	public static boolean matchesType(final Annotation ann, final String typeName) {
		return typeName.replace("$", ".").endsWith(ann.type.toString());
	}

	public static void ensureAllClassScopeMethodWereBuild(final TypeBinding binding) {
		if (binding instanceof SourceTypeBinding) {
			ClassScope cs = ((SourceTypeBinding) binding).scope;
			if (cs != null) {
				try {
					Reflection.classScopeBuildFieldsAndMethodsMethod.invoke(cs);
				} catch (final Exception e) {
					// See 'Reflection' class for why we ignore this exception.
				}
			}
		}
	}

	private static final class Reflection {
		public static final Method classScopeBuildFieldsAndMethodsMethod;

		static {
			Method m = null;
			try {
				m = ClassScope.class.getDeclaredMethod("buildFieldsAndMethods");
				m.setAccessible(true);
			} catch (final Exception e) {
				// That's problematic, but as long as no local classes are used we don't actually need it.
				// Better fail on local classes than crash altogether.
			}

			classScopeBuildFieldsAndMethodsMethod = m;
		}
	}
}
