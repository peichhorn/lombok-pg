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
package lombok.eclipse.handlers;

import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Arrays.*;
import static lombok.core.util.Names.camelCase;
import static lombok.eclipse.handlers.Eclipse.*;
import static org.eclipse.jdt.core.dom.Modifier.*;
import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.handlers.EclipseNodeBuilder.*;

import java.util.Arrays;
import lombok.SwingInvokeAndWait;
import lombok.SwingInvokeLater;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ThisReferenceReplaceVisitor;
import lombok.eclipse.handlers.ThisReferenceReplaceVisitor.IReplacementProvider;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.SwingInvokeLater} and {@code lombok.SwingInvokeAndWait} annotation for eclipse.
 */
public class HandleSwingInvoke {
	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleSwingInvokeLater implements EclipseAnnotationHandler<SwingInvokeLater> {
		@Override public boolean handle(AnnotationValues<SwingInvokeLater> annotation, Annotation ast, EclipseNode annotationNode) {
			return new HandleSwingInvoke().generateSwingInvoke("invokeLater", SwingInvokeLater.class, ast, annotationNode);
		}
	}
	
	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleSwingInvokeAndWait implements EclipseAnnotationHandler<SwingInvokeAndWait> {
		@Override public boolean handle(AnnotationValues<SwingInvokeAndWait> annotation, Annotation ast, EclipseNode annotationNode) {
			return new HandleSwingInvoke().generateSwingInvoke("invokeAndWait", SwingInvokeAndWait.class, ast, annotationNode);
		}
	}
	
	public boolean generateSwingInvoke(String methodName, Class<? extends java.lang.annotation.Annotation> annotationType, ASTNode source, EclipseNode annotationNode) {
		EclipseMethod method = EclipseMethod.methodOf(annotationNode);
		
		if (method == null) {
			annotationNode.addError(canBeUsedOnMethodOnly(annotationType));
			return true;
		}
		
		if (method.isAbstract() || method.isEmpty()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return true;
		}
		
		replaceWithQualifiedThisReference(method.node(), source);
		
		String field = "$" + camelCase(method.name(), "runnable");
		
		MethodDeclaration runMethod = method(method.node(), source, PUBLIC, "void", "run").withAnnotation("java.lang.Override")
			.withStatements(Arrays.asList(method.get().statements)).build();
		
		TypeDeclaration anonymousType = clazz(method.node(), source, 0, "").withBits(ASTNode.IsAnonymousType | ASTNode.IsLocalType).withMethod(runMethod).build();
		
		QualifiedAllocationExpression initialization = new QualifiedAllocationExpression(anonymousType);
		setGeneratedByAndCopyPos(initialization, source);
		initialization.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		initialization.type = typeReference(source, "java.lang.Runnable");
		
		MessageSend elseStatementRun = methodCall(source, "java.awt.EventQueue", methodName, nameReference(source, field));
		
		Block elseStatement;
		if ("invokeAndWait".equals(methodName)) {
			elseStatement = block(source, generateTryCatchBlock(source, elseStatementRun));
		} else {
			elseStatement = block(source, elseStatementRun);
		}
		
		method.body( //
				local(method.node(), source, FINAL, "java.lang.Runnable", field).withInitialization(initialization).build(), //
				ifStatement(source, methodCall(source, "java.awt.EventQueue", "isDispatchThread"), //
						block(source, methodCall(source, field, "run")), 
						elseStatement));

		method.rebuild();

		return true;
	}

	private TryStatement generateTryCatchBlock(ASTNode source, MessageSend elseStatementRun) {
		IfStatement ifStatement = ifStatement(source, notEqual(source, methodCall(source, "$ex2", "getCause"), nullLiteral(source)), //
				throwNewException(source, "java.lang.RuntimeException", methodCall(source, "$ex2", "getCause")));
		
		TryStatement tryStatement = new TryStatement();
		setGeneratedByAndCopyPos(tryStatement, source);
		tryStatement.tryBlock = block(source, elseStatementRun);
		tryStatement.catchArguments = array(
				argument(source, "java.lang.InterruptedException", "$ex1"), 
				argument(source, "java.lang.reflect.InvocationTargetException", "$ex2"));
		tryStatement.catchBlocks = array(block(source), block(source, ifStatement));
		return tryStatement;
	}

	private static void replaceWithQualifiedThisReference(final EclipseNode node, final ASTNode source) {
		EclipseNode parent = typeNodeOf(node);
		final TypeDeclaration typeDec = (TypeDeclaration)parent.get();
		final IReplacementProvider replacement = new HandleSwingInvokeReplacementProvider(new String(typeDec.name), source);
		ASTNode astNode = node.get();
		if (astNode instanceof MethodDeclaration) {
			((MethodDeclaration)astNode).traverse(new ThisReferenceReplaceVisitor(replacement), (ClassScope)null);
		} else {
			astNode.traverse(new ThisReferenceReplaceVisitor(replacement), null);
		}
	}
	
	private static class HandleSwingInvokeReplacementProvider implements IReplacementProvider {
		private final String typeName;
		private final ASTNode source;
		
		public HandleSwingInvokeReplacementProvider(String typeName, ASTNode source) {
			super();
			this.typeName = typeName;
			this.source = source;
		}
		
		@Override public Expression getReplacement() {
			return thisReference(source, typeReference(source, typeName));
		}
	}
}