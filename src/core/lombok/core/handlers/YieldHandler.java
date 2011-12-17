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
 * 
 * Credit where credit is due:
 * ===========================
 * 
 *   Yield is based on the idea, algorithms and sources of
 *   Arthur and Vladimir Nesterovsky (http://www.nesterovsky-bros.com/weblog),
 *   who generously allowed me to use them.
 */
package lombok.core.handlers;

import static lombok.ast.AST.*;
import static lombok.core.util.Names.*;

import java.io.Closeable;
import java.util.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.util.Is;

public class YieldHandler<METHOD_TYPE extends IMethod<?, ?, ?, ?>, AST_BASE_TYPE> {

	public boolean handle(final METHOD_TYPE method, final AbstractYieldDataCollector<METHOD_TYPE, AST_BASE_TYPE> collector) {
		final boolean returnsIterable = method.returns(Iterable.class);
		final boolean returnsIterator = method.returns(Iterator.class);
		if (!(returnsIterable || returnsIterator)) {
			method.node().addError("Method that contain yield() can only return java.util.Iterator or java.lang.Iterable.");
			return true;
		}
		if (method.hasNonFinalArgument()) {
			method.node().addError("Parameters should be final.");
			return true;
		}

		final String stateName = "$state";
		final String nextName = "$next";
		final String errorName = "$yieldException";
		collector.collect(method, stateName, nextName, errorName, returnsIterable);

		if (!collector.hasYields()) {
			return true;
		}

		final String yielderName = collector.yielderName(method);
		ClassDecl yielder = collector.getYielder();
		method.replaceBody(yielder, Return(New(Type(yielderName))));
		method.rebuild();

		return true;
	}

	public static abstract class AbstractYieldDataCollector<METHOD_TYPE extends IMethod<?, ?, ?, ?>, AST_BASE_TYPE> {
		protected METHOD_TYPE method;
		protected Set<String> names = new HashSet<String>();
		protected List<Scope<AST_BASE_TYPE>> yields = new ArrayList<Scope<AST_BASE_TYPE>>();
		protected List<Scope<AST_BASE_TYPE>> breaks = new ArrayList<Scope<AST_BASE_TYPE>>();
		protected List<Scope<AST_BASE_TYPE>> variableDecls = new ArrayList<Scope<AST_BASE_TYPE>>();
		@Getter
		protected List<FieldDecl> stateVariables = new ArrayList<FieldDecl>();
		protected Scope<AST_BASE_TYPE> root;
		protected Scope<AST_BASE_TYPE> current;
		protected Map<AST_BASE_TYPE, Scope<AST_BASE_TYPE>> allScopes = new HashMap<AST_BASE_TYPE, Scope<AST_BASE_TYPE>>();
		protected List<Case> cases = new ArrayList<Case>();
		protected List<Statement<?>> statements = new ArrayList<Statement<?>>();
		protected List<Case> breakCases = new ArrayList<Case>();
		protected List<ErrorHandler> errorHandlers = new ArrayList<ErrorHandler>();
		protected int finallyBlocks;
		protected Map<NumberLiteral, Case> labelLiterals = new HashMap<NumberLiteral, Case>();
		protected Set<Case> usedLabels = new HashSet<Case>();
		protected String stateName;
		protected String nextName;
		protected String errorName;
		protected boolean returnsIterable;

		public ClassDecl getYielder() {
			final String yielderName = yielderName(method);
			final String elementType = elementType(method);
			final List<FieldDecl> variables = getStateVariables();
			final Switch stateSwitch = getStateSwitch();
			final Switch errorHandlerSwitch = getErrorHandlerSwitch();
			final Statement<?> closeStatement = getCloseStatement();

			ClassDecl yielder = ClassDecl(yielderName).posHint(method.get()).makeLocal().implementing(Type(Iterator.class).withTypeArgument(Type(elementType))) //
					.withFields(variables) //
					.withField(FieldDecl(Type("int"), stateName).makePrivate()) //
					.withField(FieldDecl(Type("boolean"), "$hasNext").makePrivate()) //
					.withField(FieldDecl(Type("boolean"), "$nextDefined").makePrivate()) //
					.withField(FieldDecl(Type(elementType), nextName).makePrivate()) //
					.withMethod(ConstructorDecl(yielderName).withImplicitSuper().makePrivate()); //
			if (returnsIterable) {
				yielder.implementing(Type(Iterable.class).withTypeArgument(Type(elementType))) //
						.withMethod(MethodDecl(Type(Iterator.class).withTypeArgument(Type(elementType)), "iterator").makePublic() //
								.withStatement(If(Equal(Name(stateName), Number(0))).Then(Block() //
										.withStatement(Assign(Name(stateName), Number(1))) //
										.withStatement(Return(This()))) //
										.Else(Return(New(Type(yielderName))))));
			}
			yielder.implementing(Type(Closeable.class)) //
					.withMethod(MethodDecl(Type("boolean"), "hasNext").makePublic() //
							.withStatement(If(Not(Name("$nextDefined"))).Then(Block() //
									.withStatement(Assign(Name("$hasNext"), Call("getNext"))) //
									.withStatement(Assign(Name("$nextDefined"), True())))) //
							.withStatement(Return(Name("$hasNext")))) //
					.withMethod(MethodDecl(Type(elementType), "next").makePublic() //
							.withStatement(If(Not(Call("hasNext"))).Then(Block() //
									.withStatement(Throw(New(Type(NoSuchElementException.class)))))) //
							.withStatement(Assign(Name("$nextDefined"), False())) //
							.withStatement(Return(Name(nextName)))) //
					.withMethod(MethodDecl(Type("void"), "remove").makePublic() //
							.withStatement(Throw(New(Type(UnsupportedOperationException.class))))) //
					.withMethod(MethodDecl(Type("void"), "close").makePublic() //
							.withStatement(closeStatement));
			if (errorHandlerSwitch != null) {
				String caughtErrorName = errorName + "Caught";
				yielder.withMethod(MethodDecl(Type("boolean"), "getNext").makePrivate() //
						.withStatement(LocalDecl(Type(Throwable.class), errorName)) //
						.withStatement(While(True()).Do(Block() //
								.withStatement(Try(Block() //
										.withStatement(stateSwitch)) //
										.Catch(Arg(Type(Throwable.class), caughtErrorName), Block() //
												.withStatement(Assign(Name(errorName), Name(caughtErrorName))))) //
								.withStatement(errorHandlerSwitch))));
			} else {
				yielder.withMethod(MethodDecl(Type("boolean"), "getNext").makePrivate() //
						.withStatement(While(True()).Do(stateSwitch)));
			}
			return yielder;
		}

		public Switch getStateSwitch() {
			final List<Case> switchCases = new ArrayList<Case>();
			for (Case label : cases) if (label != null) switchCases.add(label);
			return Switch(Name(stateName)).withCases(switchCases).withCase(Case().withStatement(Return(False())));
		}

		public Switch getErrorHandlerSwitch() {
			if (errorHandlers.isEmpty()) {
				return null;
			} else {
				final List<Case> switchCases = new ArrayList<Case>();
				final Set<Case> labels = new HashSet<Case>();
				for (ErrorHandler handler : errorHandlers) {
					Case lastCase = null;
					for (int i = handler.begin; i < handler.end; i++) {
						Case label = cases.get(i);
						if ((label != null) && labels.add(label)) {
							lastCase = Case(label.getPattern());
							switchCases.add(lastCase);
						}
					}
					if (lastCase != null) {
						lastCase.withStatements(handler.statements);
					}
				}

				final String unhandledErrorName = errorName + "Unhandled";
				switchCases.add(Case() //
						.withStatement(setState(literal(getBreakLabel(root)))) //
						.withStatement(LocalDecl(Type(ConcurrentModificationException.class), unhandledErrorName).withInitialization(New(Type(ConcurrentModificationException.class)))) //
						.withStatement(Call(Name(unhandledErrorName), "initCause").withArgument(Name(errorName))) //
						.withStatement(Throw(Name(unhandledErrorName))));
				return Switch(Name(stateName)).withCases(switchCases);
			}
		}

		public Statement<?> getCloseStatement() {
			final Statement<?> statement = setState(literal(getBreakLabel(root)));
			if (breakCases.isEmpty()) {
				return statement;
			} else {
				Number prev = null;
				final List<Case> switchCases = new ArrayList<Case>();
				for (final Case breakCase : breakCases) {
					NumberLiteral literal = (NumberLiteral) breakCase.getPattern();
					Number value = literal.getNumber();
					if ((prev != null) && prev.equals(value)) continue;
					switchCases.add(breakCase);
					prev = value;
				}
				switchCases.add(Case() //
						.withStatement(statement) //
						.withStatement(Return()));
				return Do(Switch(Name(stateName)).withCases(switchCases)).While(Call("getNext"));
			}
		}

		public boolean hasYields() {
			return !yields.isEmpty();
		}

		public void collect(final METHOD_TYPE method, final String state, final String next, final String errorName, final boolean returnsIterable) {
			this.method = method;
			this.stateName = state;
			this.nextName = next;
			this.errorName = errorName;
			this.returnsIterable = returnsIterable;

			if (scan()) {
				prepareRefactor();
				refactor();
			}
		}

		public String yielderName(final METHOD_TYPE method) {
			String[] parts = method.name().split("_");
			String[] newParts = new String[parts.length + 1];
			newParts[0] = "yielder";
			System.arraycopy(parts, 0, newParts, 1, parts.length);
			return camelCase("$", newParts);
		}

		public abstract String elementType(final METHOD_TYPE method);

		public abstract boolean scan();

		public abstract void prepareRefactor();

		public void refactor() {
			current = root;

			Case begin = Case();
			Case iteratorLabel = getIterationLabel(root);

			usedLabels.add(begin);
			usedLabels.add(iteratorLabel);
			usedLabels.add(getBreakLabel(root));

			addLabel(begin);
			addStatement(setState(literal(iteratorLabel)));
			addLabel(iteratorLabel);
			root.refactor();
			endCase();

			optimizeStates();
			synchronizeLiteralsAndLabels();
		}

		public Expression<?> getStateFromAssignment(Statement<?> statement) {
			if (statement instanceof Assignment) {
				Assignment assign = (Assignment) statement;
				if (assign.getLeft() instanceof NameRef) {
					NameRef field = (NameRef) assign.getLeft();
					if (stateName.equals(field.getName())) {
						return assign.getRight();
					}
				}
			}
			return null;
		}

		public Case getLabel(Expression<?> expression) {
			return expression == null ? null : labelLiterals.get(expression);
		}

		public void endCase() {
			if (!cases.isEmpty()) {
				Case lastCase = cases.get(cases.size() - 1);
				if (lastCase.getStatements().isEmpty() && !statements.isEmpty()) {
					lastCase.withStatements(statements);
					statements.clear();
				}
			}
		}

		public void addLabel(final Case label) {
			endCase();
			label.withPattern(Number(cases.size()));
			cases.add(label);
		}

		public void addStatement(final Statement<?> statement) {
			statements.add(statement);
		}

		public Case getBreakLabel(final Scope<AST_BASE_TYPE> scope) {
			Case label = scope.breakLabel;
			if (label == null) {
				label = Case();
				scope.breakLabel = label;
			}
			return label;
		}

		public Case getIterationLabel(final Scope<AST_BASE_TYPE> scope) {
			Case label = scope.iterationLabel;
			if (label == null) {
				label = Case();
				scope.iterationLabel = label;
			}
			return label;
		}

		public Case getFinallyLabel(Scope<AST_BASE_TYPE> scope) {
			Case label = scope.finallyLabel;
			if (label == null) {
				label = Case();
				scope.finallyLabel = label;
			}
			return label;
		}

		public Expression<?> literal(final Case label) {
			NumberLiteral pattern = (NumberLiteral) label.getPattern();
			NumberLiteral literal = Number(pattern == null ? -1 : pattern.getNumber());
			labelLiterals.put(literal, label);
			return literal;
		}

		public Statement<?> setState(final Expression<?> expression) {
			return Assign(Name(stateName), expression);
		}

		public void refactorStatement(final Object statement) {
			if (statement == null) return;
			Scope<AST_BASE_TYPE> scope = allScopes.get(statement);
			if (scope != null) {
				Scope<AST_BASE_TYPE> previous = current;
				current = scope;
				scope.refactor();
				current = previous;
			} else {
				addStatement(Stat(statement));
			}
		}

		public void optimizeStates() {
			optimizeStateChanges();
			optimizeSuccessiveStates();
		}

		/**
		 * Handles the following scenarios:
		 * <ol>
		 * <li>
		 * 
		 * <pre>
		 * case 2:
		 * case 3:                  => merge 2 and 3 into 4
		 * case 4:
		 *   bodyOf4();
		 * </pre>
		 * 
		 * </li>
		 * <li>
		 * 
		 * <pre>
		 * case 2:
		 *   $state = 5;            => merge 2 into 5
		 *   continue;
		 * // case 3-4
		 * case 5:
		 *   bodyOf5();
		 * </pre>
		 * 
		 * </li>
		 * </ol>
		 */
		public void optimizeStateChanges() {
			int count = cases.size();
			for (Map.Entry<NumberLiteral, Case> entry : labelLiterals.entrySet()) {
				Case label = entry.getValue();
				while (label.getPattern() != null) {
					if (label.getStatements().isEmpty()) {
						NumberLiteral literal = (NumberLiteral) label.getPattern();
						int i = (Integer) literal.getNumber() + 1;
						if (i < count) {
							label = cases.get(i);
						} else {
							break;
						}
					} else {
						Case next = getLabel(getStateFromAssignment(label.getStatements().get(0)));
						int numberOfStatements = label.getStatements().size();
						if ((next != null) && ((numberOfStatements == 1) || ((numberOfStatements > 1) && (label.getStatements().get(1) instanceof Continue)))) {
							label = next;
						} else {
							break;
						}
					}
				}
				entry.setValue(label);
				if (label.getPattern() != null) {
					usedLabels.add(label);
				}
			}
		}

		/**
		 * Handles the following scenarios:
		 * <ol>
		 * <li>
		 * 
		 * <pre>
		 * case 2:
		 *   bodyOf2();
		 *   // doesn't end with
		 *   //continue;
		 *   // or                  => case 2:
		 *   //return true;              bodyOf2();
		 * case 3:                       bodyOf3();
		 *   bodyOf3();
		 * </pre>
		 * 
		 * </li>
		 * <li>
		 * 
		 * <pre>
		 * case 2:
		 *   bodyOf2();             => case 2:
		 *   continue;                   bodyOf2();
		 *   unreachableBodyOf2();       continue;
		 * </pre>
		 * 
		 * </li>
		 * </ol>
		 */
		public void optimizeSuccessiveStates() {
			Case previous = null;
			for (int i = 0, id = 0, iend = cases.size(); i < iend; i++) {
				Case label = cases.get(i);
				if (!usedLabels.contains(label) && (previous != null)) {
					Statement<?> last = previous.getStatements().get(previous.getStatements().size() - 1);
					if (!label.getStatements().isEmpty() && Is.noneOf(last, Continue.class, Return.class)) {
						previous.withStatements(label.getStatements());
					}
					cases.set(i, null);
					continue;
				}
				NumberLiteral literal = (NumberLiteral) label.getPattern();
				literal.setNumber(id++);
				if (previous == null) {
					previous = label;
					continue;
				}
				boolean found = false;
				boolean remove = false;
				List<Statement<?>> list = previous.getStatements();
				for (Iterator<Statement<?>> iter = list.iterator(); iter.hasNext();) {
					Statement<?> statement = iter.next();
					if (remove || (found && (statement instanceof Continue))) {
						remove = true;
						iter.remove();
					} else {
						found = getLabel(getStateFromAssignment(statement)) == label;
					}
				}
				previous = label;
			}
		}

		public void synchronizeLiteralsAndLabels() {
			for (Map.Entry<NumberLiteral, Case> entry : labelLiterals.entrySet()) {
				Case label = entry.getValue();
				if (label != null) {
					NumberLiteral literal = (NumberLiteral) label.getPattern();
					entry.getKey().setNumber(literal.getNumber());
				}
			}
		}
	}

	public static class ErrorHandler {
		public int begin;
		public int end;
		public List<Statement<?>> statements = new ArrayList<Statement<?>>();
	}

	@RequiredArgsConstructor
	public abstract static class Scope<AST_BASE_TYPE> {
		public final Scope<AST_BASE_TYPE> parent;
		public final AST_BASE_TYPE node;
		public Scope<AST_BASE_TYPE> target;
		public String labelName;
		public Case iterationLabel;
		public Case breakLabel;
		public Case finallyLabel;

		public abstract void refactor();
	}
}
