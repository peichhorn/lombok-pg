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
package lombok.javac.handlers;

import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.javac.handlers.Javac.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import lombok.*;
import lombok.javac.JavacASTAdapter;
import lombok.javac.JavacASTVisitor;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacASTMaker;
import lombok.javac.handlers.ast.JavacMethod;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import org.mangosdk.spi.ProviderFor;

@ProviderFor(JavacASTVisitor.class)
public class HandleTuple extends JavacASTAdapter {
	private final Set<String> methodNames = new HashSet<String>();
	private int withVarCounter;

	@Override
	public void visitCompilationUnit(final JavacNode top, final JCCompilationUnit unit) {
		methodNames.clear();
		withVarCounter = 0;
	}

	@Override
	public void visitLocal(final JavacNode localNode, final JCVariableDecl local) {
		JCMethodInvocation initTupleCall = getTupelCall(localNode, local.init);
		if (initTupleCall != null) {
			final JavacMethod method = JavacMethod.methodOf(localNode, local);
			if (method == null) {
				localNode.addError(canBeUsedInBodyOfMethodsOnly("tuple"));
			} else if (handle(localNode, initTupleCall)) {
				methodNames.add(initTupleCall.meth.toString());
			}
		}
	}

	@Override
	public void visitStatement(final JavacNode statementNode, final JCTree statement) {
		if (statement instanceof JCAssign) {
			final JCAssign assignment = (JCAssign) statement;
			final JCMethodInvocation leftTupleCall = getTupelCall(statementNode, assignment.lhs);
			final JCMethodInvocation rightTupleCall = getTupelCall(statementNode, assignment.rhs);
			if ((leftTupleCall != null) && (rightTupleCall != null)) {
				final JavacMethod method = JavacMethod.methodOf(statementNode, statement);
				if (method == null) {
					statementNode.addError(canBeUsedInBodyOfMethodsOnly("tuple"));
				} else if (handle(statementNode, leftTupleCall, rightTupleCall)) {
					methodNames.add(leftTupleCall.meth.toString());
					methodNames.add(rightTupleCall.meth.toString());
				}
			}
		}
	}

	private JCMethodInvocation getTupelCall(final JavacNode node, final JCExpression expression) {
		if (expression instanceof JCMethodInvocation) {
			final JCMethodInvocation tupleCall = (JCMethodInvocation) expression;
			final String methodName = tupleCall.meth.toString();
			if (isMethodCallValid(node, methodName, Tuple.class, "tuple")) {
				return tupleCall;
			}
		}
		return null;
	}

	@Override
	public void endVisitCompilationUnit(final JavacNode top, final JCCompilationUnit unit) {
		for (String methodName : methodNames) {
			deleteMethodCallImports(top, methodName, Tuple.class, "tuple");
		}
	}

	public boolean handle(final JavacNode tupleInitNode, final JCMethodInvocation initTupleCall) {
		if (initTupleCall.args.isEmpty()) {
			return true;
		}
		int numberOfArguments = initTupleCall.args.size();
		List<JCVariableDecl> localDecls = List.<JCVariableDecl> nil();
		String type = ((JCVariableDecl) tupleInitNode.get()).vartype.toString();
		for (JavacNode node : tupleInitNode.directUp().down()) {
			if (!(node.get() instanceof JCVariableDecl)) continue;
			JCVariableDecl localDecl = (JCVariableDecl) node.get();
			if (!type.equals(localDecl.vartype.toString())) continue;
			localDecls = localDecls.append(localDecl);
			if (localDecls.size() > numberOfArguments) {
				localDecls.head = localDecls.tail.head;
			}
			if (node.equals(tupleInitNode)) {
				break;
			}
		}
		if (numberOfArguments != localDecls.length()) {
			tupleInitNode.addError(String.format("Argument mismatch on the right side. (required: %s found: %s)", localDecls.length(), numberOfArguments));
			return false;
		}
		int index = 0;
		for (JCVariableDecl localDecl : localDecls) {
			localDecl.init = initTupleCall.args.get(index++);
		}
		return true;
	}

	public boolean handle(final JavacNode tupleAssignNode, final JCMethodInvocation leftTupleCall, final JCMethodInvocation rightTupleCall) {
		if (!validateTupel(tupleAssignNode, leftTupleCall, rightTupleCall)) return false;

		ListBuffer<JCStatement> tempVarAssignments = ListBuffer.lb();
		ListBuffer<JCStatement> assignments = ListBuffer.lb();

		List<String> varnames = collectVarnames(leftTupleCall.args);
		JavacASTMaker builder = new JavacASTMaker(tupleAssignNode, leftTupleCall);
		if (leftTupleCall.args.length() == rightTupleCall.args.length()) {
			Iterator<String> varnameIter = varnames.listIterator();
			final Set<String> blacklistedNames = new HashSet<String>();
			for (JCExpression arg : rightTupleCall.args) {
				String varname = varnameIter.next();
				final Boolean canUseSimpleAssignment = new SimpleAssignmentAnalyser(blacklistedNames).scan(arg, null);
				blacklistedNames.add(varname);
				if ((canUseSimpleAssignment != null) && !canUseSimpleAssignment) {
					final JCExpression vartype = new VarTypeFinder(varname, tupleAssignNode.get()).scan(tupleAssignNode.top().get(), null);
					if (vartype != null) {
						String tempVarname = "$tuple" + withVarCounter++;
						tempVarAssignments.append(builder.build(LocalDecl(Type(vartype), tempVarname).makeFinal().withInitialization(Expr(arg)), JCStatement.class));
						assignments.append(builder.build(Assign(Name(varname), Name(tempVarname)), JCStatement.class));
					} else {
						tupleAssignNode.addError("Lombok-pg Bug. Unable to find vartype.");
						return false;
					}
				} else {
					assignments.append(builder.build(Assign(Name(varname), Expr(arg)), JCStatement.class));
				}
			}
		} else {
			final JCExpression vartype = new VarTypeFinder(varnames.get(0), tupleAssignNode.get()).scan(tupleAssignNode.top().get(), null);
			if (vartype != null) {
				String tempVarname = "$tuple" + withVarCounter++;
				tempVarAssignments.append(builder.build(LocalDecl(Type(vartype).withDimensions(1), tempVarname).makeFinal().withInitialization(Expr(rightTupleCall.args.head)), JCStatement.class));
				int arrayIndex = 0;
				for (String varname : varnames) {
					assignments.append(builder.build(Assign(Name(varname), ArrayRef(Name(tempVarname), Number(arrayIndex++))), JCStatement.class));
				}
			}
		}
		tempVarAssignments.appendList(assignments);
		tryToInjectStatements(tupleAssignNode, tupleAssignNode.get(), tempVarAssignments.toList());

		return true;
	}

