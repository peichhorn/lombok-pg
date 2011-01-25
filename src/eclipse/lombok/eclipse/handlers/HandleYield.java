/*
 * Copyright ©  2011 Philipp Eichhorn
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

import static lombok.eclipse.handlers.Eclipse.*;
import static lombok.core.util.Arrays.*;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.mangosdk.spi.ProviderFor;

import lombok.Yield;
import lombok.eclipse.EclipseASTAdapter;
import lombok.eclipse.EclipseASTVisitor;
import lombok.eclipse.EclipseNode;

@ProviderFor(EclipseASTVisitor.class)
public class HandleYield extends EclipseASTAdapter {
	private boolean handled;
	private String methodName;
	
	@Override public void visitCompilationUnit(EclipseNode top, CompilationUnitDeclaration unit) {
		handled = false;
	}
	
	@Override public void visitStatement(EclipseNode statementNode, Statement statement) {
		if (statement instanceof MessageSend) {
			MessageSend methodCall = (MessageSend) statement;
			methodName = new String(methodCall.selector);
			if (methodCallIsValid(statementNode, methodName, Yield.class, "yield")) {
				handled = handle(statementNode, methodCall);
			}
		}
	}
	
	@Override public void endVisitCompilationUnit(EclipseNode top, CompilationUnitDeclaration unit) {
		if (handled) {
			deleteMethodCallImports(top, methodName, Yield.class, "yield");
		}
	}
	
	public boolean handle(EclipseNode methodCallNode, MessageSend withCall) {
		if (isEmpty(withCall.arguments) || (withCall.arguments.length < 1)) {
			return true;
		}
		
		return true;
	}
}