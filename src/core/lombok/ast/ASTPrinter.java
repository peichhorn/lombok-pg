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
package lombok.ast;

import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

import lombok.*;

/**
 * For debugging purposes only
 * <p>
 * <b>Note:</b> May not generate valid java.
 */
@RequiredArgsConstructor
public final class ASTPrinter implements ASTVisitor<ASTPrinter.State, ASTPrinter.State> {

	private void writeModifiers(final Set<Modifier> modifiers, final State state) {
		if (modifiers.contains(Modifier.PUBLIC)) state.print("public ");
		if (modifiers.contains(Modifier.PRIVATE)) state.print("private ");
		if (modifiers.contains(Modifier.PROTECTED)) state.print("protected ");
		if (modifiers.contains(Modifier.STATIC)) state.print("static ");
		if (modifiers.contains(Modifier.FINAL)) state.print("final ");
		if (modifiers.contains(Modifier.VOLATILE)) state.print("volatile ");
		if (modifiers.contains(Modifier.TRANSIENT)) state.print("transient ");
	}

	@Override
	public State visitAnnotation(final Annotation node, final State state) {
		state.print("@").print(node.getType(), this);
		if (!node.getValues().isEmpty()) {
			if (node.getValues().containsKey("value") && node.getValues().size() == 1) {
				node.getValues().get("value").accept(this, state);
			} else {
				Set<Entry<String, Expression<?>>> entries = node.getValues().entrySet();
				int i = 0;
				int iend = entries.size() - 1;
				for (Entry<String, Expression<?>> entry : entries) {
					state.print(entry.getKey()).print(" = ").print(entry.getValue(), this);
					if (i == iend) break;
					state.print(", ");
					i++;
				}
			}
		}
		return state;
	}

	@Override
	public State visitArgument(final Argument node, final State state) {
		for (Annotation annotation : node.getAnnotations()) state.print(annotation, this).print(" ");
		writeModifiers(node.getModifiers(), state);
		return state.print(node.getType(), this).print(" ").print(node.getName());
	}

	@Override
	public State visitArrayRef(final ArrayRef node, final State state) {
		return state.print(node.getIndexed(), this).print("[").print(node.getIndex(), this).print("]");
	}

	@Override
	public State visitAssignment(final Assignment node, final State state) {
		return state.print(node.getLeft(), this).print(" = ").print(node.getRight(), this);
	}

	@Override
	public State visitBinary(final Binary node, final State state) {
		return state.print(node.getLeft(), this).print(" ").print(node.getOperator()).print(" ").print(node.getRight(), this);
	}

	@Override
	public State visitBlock(final Block node, final State state) {
		state.print("{\n");
		final State indentedState = state.indent();
		for (Statement<?> statement : node.getStatements()) {
			indentedState.printIndent().print(statement, this).print(";\n");
		}
		return state.printIndent().print("}");
	}

	@Override
	public State visitBooleanLiteral(final BooleanLiteral node, final State state) {
		return state.print(node.isTrue() ? "true" : "false");
	}

	@Override
	public State visitBreak(final Break node, final State state) {
		state.print("break");
		if (node.getLabel() != null) state.print(" ").print(node.getLabel());
		return state;
	}

	@Override
	public State visitCall(final Call node, final State state) {
		if (node.getReceiver() != null) {
			state.print(node.getReceiver(), this).print(".");
		}
		if (!node.getTypeArgs().isEmpty()) {
			state.print("<");
			for (int i = 0, iend = node.getTypeArgs().size() - 1; i <= iend; i++) {
				state.print(node.getTypeArgs().get(i), this);
				if (i == iend) break;
				state.print(", ");
			}
			state.print(">");
		}
		state.print(node.getName()).print("(");
		for (int i = 0, iend = node.getArgs().size() - 1; i <= iend; i++) {
			state.print(node.getArgs().get(i), this);
			if (i == iend) break;
			state.print(", ");
		}
		return state.print(")");
	}

	@Override
	public State visitCast(final Cast node, final State state) {
		return state.print("(").print(node.getType(), this).print(") ").print(node.getExpression(), this);
	}

	@Override
	public State visitCase(final Case node, final State state) {
		state.printIndent();
		if (node.getPattern() == null) {
			state.print("default:\n");
		} else {
			state.print("case ").print(node.getPattern(), this).print(":\n");
		}
		final State indentedState = state.indent();
		for (Statement<?> statement : node.getStatements()) {
			indentedState.printIndent().print(statement, this).print(";\n");
		}
		return state;
	}

