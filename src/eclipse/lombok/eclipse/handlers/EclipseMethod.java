package lombok.eclipse.handlers;

import java.util.Arrays;

import static lombok.eclipse.handlers.EclipseNodeBuilder.annotation;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import lombok.eclipse.EclipseNode;

public class EclipseMethod {
	private final EclipseNode methodNode;
	
	private EclipseMethod(final EclipseNode methodNode) {
		if (!(methodNode.get() instanceof AbstractMethodDeclaration)) {
			throw new IllegalArgumentException();
		}
		this.methodNode = methodNode;
	}
	
	public boolean returns(Class<?> clazz) {
		if (isConstructor()) return false;
		MethodDeclaration methodDecl = (MethodDeclaration)get();
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (char[] elem : methodDecl.returnType.getTypeName()) {
			if (first) first = false;
			else sb.append('.');
			sb.append(elem);
		}
		String type = sb.toString();
		return type.endsWith(clazz.getSimpleName());
	}
	
	public boolean isSynchronized() {
		return !isConstructor() && (get().bits & ASTNode.IsSynchronized) != 0;
	}
	
	public boolean isConstructor() {
		return get() instanceof ConstructorDeclaration;
	}
	
	public AbstractMethodDeclaration get() {
		return (AbstractMethodDeclaration)methodNode.get();
	}
	
	public EclipseNode node() {
		return methodNode;
	}
	
	public boolean hasNonFinalParameter() {
		if (get().arguments != null) for (Argument arg : get().arguments) {
			if ((arg.modifiers & ClassFileConstants.AccFinal) == 0) {
				return true;
			}
		}
		return false;
	}
	
	public void body(Statement... statements) {
		get().statements = statements;
		get().annotations = createSuppressWarningsAll(get(), get().annotations);
	}
	
	public Annotation[] createSuppressWarningsAll(ASTNode source, Annotation[] originalAnnotationArray) {
		Annotation ann = annotation(source, "java.lang.SuppressWarnings", "all");
		if (originalAnnotationArray == null) return new Annotation[] { ann };
		Annotation[] newAnnotationArray = Arrays.copyOf(originalAnnotationArray, originalAnnotationArray.length + 1);
		newAnnotationArray[originalAnnotationArray.length] = ann;
		return newAnnotationArray;
	}
	
	public void rebuild() {
		node().rebuild();
	}
	
	public String toString() {
		return get().toString();
	}
	
	public static EclipseMethod methodOf(final EclipseNode node) {
		EclipseNode methodNode = node;
		while ((methodNode != null) && !(methodNode.get() instanceof AbstractMethodDeclaration)) {
			methodNode = methodNode.up();
		}
		return methodNode == null ? null : new EclipseMethod(methodNode);
	}
}
