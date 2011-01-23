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
import lombok.core.AST.Kind;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ThisReferenceReplaceVisitor;
import lombok.eclipse.handlers.ThisReferenceReplaceVisitor.IReplacementProvider;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.EqualExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
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
		EclipseNode methodNode = annotationNode.up();
		
		if (methodNode == null || methodNode.getKind() != Kind.METHOD || !(methodNode.get() instanceof MethodDeclaration)) {
			annotationNode.addError(canBeUsedOnMethodOnly(annotationType));
			return true;
		}
		
		MethodDeclaration method = (MethodDeclaration)methodNode.get();
		
		if (method.isAbstract()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return true;
		}
		
		if (isEmpty(method.statements)) return false;
		
		replaceWithQualifiedThisReference(methodNode, source);
		
		String field = "$" + camelCase(new String(method.selector), "runnable");
		
		MethodDeclaration runMethod = method(methodNode, source, PUBLIC, "void", "run").withAnnotation("java.lang.Override")
			.withStatements(Arrays.asList(method.statements)).build();
		
		TypeDeclaration anonymousType = clazz(methodNode, source, 0, "").withBits(ASTNode.IsAnonymousType | ASTNode.IsLocalType).withMethod(runMethod).build();
		
		QualifiedAllocationExpression initialization = new QualifiedAllocationExpression(anonymousType);
		setGeneratedByAndCopyPos(initialization, source);
		initialization.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		initialization.type = typeReference(source, "java.lang.Runnable");
		
		Block thenStatement = new Block(0);
		setGeneratedByAndCopyPos(thenStatement, source);
		thenStatement.statements = array(methodCall(source, field, "run"));
		
		MessageSend elseStatementRun = methodCall(source, "java.awt.EventQueue", methodName);
		elseStatementRun.arguments = array(nameReference(source, field));
		
		Block elseStatement = new Block(0);
		setGeneratedByAndCopyPos(elseStatement, source);
		if ("invokeAndWait".equals(methodName)) {
			elseStatement.statements = array(generateTryCatchBlock(elseStatementRun, source));
		} else {
			elseStatement.statements = array(elseStatementRun);
		}
		
		Expression condition = methodCall(source, "java.awt.EventQueue", "isDispatchThread");
		
		method.statements = array(local(methodNode, source, FINAL, "java.lang.Runnable", field).withInitialization(initialization).build(),
				new IfStatement(condition, thenStatement, elseStatement, 0, 0));
		setGeneratedByAndCopyPos(method.statements[1], source);

		methodNode.rebuild();

		return true;
	}

	private TryStatement generateTryCatchBlock(MessageSend elseStatementRun, ASTNode source) {
		Argument catchArg1 = argument(source, "java.lang.InterruptedException", "$ex1");
		Argument catchArg2 = argument(source, "java.lang.reflect.InvocationTargetException", "$ex2");
		
		Block block1 = new Block(0);
		setGeneratedByAndCopyPos(block1, source);
		
		AllocationExpression newClassExp = new AllocationExpression();
		setGeneratedByAndCopyPos(newClassExp, source);
		newClassExp.type = typeReference(source, "java.lang.RuntimeException");
		newClassExp.arguments = array(methodCall(source, "$ex2", "getCause"));
		
		Statement rethrowStatement = new ThrowStatement(newClassExp, 0, 0);
		setGeneratedByAndCopyPos(rethrowStatement, source);
		
		NullLiteral nullLiteral = new NullLiteral(0, 0);
		setGeneratedByAndCopyPos(nullLiteral, source);
		
		EqualExpression notNullCondition = new EqualExpression(methodCall(source, "$ex2", "getCause"), nullLiteral, OperatorIds.NOT_EQUAL);
		setGeneratedByAndCopyPos(notNullCondition, source);
		
		IfStatement ifStatement = new IfStatement(notNullCondition, rethrowStatement, 0, 0);
		setGeneratedByAndCopyPos(ifStatement, source);
		
		Block block2 = new Block(0);
		setGeneratedByAndCopyPos(block2, source);
		block2.statements = array(ifStatement);
		
		TryStatement tryStatement = new TryStatement();
		setGeneratedByAndCopyPos(tryStatement, source);
		tryStatement.tryBlock = new Block(0);
		setGeneratedByAndCopyPos(tryStatement.tryBlock, source);
		tryStatement.tryBlock.statements = array(elseStatementRun);
		tryStatement.catchArguments = array(catchArg1, catchArg2);
		tryStatement.catchBlocks = array(block1, block2);
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