	@Override
	public State visitCharLiteral(final CharLiteral node, final State state) {
		return state.print("'").print(node.getCharacter()).print("'");
	}

	@Override
	public State visitClassDecl(final ClassDecl node, final State state) {
		if (!node.isAnonymous()) {
			for (Annotation annotation : node.getAnnotations()) {
				state.print(annotation, this).print(" ");
			}
			writeModifiers(node.getModifiers(), state);
			if (node.isInterface()) {
				state.print("interface ");
			} else {
				state.print("class ");
			}
			state.print(node.getName());
			if (!node.getTypeParameters().isEmpty()) {
				state.print("<");
				for (int i = 0, iend = node.getTypeParameters().size() - 1; i <= iend; i++) {
					state.print(node.getTypeParameters().get(i), this);
					if (i == iend) break;
					state.print(", ");
				}
				state.print(">");
			}
			if (node.getSuperclass() != null) {
				state.print(" extends ").print(node.getSuperclass(), this);
			}
			if (!node.getSuperInterfaces().isEmpty()) {
				state.print(" implements ");
				for (int i = 0, iend = node.getSuperInterfaces().size() - 1; i <= iend; i++) {
					state.print(node.getSuperInterfaces().get(i), this);
					if (i == iend) break;
					state.print(", ");
				}
			}
			state.print(" ");
		}
		state.print("{\n");
		final State indentedState = state.indent();
		for (FieldDecl field : node.getFields()) {
			indentedState.printIndent().print(field, this).print(";\n");
		}
		for (AbstractMethodDecl<?> method : node.getMethods()) {
			indentedState.print("\n").printIndent().print(method, this).print("\n");
		}
		for (ClassDecl memberType : node.getMemberTypes()) {
			indentedState.print("\n").printIndent().print(memberType, this).print("\n");
		}
		return state.printIndent().print("}");
	}

	@Override
	public State visitConstructorDecl(final ConstructorDecl node, final State state) {
		for (Annotation annotation : node.getAnnotations()) {
			state.print(annotation, this).print(" ");
		}
		writeModifiers(node.getModifiers(), state);
		if (!node.getTypeParameters().isEmpty()) {
			state.print("<");
			for (int i = 0, iend = node.getTypeParameters().size() - 1; i <= iend; i++) {
				state.print(node.getTypeParameters().get(i), this);
				if (i == iend) break;
				state.print(", ");
			}
			state.print("> ");
		}
		state.print(node.getName()).print("(");
		for (int i = 0, iend = node.getArguments().size() - 1; i <= iend; i++) {
			state.print(node.getArguments().get(i), this);
			if (i == iend) break;
			state.print(", ");
		}
		state.print(")");
		if (!node.getThrownExceptions().isEmpty()) {
			state.print(" throws ");
			for (int i = 0, iend = node.getThrownExceptions().size() - 1; i <= iend; i++) {
				state.print(node.getThrownExceptions().get(i), this);
				if (i == iend) break;
				state.print(", ");
			}
		}
		state.print(" {\n");
		final State indentedState = state.indent();
		if (node.implicitSuper()) {
			indentedState.printIndent().print("super();\n");
		}
		for (Statement<?> statement : node.getStatements()) {
			indentedState.printIndent().print(statement, this).print(";\n");
		}
		return state.printIndent().print("}");
	}

	@Override
	public State visitContinue(final Continue node, final State state) {
		state.print("continue");
		if (node.getLabel() != null) state.print(" ").print(node.getLabel());
		return state;
	}

	@Override
	public State visitDoWhile(final DoWhile node, final State state) {
		return state.print("do ").print(node.getAction(), this).print("\nwhile ( ").print(node.getCondition(), this).print(")");
	}

	@Override
	public State visitEnumConstant(final EnumConstant node, final State state) {
		state.print(node.getName()).print("(");
		for (int i = 0, iend = node.getArgs().size() - 1; i <= iend; i++) {
			state.print(node.getArgs().get(i), this);
			if (i == iend) break;
			state.print(", ");
		}
		return state.print(")");
	}

	@Override
	public State visitFieldDecl(final FieldDecl node, final State state) {
		for (Annotation annotation : node.getAnnotations()) {
			state.print(annotation, this).print(" ");
		}
		writeModifiers(node.getModifiers(), state);
		state.print(node.getType(), this).print(" ").print(node.getName());
		if (node.getInitialization() != null) {
			state.print(" = ").print(node.getInitialization(), this);
		}
		return state;
	}

	@Override
	public State visitFieldRef(final FieldRef node, final State state) {
		return state.print(node.getReceiver(), this).print(".").print(node.getName());
	}