	private boolean validateTupel(final JavacNode tupleAssignNode, final JCMethodInvocation leftTupleCall, final JCMethodInvocation rightTupleCall) {
		if ((leftTupleCall.args.length() != rightTupleCall.args.length()) && (rightTupleCall.args.length() != 1)) {
			tupleAssignNode.addError("The left and right hand side of the assignment must have the same amount of arguments or"
					+ " must have one array-type argument for the tuple assignment to work.");
			return false;
		}
		if (!containsOnlyNames(leftTupleCall.args)) {
			tupleAssignNode.addError("Only variable names are allowed as arguments of the left hand side in a tuple assignment.");
			return false;
		}
		return true;
	}

	private void tryToInjectStatements(final JavacNode node, final JCTree nodeThatUsesTupel, final List<JCStatement> statementsToInject) {
		JavacNode parent = node;
		JCTree statementThatUsesTupel = nodeThatUsesTupel;
		while (!(statementThatUsesTupel instanceof JCStatement)) {
			parent = parent.directUp();
			statementThatUsesTupel = parent.get();
		}
		JCStatement statement = (JCStatement) statementThatUsesTupel;
		JavacNode grandParent = parent.directUp();
		JCTree block = grandParent.get();
		if (block instanceof JCBlock) {
			((JCBlock) block).stats = injectStatements(((JCBlock) block).stats, statement, statementsToInject);
		} else if (block instanceof JCCase) {
			((JCCase) block).stats = injectStatements(((JCCase) block).stats, statement, statementsToInject);
		} else if (block instanceof JCMethodDecl) {
			((JCMethodDecl) block).body.stats = injectStatements(((JCMethodDecl) block).body.stats, statement, statementsToInject);
		} else {
			// this would be odd odd but what the hell
			return;
		}
		grandParent.rebuild();
	}

	private List<JCStatement> injectStatements(final List<JCStatement> statements, final JCStatement statement, final List<JCStatement> statementsToInject) {
		final ListBuffer<JCStatement> newStatements = ListBuffer.lb();
		for (JCStatement stat : statements) {
			if (stat == statement) {
				newStatements.appendList(statementsToInject);
			} else newStatements.append(stat);
		}
		return newStatements.toList();
	}

	private List<String> collectVarnames(final List<JCExpression> expressions) {
		ListBuffer<String> varnames = ListBuffer.lb();
		for (JCExpression expression : expressions) {
			varnames.append(expression.toString());
		}
		return varnames.toList();
	}

	private boolean containsOnlyNames(final List<JCExpression> expressions) {
		for (JCExpression expression : expressions) {
			if (!(expression instanceof JCIdent)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Look for the type of a variable in the scope of the given expression.
	 * <p>
	 * {@link VarTypeFinder#scan(com.sun.source.tree.Tree, Void) VarTypeFinder.scan(Tree, Void)} will return the type of
	 * a variable in the scope of the given expression.
	 */
	@RequiredArgsConstructor
	private static class VarTypeFinder extends TreeScanner<JCExpression, Void> {
		private final String varname;
		private final JCTree expr;
		private boolean lockVarname;

		@Override
		public JCExpression visitVariable(final VariableTree node, final Void p) {
			if (!lockVarname && varname.equals(node.getName().toString())) {
				return (JCExpression) node.getType();
			}
			return null;
		}

		@Override
		public JCExpression visitAssignment(final AssignmentTree node, final Void p) {
			if ((expr != null) && (expr.equals(node))) {
				lockVarname = true;
			}
			return super.visitAssignment(node, p);
		}

		@Override
		public JCExpression reduce(final JCExpression r1, final JCExpression r2) {
			return (r1 != null) ? r1 : r2;
		}
	}

	/**
	 * Look for variable names that would break a simple assignment after transforming the tuple.
	 * <p>
	 * If {@link SimpleAssignmentAnalyser#scan(com.sun.source.tree.Tree, Void) AssignmentAnalyser.scan(Tree, Void)}
	 * return {@code null} or {@code true} everything is fine, otherwise a temporary assignment is needed.
	 */
	@RequiredArgsConstructor
	private static class SimpleAssignmentAnalyser extends TreeScanner<Boolean, Void> {
		private final Set<String> blacklistedVarnames;

		@Override
		public Boolean visitMemberSelect(final MemberSelectTree node, final Void p) {
			return true;
		}

		@Override
		public Boolean visitIdentifier(final IdentifierTree node, final Void p) {
			return !blacklistedVarnames.contains(node.getName().toString());
		}

		@Override
		public Boolean reduce(final Boolean r1, final Boolean r2) {
			return !r1 && r2;
		}
	}
}
