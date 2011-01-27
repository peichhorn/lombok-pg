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

import static com.sun.tools.javac.code.Flags.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.javac.handlers.Javac.*;
import static lombok.javac.handlers.JavacTreeBuilder.statements;

import java.util.Iterator;

import lombok.Yield;
import lombok.javac.JavacASTAdapter;
import lombok.javac.JavacASTVisitor;
import lombok.javac.JavacNode;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;

@ProviderFor(JavacASTVisitor.class)
public class HandleYield extends JavacASTAdapter {
	private boolean handled;
	private String methodName;
	
	@Override public void visitCompilationUnit(JavacNode top, JCCompilationUnit unit) {
		handled = false;
	}
	
	@Override public void visitStatement(JavacNode statementNode, JCTree statement) {
		if (statement instanceof JCMethodInvocation) {
			JCMethodInvocation methodCall = (JCMethodInvocation) statement;
			methodName = methodCall.meth.toString();
			if (isMethodCallValid(statementNode, methodName, Yield.class, "yield")) {
				try {
					JavacNode methodNode = methodNodeOf(statementNode);
					if (isConstructor(methodNode)) {
						methodNode.addError(canBeUsedInBodyOfMethodsOnly("yield"));
					} else {
						handled = handle(methodNode);
					}
				} catch (IllegalArgumentException e) {
					statementNode.addError(canBeUsedInBodyOfMethodsOnly("yield"));
				}
			}
		}
	}
	
	@Override public void endVisitCompilationUnit(JavacNode top, JCCompilationUnit unit) {
		if (handled) {
			deleteMethodCallImports(top, methodName, Yield.class, "yield");
		}
	}
	
	public boolean handle(JavacNode methodNode) {
		final boolean returnsIterable = returns(methodNode, Iterable.class);
		final boolean returnsIterator = returns(methodNode, Iterator.class);
		if (!(returnsIterable || returnsIterator)) {
			methodNode.addError("Method that contain yield() can only return java.util.Iterator or java.lang.Iterable.");
			return true;
		}
		if (isSynchronized(methodNode)) {
			methodNode.addError("Method that contain yield() should not be synchronized");
			return true;
		}
		if (hasNonFinalParameter(methodNode)) {
			methodNode.addError("Parameters should be final.");
			return true;
		}
		final String yielderName = yielderName(methodNode);
		final String elementType = elementType(methodNode);
		final String variables = "";
		final String stateMachine = "";
		
		TreeMaker maker = methodNode.getTreeMaker();
		JCMethodDecl method = (JCMethodDecl)methodNode.get();
		if (returnsIterable) {
			method.body = maker.Block(0, statements(methodNode, yielderForIterable(yielderName, elementType, variables, stateMachine)));
		} else if (returnsIterator) {
			method.body = maker.Block(0, statements(methodNode, yielderForIterator(yielderName, elementType, variables, stateMachine)));
		}
		
		methodNode.rebuild();
		
		return true;
	}
	
	private String yielderName(JavacNode methodNode) {
		String[] parts = methodNode.getName().split("_");
		String[] newParts = new String[parts.length + 1];
		newParts[0] = "yielder";
		System.arraycopy(parts, 0, newParts, 1, parts.length);
		return camelCase("$", newParts);
	}
	
	private String elementType(JavacNode methodNode) {
		JCMethodDecl methodDecl = (JCMethodDecl)methodNode.get();
		JCExpression type = methodDecl.restype;
		if (type instanceof JCTypeApply) {
			return ((JCTypeApply)type).arguments.head.type.toString();
		} else {
			return Object.class.getName();
		}
	}
	
	private boolean returns(JavacNode methodNode, Class<?> clazz) {
		JCMethodDecl methodDecl = (JCMethodDecl)methodNode.get();
		final String type = getTypeStringOf(methodDecl.restype);
		return type.equals(clazz.getName());
	}
	
	private boolean isSynchronized(JavacNode methodNode) {
		JCMethodDecl methodDecl = (JCMethodDecl)methodNode.get();
		return (methodDecl.mods != null) && ((methodDecl.mods.flags & SYNCHRONIZED) != 0);
	}
	
	private boolean hasNonFinalParameter(JavacNode methodNode) {
		JCMethodDecl methodDecl = (JCMethodDecl)methodNode.get();
		for(JCVariableDecl param: methodDecl.params) {
			if ((param.mods == null) || (param.mods.flags & FINAL) == 0) {
				return true;
			}
		}
		return false;
	}
	
	private String getTypeStringOf(JCExpression type) {
		if (type instanceof JCTypeApply) {
			return ((JCTypeApply)type).clazz.type.toString();
		} else {
			return type.type.toString();
		}
	}
	
	private String yielderForIterator(String yielderName, String elementType, String variables, String stateMachine) {
		return String.format(YIELDER_TEMPLATE, yielderName, elementType, "", variables, elementType, "", stateMachine, yielderName);
	}
	
	private String yielderForIterable(String yielderName, String elementType, String variables, String stateMachine) {
		String iterableImport = String.format(ITERABLE_IMPORT, elementType);
		String iteratorMethod = String.format(ITERATOR_METHOD, elementType, yielderName);
		return String.format(YIELDER_TEMPLATE, yielderName, elementType, iterableImport, variables, elementType, iteratorMethod, stateMachine, yielderName);
	}
	 
	String YIELDER_TEMPLATE = // 
			"class %s implements java.util.Iterator<%s> %s { %s private int $state; private boolean $hasNext; private boolean $nextDefined; private %s $next; %s " + //
			"public boolean hasNext() { if (!$nextDefined) { $hasNext = getNext(); $nextDefined = true; } return $hasNext; } " + //
			"public String next() { if (!hasNext()) { throw new java.util.NoSuchElementException(); } $nextDefined = false; return $next; }" + //
			"public void remove() { throw new java.lang.UnsupportedOperationException(); }" + //
			"private boolean getNext() { while(true) { switch ($state) { %s default: } return false; } } } return new %s();";
	String ITERABLE_IMPORT = ", java.lang.Iterable<%s>";
	String ITERATOR_METHOD = "public java.util.Iterator<%s> iterator() { return new %s(); }";
	String CASE = "case %s: ";
	String STATE_RETURN_TRUE = "$next = %s; $state = %s; return true;";
	String STATE_CONTINUE = "$state = %s; continue;";
}