	@Override
	public State visitForeach(final Foreach node, final State state) {
		return state.print("for (").print(node.getElementVariable(), this).print(" : ").print(node.getCollection(), this).print(") ").print(node.getAction(), this);
	}

	@Override
	public State visitIf(final If node, final State state) {
		state.print("if (").print(node.getCondition(), this).print(") ").print(node.getThenStatement(), this);
		if (node.getElseStatement() != null) {
			state.print("\n").printIndent().print("else ").print(node.getElseStatement(), this);
		}
		return state;
	}

	@Override
	public State visitInitializer(Initializer node, State state) {
		state.print("{\n");
		final State indentedState = state.indent();
		for (Statement<?> statement : node.getStatements()) {
			indentedState.printIndent().print(statement, this).print(";\n");
		}
		return state.printIndent().print("}\n");
	}

	@Override
	public State visitInstanceOf(final InstanceOf node, final State state) {
		return state.print(node.getExpression(), this).print(" instanceof ").print(node.getType(), this);
	}

	@Override
	public State visitLocalDecl(final LocalDecl node, final State state) {
		for (Annotation annotation : node.getAnnotations()) {
			state.print(annotation, this).print(" ");
		}
		writeModifiers(node.getModifiers(), state);
		state.print(node.getType(), this).print(" ").print(node.getName());
		if (node.getInitialization() != null) {
			state.print(" = ").print(node.getInitialization(), this);
		}
		return state;
	}

	@Override
	public State visitMethodDecl(final MethodDecl node, final State state) {
		for (Annotation annotation : node.getAnnotations()) {
			state.print(annotation, this).print(" ");
		}
		writeModifiers(node.getModifiers(), state);
		if (!node.getTypeParameters().isEmpty()) {
			state.print("<");
			for (int i = 0, iend = node.getTypeParameters().size() - 1; i <= iend; i++) {
				state.print(node.getTypeParameters().get(i), this);
				if (i == iend) break;
				state.print(", ");
			}
			state.print("> ");
		}
		state.print(node.getReturnType(), this).print(" ").print(node.getName()).print("(");
		for (int i = 0, iend = node.getArguments().size() - 1; i <= iend; i++) {
			state.print(node.getArguments().get(i), this);
			if (i == iend) break;
			state.print(", ");
		}
		state.print(")");
		if (!node.getThrownExceptions().isEmpty()) {
			state.print(" throws ");
			for (int i = 0, iend = node.getThrownExceptions().size() - 1; i <= iend; i++) {
				state.print(node.getThrownExceptions().get(i), this);
				if (i == iend) break;
				state.print(", ");
			}
		}
		if (node.noBody()) {
			state.print(";\n");
		} else {
			state.print(" {\n");
			final State indentedState = state.indent();
			for (Statement<?> statement : node.getStatements()) {
				indentedState.printIndent().print(statement, this).print(";\n");
			}
			state.printIndent().print("}");
		}
		return state;
	}

	@Override
	public State visitNameRef(final NameRef node, final State state) {
		return state.print(node.getName());
	}

	@Override
	public State visitNew(final New node, final State state) {
		state.print("new ").print(node.getType(), this);
		if (!node.getTypeArgs().isEmpty()) {
			state.print("<");
			for (int i = 0, iend = node.getTypeArgs().size() - 1; i <= iend; i++) {
				state.print(node.getTypeArgs().get(i), this);
				if (i == iend) break;
				state.print(", ");
			}
			state.print(">");
		}
		state.print("(");
		for (int i = 0, iend = node.getArgs().size() - 1; i <= iend; i++) {
			state.print(node.getArgs().get(i), this);
			if (i == iend) break;
			state.print(", ");
		}
		state.print(")");
		if (node.getAnonymousType() != null) {
			state.print(node.getAnonymousType(), this);
		}
		return state;
	}

	@Override
	public State visitNewArray(final NewArray node, final State state) {
		// TODO
		return state;
	}

	@Override
	public State visitNullLiteral(final NullLiteral node, final State state) {
		return state.print("null");
	}

	@Override
	public State visitNumberLiteral(final NumberLiteral node, final State state) {
		final Number number = node.getNumber();
		if (number instanceof Integer) {
			return state.print(number.intValue());
		} else if (number instanceof Long) {
			return state.print(number.longValue()).print("L");
		} else if (number instanceof Float) {
			return state.print(number.floatValue()).print("f");
		} else {
			return state.print(number.doubleValue()).print("d");
		}
	}

	@Override
	public State visitReturn(final Return node, final State state) {
		state.print("return");
		if (node.getExpression() != null) {
			state.print(" ").print(node.getExpression(), this);
		}
		return state;
	}

