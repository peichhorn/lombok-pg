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

import static lombok.core.util.ErrorMessages.*;
import static lombok.javac.handlers.Javac.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static com.sun.tools.javac.code.Flags.*;

import java.util.HashSet;
import java.util.Set;

import org.mangosdk.spi.ProviderFor;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

import lombok.With;
import lombok.javac.JavacASTAdapter;
import lombok.javac.JavacASTVisitor;
import lombok.javac.JavacNode;

@ProviderFor(JavacASTVisitor.class)
public class HandleWith extends JavacASTAdapter {
	private final Set<String> methodNames = new HashSet<String>();
	private int withVarCounter;

	@Override public void visitCompilationUnit(JavacNode top, JCCompilationUnit unit) {
		methodNames.clear();
		withVarCounter = 0;
	}

	@Override public void visitStatement(JavacNode statementNode, JCTree statement) {
		if (statement instanceof JCMethodInvocation) {
			JCMethodInvocation methodCall = (JCMethodInvocation) statement;
			String methodName = methodCall.meth.toString();
			if (isMethodCallValid(statementNode, methodName, With.class, "with")) {
				final JavacMethod method = JavacMethod.methodOf(statementNode);
				if (method == null) {
					statementNode.addError(canBeUsedInBodyOfMethodsOnly("with"));
				} else if (handle(statementNode, methodCall)) {
					methodNames.add(methodName);
				}
			}
		}
	}

	@Override public void endVisitCompilationUnit(JavacNode top, JCCompilationUnit unit) {
		for (String methodName : methodNames) {
			deleteMethodCallImports(top, methodName, With.class, "with");
		}
	}

	public boolean handle(JavacNode methodCallNode, JCMethodInvocation withCall) {
		if (withCall.args.size() < 2) {
			return true;
		}

		TreeMaker maker = methodCallNode.getTreeMaker();
		ListBuffer<JCStatement> withCallStatements = ListBuffer.lb();
		JCExpression withExpr = withCall.args.head;
		String withExprName;
		if ((withExpr instanceof JCFieldAccess) || (withExpr instanceof JCIdent)) {
			withExprName = withExpr.toString();
		} else if (withExpr instanceof JCNewClass) {
			withExprName = "$with" + (withVarCounter++);
			withCallStatements.append(maker.VarDef(maker.Modifiers(FINAL), methodCallNode.toName(withExprName), ((JCNewClass)withExpr).clazz, withExpr));
			withExpr = chainDots(maker, methodCallNode, withExprName);
		} else {
			methodCallNode.addError(firstArgumentCanBeVariableNameOrNewClassStatementOnly("with"));
			return false;
		}
		final JavacNode parent = methodCallNode.directUp();
		JCTree statementThatUsesWith = parent.get();

		boolean wasNoMethodCall;
		try {
			wasNoMethodCall = tryToRemoveWithCall(methodCallNode, withCall, withExpr, statementThatUsesWith);
		} catch (IllegalArgumentException e) {
			methodCallNode.addError(isNotAllowedHere("with"));
			return false;
		}

		if (tryToTransformAllStatements(methodCallNode, withCall.args.tail, withExprName, withCallStatements)) {
			return false;
		}

		tryToInjectStatements(parent, statementThatUsesWith, wasNoMethodCall, withCallStatements.toList());
		return true;
	}

	private boolean tryToRemoveWithCall(JavacNode methodCallNode, JCMethodInvocation withCall, JCExpression withExpr, JCTree statementThatUsesWith) throws IllegalArgumentException {
		if ((statementThatUsesWith instanceof JCAssign) && ((JCAssign)statementThatUsesWith).rhs == withCall) {
			((JCAssign)statementThatUsesWith).rhs = withExpr;
		} else if (statementThatUsesWith instanceof JCFieldAccess) {
			((JCFieldAccess)statementThatUsesWith).selected = withExpr;
		} else if (statementThatUsesWith instanceof JCExpressionStatement) {
			((JCExpressionStatement)statementThatUsesWith).expr = withExpr;
			return false;
		} else if (statementThatUsesWith instanceof JCVariableDecl) {
			((JCVariableDecl)statementThatUsesWith).init = withExpr;
		} else if (statementThatUsesWith instanceof JCReturn) {
			((JCReturn)statementThatUsesWith).expr = withExpr;
		} else if (statementThatUsesWith instanceof JCMethodInvocation) {
			JCMethodInvocation methodCall = (JCMethodInvocation)statementThatUsesWith;
			ListBuffer<JCExpression> newArgs = ListBuffer.lb();
			for (JCExpression arg : methodCall.args) {
				if (arg == withCall) newArgs.append(withExpr);
				else newArgs.append(arg);
			}
			methodCall.args = newArgs.toList();
		} else {
			throw new IllegalArgumentException();
		}
		return true;
	}

