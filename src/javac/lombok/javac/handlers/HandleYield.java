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

/*
IDEA:
=====

Iterator<T> methodUsingYield(...) {
}

Iterable<T> methodUsingYield(...) {
}

getsTransformedTo:

Iterable<T> methodUsingYield(...) {
	class Yielder implements Iterator<T>, Iterable<T> {
	}
	return new Yielder();
}


Step 1: transform every loop to for(;;)
-------

 while(condIter) => for(;condIter;)
 foreach         => for(Iter iter = iterable.iterator;iter.hashNext;) { currentElem = iter.next(); rest;}

Step3: prepate state-machine of method using yield():
-------

split method after yieldCall, break and continue

Step3: state-machine of method using yield():
-------

 ...
 before1
 yield1
 before2
 ...
 for(initIter;condIter;afterIter) {
   ...
   body1
   yield2
   body2
   ...
 }
 ...
 after1
 yield3
 after2
 ...


private boolean $next() {
	while(true) {
		switch ($stateId) {
		case 0:
			$stateId = 1; 
		case 1: // start of before scope
			// before1
		case 2: 
			// yield1
			$stateId = 3;
			return true;
		case 3:
			// before2
		case 4
			// initIter
		case 5: // start of loop scope
			// if (!condIter) $stateId = 9; continue;
			// body1
		case 6: //
			// yield2
			$stateId = 7;
			return true;
		case 7:
			// body2
		case 8:
			// afterIter
			$stateId = 5;
		case 9: // start of after scope
			// after1
		case 10:
			// yield3
			$stateId = 11;
			return true;
		case 11:
			// after2
		default:
		}
		return false;
	}
}

Step4: minimize states:
-------
 in their separate scope all states can be combined as long as there are states or until they end with a yield state
 if contIter is the true literal, use no if(!condIter)

private boolean $next() {
	while(true) {
		switch ($stateId) {
		case 0:
			$stateId = 1; 
		case 1: 
			// before1
			// yield1
			$stateId = 2;
			return true;
		case 2:
			// before2
			// initIter
		case 3:
			// if (!condIter) $stateId = 9; continue;
			// body1
			// yield2
			$stateId = 4;
			return true;
		case 4:
			// body2
			// afterIter
			$stateId = 3;
		case 5:
			// after1
			// yield3
			$stateId = 6;
			return true;
		case 6:
			// after2
		default:
		}
		return false;
	}
}

yield() means: $stateNext = Ti; $stateId = (next state in scope); return true;
continue means: $stateId = (first state in loop scope); continue;
break means: $stateId = (first state in after scope); continue;

case 0 is obsolete
*/
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