	@Override
	public State visitReturnDefault(final ReturnDefault node, final State state) {
		return state.print("return defaultValue");
	}

	@Override
	public State visitStringLiteral(final StringLiteral node, final State state) {
		return state.print("\"").print(node.getString()).print("\"");
	}

	@Override
	public State visitSwitch(final Switch node, final State state) {
		state.print("switch (").print(node.getExpression(), this).print(") {\n");
		for (Case caze : node.getCases()) state.print(caze, this);
		return state.printIndent().print("}");
	}

	@Override
	public State visitSynchronized(final Synchronized node, final State state) {
		state.print("synchronized (").print(node.getLock(), this).print(") {\n");
		final State indentedState = state.indent();
		for (Statement<?> statement : node.getStatements()) {
			indentedState.printIndent().print(statement, this).print(";\n");
		}
		return state.printIndent().print("}\n");
	}

	@Override
	public State visitThis(final This node, final State state) {
		if (!node.isImplicit()) {
			if (node.getType() != null) {
				state.print(node.getType(), this).print(".");
			}
			state.print("this");
		}
		return state;
	}

	@Override
	public State visitThrow(final Throw node, final State state) {
		return state.print("throw ").print(node.getExpression(), this);
	}

	@Override
	public State visitTry(final Try node, final State state) {
		state.print("try ").print(node.getTryBlock(), this);
		final Iterator<lombok.ast.Argument> iter = node.getCatchArguments().iterator();
		for (lombok.ast.Block catchBlock : node.getCatchBlocks()) {
			lombok.ast.Argument catchArgument = iter.next();
			state.print("catch (").print(catchArgument, this).print(") ").print(catchBlock, this);
		}
		return state;
	}

	@Override
	public State visitTypeParam(final TypeParam node, final State state) {
		state.print(node.getName());
		if (!node.getBounds().isEmpty()) {
			state.print(" extends ");
			for (int i = 0, iend = node.getBounds().size() - 1; i <= iend; i++) {
				state.print(node.getBounds().get(i), this);
				if (i == iend) break;
				state.print(" & ");
			}
		}
		return state;
	}

	@Override
	public State visitTypeRef(final TypeRef node, final State state) {
		state.print(node.getTypeName());
		if (!node.getTypeArgs().isEmpty()) {
			state.print("<");
			for (int i = 0, iend = node.getTypeArgs().size() - 1; i <= iend; i++) {
				state.print(node.getTypeArgs().get(i), this);
				if (i == iend) break;
				state.print(", ");
			}
			state.print(">");
		}
		for (int i = 0, iend = node.getDims(); i < iend; i++) state.print("[]");
		return state;
	}

	@Override
	public State visitUnary(final Unary node, final State state) {
		return state.print(node.getOperator()).print(node.getExpression(), this);
	}

	@Override
	public State visitWhile(final While node, final State state) {
		return state.print("while (").print(node.getCondition(), this).print(") ").print(node.getAction(), this);
	}

	@Override
	public State visitWildcard(final Wildcard node, final State state) {
		state.print("?");
		if (node.getBound() != null) {
			state.print(" ").print(node.getBound().name().toLowerCase()).print(" ").print(node.getType(), this);
		}
		return state;
	}

	@Override
	public State visitWrappedExpression(final WrappedExpression node, final State state) {
		return state.print(node.getWrappedObject());
	}

	@Override
	public State visitWrappedMethodDecl(final WrappedMethodDecl node, final State state) {
		return state.print(node.getWrappedObject());
	}

	@Override
	public State visitWrappedStatement(final WrappedStatement node, final State state) {
		return state.print(node.getWrappedObject());
	}

	@Override
	public State visitWrappedTypeRef(final WrappedTypeRef node, final State state) {
		return state.print(node.getWrappedObject());
	}

	@RequiredArgsConstructor
	public static class State {
		private final PrintStream out;
		private final String indent;
		private final int depth;

		public State(final PrintStream out) {
			this(out, "  ", 0);
		}

		public State(final PrintStream out, final String indent) {
			this(out, indent, 0);
		}

		public State indent() {
			return new State(out, indent, depth + 1);
		}

		public State printIndent() {
			for (int i = 0; i < depth; i++) {
				out.append(indent);
			}
			return this;
		}

		public State print(final CharSequence csq) {
			out.append(csq);
			return this;
		}

		public State print(final Object o) {
			return print(o.toString());
		}

		public State print(final Node<?> node, final ASTPrinter printer) {
			return node.accept(printer, this);
		}
	}
}
