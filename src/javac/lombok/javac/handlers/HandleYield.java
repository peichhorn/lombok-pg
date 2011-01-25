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

import static lombok.javac.handlers.Javac.deleteMethodCallImports;
import static lombok.javac.handlers.Javac.methodCallIsValid;

import lombok.Yield;
import lombok.javac.JavacASTAdapter;
import lombok.javac.JavacASTVisitor;
import lombok.javac.JavacNode;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

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
			if (methodCallIsValid(statementNode, methodName, Yield.class, "yield")) {
				handled = handle(statementNode, methodCall);
			}
		}
	}
	
	@Override public void endVisitCompilationUnit(JavacNode top, JCCompilationUnit unit) {
		if (handled) {
			deleteMethodCallImports(top, methodName, Yield.class, "yield");
		}
	}
	
	public boolean handle(JavacNode methodCallNode, JCMethodInvocation withCall) {
		if (withCall.args.size() < 1) {
			return true;
		}
		
		return true;
	}
}