	private boolean tryToTransformAllStatements(JavacNode node, List<JCExpression> args, String withExprName, ListBuffer<JCStatement> withCallStatements) {
		TreeMaker maker = node.getTreeMaker();
		for (JCExpression arg : args) {
			if (arg instanceof JCMethodInvocation) {
				arg = (JCExpression)arg.accept(new WithReferenceReplaceVisitor(node, withExprName), null);
				withCallStatements.append(maker.Exec(arg));
			} else {
				node.addError(unsupportedExpressionIn("with", arg));
				return true;
			}
		}
		return false;
	}

	private void tryToInjectStatements(JavacNode parent, JCTree statementThatUsesWith, boolean wasNoMethodCall, List<JCStatement> withCallStatements) {
		while (!(statementThatUsesWith instanceof JCStatement)) {
			parent = parent.directUp();
			statementThatUsesWith = parent.get();
		}
		JCStatement statement = (JCStatement) statementThatUsesWith;
		JavacNode grandParent = parent.directUp();
		JCTree block = grandParent.get();
		if (block instanceof JCBlock) {
			((JCBlock)block).stats = injectStatements(((JCBlock)block).stats, statement, wasNoMethodCall, withCallStatements);
		} else if (block instanceof JCCase) {
			((JCCase)block).stats = injectStatements(((JCCase)block).stats, statement, wasNoMethodCall, withCallStatements);
		} else if (block instanceof JCMethodDecl) {
			((JCMethodDecl)block).body.stats = injectStatements(((JCMethodDecl)block).body.stats, statement, wasNoMethodCall, withCallStatements);
		} else {
			// this would be odd odd but what the hell
			return;
		}
		grandParent.rebuild();
	}

	private List<JCStatement> injectStatements(List<JCStatement> statements, JCStatement statement, boolean wasNoMethodCall, List<JCStatement> withCallStatements) {
		final ListBuffer<JCStatement> newStatements = ListBuffer.lb();
		for (JCStatement stat : statements) {
			if (stat == statement) {
				newStatements.appendList(withCallStatements);
				if (wasNoMethodCall) newStatements.append(stat);
			} else newStatements.append(stat);
		}
		return newStatements.toList();
	}

	public static class WithReferenceReplaceVisitor extends TreeCopier<Void> {
		private final JavacNode node;
		private final TreeMaker maker;
		private final String withExprName;
		private boolean isMethodName;

		public WithReferenceReplaceVisitor(final JavacNode node, final String withExprName) {
			super(node.getTreeMaker());
			this.node = node;
			this.maker = node.getTreeMaker();
			this.withExprName = withExprName;
		}

		@Override
		public JCTree visitNewArray(NewArrayTree node, Void p) {
			JCNewArray tree = (JCNewArray) super.visitNewArray(node, p);
			tree.elems = tryToReplace(tree.elems);
			tree.dims = tryToReplace(tree.dims);
			return tree;
		}

		@Override
		public JCTree visitNewClass(NewClassTree node, Void p) {
			JCNewClass tree = (JCNewClass) super.visitNewClass(node, p);
			tree.encl = tryToReplace(tree.encl);
			tree.args = tryToReplace(tree.args);
			return tree;
		}

		@Override
		public JCTree visitMethodInvocation(MethodInvocationTree node, Void p) {
			JCMethodInvocation tree = (JCMethodInvocation) super.visitMethodInvocation(node, p);
			isMethodName = true;
			tree.meth = tryToReplace(tree.meth);
			isMethodName = false;
			tree.args = tryToReplace(tree.args);
			return tree;
		}

		private List<JCExpression> tryToReplace(List<JCExpression> expressions) {
			ListBuffer<JCExpression> newExpr = ListBuffer.lb();
			for (JCExpression expr : expressions) {
				newExpr.append(tryToReplace(expr));
			}
			return newExpr.toList();
		}

		private JCExpression tryToReplace(JCExpression expr) {
			if (expr instanceof JCIdent) {
				String s = expr.toString();
				if ("_".equals(s)) return chainDotsString(maker, node, withExprName);
				if (!"this".equals(s) && isMethodName) return chainDotsString(maker, node, withExprName + "." + s);
			} else if (expr instanceof JCFieldAccess) {
				String[] s = expr.toString().split("\\.");
				if (s.length == 2) {
					if ("this".equals(s[0])) return chainDotsString(maker, node, s[1]);
					if ("_".equals(s[0])) return chainDotsString(maker, node, withExprName + "." + s[1]);
				}
			}
			return expr;
		}
	}
}