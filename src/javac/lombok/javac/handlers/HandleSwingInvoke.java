/*
 * Copyright © 2010-2011 Philipp Eichhorn
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

import static lombok.javac.handlers.JavacHandlerUtil.*;
import static lombok.javac.handlers.JavacTreeBuilder.*;
import static com.sun.tools.javac.code.Flags.*;

import java.lang.annotation.Annotation;

import lombok.SwingInvokeAndWait;
import lombok.SwingInvokeLater;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.List;

/**
 * Handles the {@code lombok.SwingInvokeLater} and {@code lombok.SwingInvokeAndWait} annotation for javac.
 */
public class HandleSwingInvoke {
	private final static String METHOD_BODY = "final java.lang.Runnable %s = new java.lang.Runnable(){ " + //
		"@java.lang.Override public void run() %s }; if (java.awt.EventQueue.isDispatchThread()) { %s.run(); } else { %s }";
	private final static String TRY_CATCH_BLOCK = // 
		"try { %s } catch (final java.lang.InterruptedException $ex1) { " + //
		"} catch (final java.lang.reflect.InvocationTargetException $ex2) { " + //
		"if ($ex2.getCause() != null) throw new java.lang.RuntimeException($ex2.getCause()); }";
	private final static String ELSE_STATEMENT = "java.awt.EventQueue.%s(%s);";
	
	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleSwingInvokeLater implements JavacAnnotationHandler<SwingInvokeLater> {
		@Override public boolean handle(AnnotationValues<SwingInvokeLater> annotation, JCAnnotation ast, JavacNode annotationNode) {
			return new HandleSwingInvoke().generateSwingInvoke("invokeLater", SwingInvokeLater.class, annotationNode);
		}

		@Override public boolean isResolutionBased() {
			return false;
		}
	}
	
	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleSwingInvokeAndWait implements JavacAnnotationHandler<SwingInvokeAndWait> {
		@Override public boolean handle(AnnotationValues<SwingInvokeAndWait> annotation, JCAnnotation ast, JavacNode annotationNode) {
			return new HandleSwingInvoke().generateSwingInvoke("invokeAndWait", SwingInvokeAndWait.class, annotationNode);
		}
		
		@Override public boolean isResolutionBased() {
			return false;
		}
	}
	
	public boolean generateSwingInvoke(String methodName, Class<? extends Annotation> annotationType, JavacNode annotationNode) {
		markAnnotationAsProcessed(annotationNode, annotationType);
		JavacNode methodNode = annotationNode.up();
		
		if (methodNode == null || methodNode.getKind() != Kind.METHOD || !(methodNode.get() instanceof JCMethodDecl)) {
			annotationNode.addError("@" + annotationType.getSimpleName() + " is legal only on methods.");
			return true;
		}
		
		JCMethodDecl method = (JCMethodDecl)methodNode.get();
		if (((method.mods.flags & ABSTRACT) != 0) || ((method.body == null))) {
			annotationNode.addError("@" + annotationType.getSimpleName() + " is legal only on concrete methods.");
			return true;
		}
		
		TreeMaker maker = methodNode.getTreeMaker();
		
		repaceWithQualifiedThisReference(maker, methodNode);
		
		String fieldName = "$" + methodNode.getName() + "Runnable";
		
		String elseStatement = String.format(ELSE_STATEMENT, methodName, fieldName);
		if ("invokeAndWait".equals(methodName)) elseStatement = String.format(TRY_CATCH_BLOCK, elseStatement);
		List<JCStatement> methodBody = statements(methodNode, METHOD_BODY, fieldName, method.body, fieldName, elseStatement);
		method.body = maker.Block(0, methodBody);
		
		methodNode.rebuild();
		
		return true;
	}
	
	private static void repaceWithQualifiedThisReference(final TreeMaker maker, final JavacNode node) {
		JavacNode parent = node;
		while (parent != null && !(parent.get() instanceof JCClassDecl)) {
			parent = parent.up();
		}
		if (parent != null) {
			JCClassDecl typeDec = (JCClassDecl)parent.get();
			JCExpression qualThisRef = chainDotsString(maker, node, typeDec.name + ".this");
			
			JCTree tree = node.get();
			tree.accept(new ThisReferenceReplaceVisitor(qualThisRef), null);
		}
	}
}