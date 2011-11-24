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
package lombok.eclipse.handlers.ast;

import static org.eclipse.jdt.internal.compiler.ast.ASTNode.IsSuperType;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;
import static org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers.AccSemicolonBody;
import static org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers.AccImplementing;

import static lombok.ast.AST.*;
import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.Eclipse.poss;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;
import static lombok.eclipse.handlers.Eclipse.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.BreakStatement;
import org.eclipse.jdt.internal.compiler.ast.CaseStatement;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.CharLiteral;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ContinueStatement;
import org.eclipse.jdt.internal.compiler.ast.DoStatement;
import org.eclipse.jdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.jdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.jdt.internal.compiler.ast.EqualExpression;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.FloatLiteral;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LongLiteral;
import org.eclipse.jdt.internal.compiler.ast.MagicLiteral;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.NumberLiteral;
import org.eclipse.jdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.SynchronizedStatement;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;

import lombok.*;
import lombok.ast.Node;
import lombok.core.util.As;
import lombok.core.util.Cast;
import lombok.core.util.Each;
import lombok.core.util.Is;
import lombok.eclipse.EclipseNode;

@RequiredArgsConstructor
public final class EclipseASTMaker implements lombok.ast.ASTVisitor<ASTNode, Void> {
	private final EclipseNode sourceNode;
	private final ASTNode source;

	public <T extends ASTNode> T build(final lombok.ast.Node<?> node) {
		return this.<T>build(node, null);
	}

	public <T extends ASTNode> T build(final lombok.ast.Node<?> node, final Class<T> extectedType) {
		if (node == null) return null;
		return Cast.<T>uncheckedCast(node.accept(this, null));
	}

	public <T extends ASTNode> List<T> build(final List<? extends lombok.ast.Node<?>> nodes) {
		return this.<T>build(nodes, null);
	}

	public <T extends ASTNode> List<T> build(final List<? extends lombok.ast.Node<?>> nodes, final Class<T> extectedType) {
		if (nodes == null) return null;
		final List<T> list = new ArrayList<T>();
		for (lombok.ast.Node<?> node : nodes) {
			list.add(build(node, extectedType));
		}
		return list;
	}

	private ASTNode posHintOf(final Node<?> node) {
		ASTNode posHint = node.posHint();
		return posHint == null ? source : posHint;
	}

	private int modifiersFor(final Set<lombok.ast.Modifier> modifiers) {
		int mods = 0;
		mods |= modifiers.contains(lombok.ast.Modifier.FINAL) ? AccFinal : 0;
		mods |= modifiers.contains(lombok.ast.Modifier.PRIVATE) ? AccPrivate : 0;
		mods |= modifiers.contains(lombok.ast.Modifier.PROTECTED) ? AccProtected: 0;
		mods |= modifiers.contains(lombok.ast.Modifier.PUBLIC) ? AccPublic : 0;
		mods |= modifiers.contains(lombok.ast.Modifier.STATIC) ? AccStatic : 0;
		mods |= modifiers.contains(lombok.ast.Modifier.TRANSIENT) ? AccTransient : 0;
		mods |= modifiers.contains(lombok.ast.Modifier.VOLATILE) ? AccVolatile : 0;
		return mods;
	}

	private Statement getEmptyStatement(final Node<?> node) {
		final EmptyStatement emptyStatement = new EmptyStatement(0, 0);
		setGeneratedByAndCopyPos(emptyStatement, source, posHintOf(node));
		return emptyStatement;
	}

	private static <ELEMENT_TYPE> ELEMENT_TYPE[] toArray(final List<?> list, final ELEMENT_TYPE[] array) {
		if ((list != null) && !list.isEmpty()) {
			return list.toArray(array);
		}
		return null;
	}

	@Override
	public ASTNode visitAnnotation(final lombok.ast.Annotation node, final Void p) {
		final Annotation ann;
		if (node.getValues().isEmpty()) {
			ann = new MarkerAnnotation(build(node.getType(), TypeReference.class), 0);
		} else if (node.getValues().containsKey("value") && node.getValues().size() == 1) {
			ann = new SingleMemberAnnotation(build(node.getType(), TypeReference.class), 0);
			((SingleMemberAnnotation)ann).memberValue = build(node.getValues().get("value"));
		} else {
			ann = new NormalAnnotation(build(node.getType(), TypeReference.class), 0);
			List<MemberValuePair> valuePairs = new ArrayList<MemberValuePair>();
			for (Entry<String, lombok.ast.Expression<?>> entry : node.getValues().entrySet()) {
				MemberValuePair valuePair = new MemberValuePair(entry.getKey().toCharArray(), 0, 0, build(entry.getValue(), Expression.class));
				setGeneratedByAndCopyPos(valuePair, source, posHintOf(node));
				valuePairs.add(valuePair);
			}
			((NormalAnnotation)ann).memberValuePairs = valuePairs.toArray(new MemberValuePair[0]);
		}
		setGeneratedByAndCopyPos(ann, source, posHintOf(node));
		return ann;
	}

	@Override
	public ASTNode visitArgument(final lombok.ast.Argument node, final Void p) {
		final Argument argument = new Argument(node.getName().toCharArray(), 0, null, 0);
		setGeneratedByAndCopyPos(argument, source, posHintOf(node));
		argument.modifiers = modifiersFor(node.getModifiers());
		argument.annotations = toArray(build(node.getAnnotations()), new Annotation[0]);
		argument.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		argument.type = build(node.getType());
		return argument;
	}

	@Override
	public ASTNode visitArrayRef(final lombok.ast.ArrayRef node, final Void p) {
		final ArrayReference arrayReference = new ArrayReference(build(node.getIndexed(), Expression.class), build(node.getIndex(), Expression.class));
		setGeneratedByAndCopyPos(arrayReference, source, posHintOf(node));
		return arrayReference;
	}

	@Override
	public ASTNode visitAssignment(final lombok.ast.Assignment node, final Void p) {
		final Assignment assignment = new Assignment(build(node.getLeft(), Expression.class), build(node.getRight(), Expression.class), 0);
		setGeneratedByAndCopyPos(assignment, source, posHintOf(node));
		return assignment;
	}

	@Override
	public ASTNode visitBinary(final lombok.ast.Binary node, final Void p) {
		final String operator = node.getOperator();
		final int opCode;
		if ("+".equals(operator)) {
			opCode = OperatorIds.PLUS;
		} else if ("-".equals(operator)) {
			opCode = OperatorIds.MINUS;
		} else if ("*".equals(operator)) {
			opCode = OperatorIds.MULTIPLY;
		} else if ("/".equals(operator)) {
			opCode = OperatorIds.DIVIDE;
		} else if ("||".equals(operator)) {
			opCode = OperatorIds.OR_OR;
		} else if ("&&".equals(operator)) {
			opCode = OperatorIds.AND_AND;
		} else if ("==".equals(operator)) {
			opCode = OperatorIds.EQUAL_EQUAL;
		} else if ("!=".equals(operator)) {
			opCode = OperatorIds.NOT_EQUAL;
		} else {
			throw new IllegalStateException(String.format("Unknown binary operator '%s'", operator));
		}
		final BinaryExpression binaryExpression;
		if ("||".equals(operator)) {
			binaryExpression = new OR_OR_Expression(build(node.getLeft(), Expression.class), build(node.getRight(), Expression.class), opCode);
		} else if ("&&".equals(operator)) {
			binaryExpression = new AND_AND_Expression(build(node.getLeft(), Expression.class), build(node.getRight(), Expression.class), opCode);
		} else if (Is.oneOf(operator, "==", "!=")) {
			binaryExpression = new EqualExpression(build(node.getLeft(), Expression.class), build(node.getRight(), Expression.class), opCode);
		} else {
			binaryExpression = new BinaryExpression(build(node.getLeft(), Expression.class), build(node.getRight(), Expression.class), opCode);
		}
		setGeneratedByAndCopyPos(binaryExpression, source, posHintOf(node));
		return binaryExpression;
	}

	@Override
	public ASTNode visitBlock(final lombok.ast.Block node, final Void p) {
		final Block block = new Block(0);
		setGeneratedByAndCopyPos(block, source, posHintOf(node));
		block.statements = toArray(build(node.getStatements()), new Statement[0]);
		return block;
	}

	@Override
	public ASTNode visitBooleanLiteral(final lombok.ast.BooleanLiteral node, final Void p) {
		final MagicLiteral literal;
		if (node.isTrue()) {
			literal = new TrueLiteral(0, 0);
		} else {
			literal = new FalseLiteral(0, 0);
		}
		setGeneratedByAndCopyPos(literal, source, posHintOf(node));
		return literal;
	}

	@Override
	public ASTNode visitBreak(final lombok.ast.Break node, final Void p) {
		final BreakStatement breakStatement = new BreakStatement(node.getLabel() == null ? null : node.getLabel().toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(breakStatement, source, posHintOf(node));
		return breakStatement;
	}

	@Override
	public ASTNode visitCall(final lombok.ast.Call node, final Void p) {
		final MessageSend messageSend = new MessageSend();
		setGeneratedByAndCopyPos(messageSend, source, posHintOf(node));
		if (node.getReceiver() == null) {
			messageSend.receiver = build(This().implicit());
		} else {
			messageSend.receiver = build(node.getReceiver());
		}
		messageSend.selector = node.getName().toCharArray();
		messageSend.typeArguments = toArray(build(node.getTypeArgs()), new TypeReference[0]);
		messageSend.arguments = toArray(build(node.getArgs()), new Expression[0]);
		return messageSend;
	}

	@Override
	public ASTNode visitCast(final lombok.ast.Cast node, final Void p) {
		final CastExpression castExpression = createCastExpression(build(node.getExpression(), Expression.class), build(node.getType(), TypeReference.class));
		setGeneratedByAndCopyPos(castExpression, source, posHintOf(node));
		return castExpression;
	}

	private CastExpression createCastExpression(final Expression expression, final Expression typeRef) {
		try {
			return Reflection.castExpressionConstructor.newInstance(expression, typeRef);
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public ASTNode visitCase(final lombok.ast.Case node, final Void p) {
		throw new IllegalStateException("");
	}

	@Override
	public ASTNode visitCharLiteral(final lombok.ast.CharLiteral node, final Void p) {
		final CharLiteral literal = new CharLiteral(node.getCharacter().toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(literal, source, posHintOf(node));
		return literal;
	}

	@Override
	public ASTNode visitClassDecl(final lombok.ast.ClassDecl node, final Void p) {
		final TypeDeclaration typeDeclaration = new TypeDeclaration(((CompilationUnitDeclaration) sourceNode.top().get()).compilationResult);
		setGeneratedByAndCopyPos(typeDeclaration, source, posHintOf(node));
		typeDeclaration.modifiers = modifiersFor(node.getModifiers());
		if (node.isInterface()) typeDeclaration.modifiers |= AccInterface;
		typeDeclaration.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		if (node.isLocal()) typeDeclaration.bits |= ASTNode.IsLocalType;
		if (node.isAnonymous()) {
			typeDeclaration.bits |= ASTNode.IsAnonymousType;
		}
		if (Is.empty(node.getName())) {
			typeDeclaration.name = CharOperation.NO_CHAR;
		} else {
			typeDeclaration.name = node.getName().toCharArray();
		}
		typeDeclaration.annotations = toArray(build(node.getAnnotations()), new Annotation[0]);
		typeDeclaration.typeParameters = toArray(build(node.getTypeParameters()), new TypeParameter[0]);
		typeDeclaration.fields = toArray(build(node.getFields()), new FieldDeclaration[0]);
		typeDeclaration.methods = toArray(build(node.getMethods()), new AbstractMethodDeclaration[0]);
		typeDeclaration.memberTypes = toArray(build(node.getMemberTypes()), new TypeDeclaration[0]);
		typeDeclaration.superInterfaces = toArray(build(node.getSuperInterfaces()), new TypeReference[0]);
		if (node.getSuperclass() != null) {
			typeDeclaration.superclass = build(node.getSuperclass());
		}
		return typeDeclaration;
	}

	@Override
	public ASTNode visitConstructorDecl(final lombok.ast.ConstructorDecl node, final Void p) {
		final ConstructorDeclaration constructorDeclaration = new ConstructorDeclaration(((CompilationUnitDeclaration) sourceNode.top().get()).compilationResult);
		setGeneratedByAndCopyPos(constructorDeclaration, source, posHintOf(node));
		constructorDeclaration.modifiers = modifiersFor(node.getModifiers());
		constructorDeclaration.annotations = toArray(build(node.getAnnotations()), new Annotation[0]);
		if (node.implicitSuper()) {
			constructorDeclaration.constructorCall = new ExplicitConstructorCall(ExplicitConstructorCall.ImplicitSuper);
		}
		constructorDeclaration.selector = node.getName().toCharArray();
		constructorDeclaration.thrownExceptions = toArray(build(node.getThrownExceptions()), new TypeReference[0]);
		constructorDeclaration.typeParameters = toArray(build(node.getTypeParameters()), new TypeParameter[0]);
		constructorDeclaration.bits |=  ECLIPSE_DO_NOT_TOUCH_FLAG;
		constructorDeclaration.arguments = toArray(build(node.getArguments()), new Argument[0]);
		if (!node.getStatements().isEmpty()) {
			constructorDeclaration.statements = toArray(build(node.getStatements()), new Statement[0]);
		}
		return constructorDeclaration;
	}

	@Override
	public ASTNode visitContinue(final lombok.ast.Continue node, final Void p) {
		final ContinueStatement continueStatement = new ContinueStatement(node.getLabel() == null ? null : node.getLabel().toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(continueStatement, source, posHintOf(node));
		return continueStatement;
	}

	@Override
	public ASTNode visitDoWhile(final lombok.ast.DoWhile node, final Void p) {
		final DoStatement doStatement = new DoStatement(build(node.getCondition(), Expression.class), build(node.getAction(), Statement.class), 0, 0);
		setGeneratedByAndCopyPos(doStatement, source, posHintOf(node));
		return doStatement;
	}

	@Override
	public ASTNode visitEnumConstant(final lombok.ast.EnumConstant node, final Void p) {
		final AllocationExpression allocationExpression = new AllocationExpression();
		setGeneratedByAndCopyPos(allocationExpression, source, posHintOf(node));
		allocationExpression.arguments = toArray(build(node.getArgs()), new Expression[0]);
		allocationExpression.enumConstant = new FieldDeclaration(node.getName().toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(allocationExpression.enumConstant, source, posHintOf(node));
		allocationExpression.enumConstant.initialization = allocationExpression;
		return allocationExpression.enumConstant;
	}

	@Override
	public ASTNode visitFieldDecl(final lombok.ast.FieldDecl node, final Void p) {
		final FieldDeclaration fieldDeclaration = new FieldDeclaration(node.getName().toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(fieldDeclaration, source, posHintOf(node));
		fieldDeclaration.modifiers = modifiersFor(node.getModifiers());
		fieldDeclaration.annotations = toArray(build(node.getAnnotations()), new Annotation[0]);
		fieldDeclaration.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		fieldDeclaration.type = build(node.getType());
		if (node.getInitialization() != null) {
			fieldDeclaration.initialization = build(node.getInitialization());
		}
		return fieldDeclaration;
	}

	@Override
	public ASTNode visitFieldRef(final lombok.ast.FieldRef node, final Void p) {
		FieldReference fieldRef = new FieldReference(node.getName().toCharArray(), 0);
		fieldRef.receiver = build(node.getReceiver());
		setGeneratedByAndCopyPos(fieldRef, source, posHintOf(node));
		return fieldRef;
	}

	@Override
	public ASTNode visitForeach(final lombok.ast.Foreach node, final Void p) {
		final ForeachStatement forEach = new ForeachStatement(build(node.getElementVariable(), LocalDeclaration.class), 0);
		setGeneratedByAndCopyPos(forEach, source, posHintOf(node));
		forEach.collection = build(node.getCollection());
		forEach.action = build(node.getAction());
		return forEach;
	}

	@Override
	public ASTNode visitIf(final lombok.ast.If node, final Void p) {
		final Statement thenStatement = node.getThenStatement() == null ? getEmptyStatement(node) : build(node.getThenStatement(), Statement.class);
		final IfStatement ifStatement = new IfStatement(build(node.getCondition(), Expression.class), thenStatement, 0, 0);
		if (node.getElseStatement() != null) {
			ifStatement.elseStatement = build(node.getElseStatement());
		}
		setGeneratedByAndCopyPos(ifStatement, source, posHintOf(node));
		return ifStatement;
	}

	@Override
	public ASTNode visitInitializer(lombok.ast.Initializer node, Void p) {
		final Block block = new Block(0);
		setGeneratedByAndCopyPos(block, source, posHintOf(node));
		block.statements = toArray(build(node.getStatements()), new Statement[0]);
		final Initializer initializer = new Initializer(block, modifiersFor(node.getModifiers()));
		initializer.bits |=  ECLIPSE_DO_NOT_TOUCH_FLAG;
		setGeneratedByAndCopyPos(initializer, source, posHintOf(node));
		return initializer;
	}

	@Override
	public ASTNode visitInstanceOf(final lombok.ast.InstanceOf node, final Void p) {
		final InstanceOfExpression instanceOfExpression = new InstanceOfExpression(build(node.getExpression(), Expression.class), build(node.getType(), TypeReference.class));
		setGeneratedByAndCopyPos(instanceOfExpression, source, posHintOf(node));
		return instanceOfExpression;
	}

	@Override
	public ASTNode visitLocalDecl(final lombok.ast.LocalDecl node, final Void p) {
		final LocalDeclaration localDeclaration = new LocalDeclaration(node.getName().toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(localDeclaration, source, posHintOf(node));
		localDeclaration.modifiers = modifiersFor(node.getModifiers());
		localDeclaration.annotations = toArray(build(node.getAnnotations()), new Annotation[0]);
		localDeclaration.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		localDeclaration.type = build(node.getType());
		if (node.getInitialization() != null) {
			localDeclaration.initialization = build(node.getInitialization());
		}
		return localDeclaration;
	}

	@Override
	public ASTNode visitMethodDecl(final lombok.ast.MethodDecl node, final Void p) {
		MethodDeclaration methodDeclaration = new MethodDeclaration(((CompilationUnitDeclaration) sourceNode.top().get()).compilationResult);
		setGeneratedByAndCopyPos(methodDeclaration, source, posHintOf(node));
		methodDeclaration.modifiers = modifiersFor(node.getModifiers());
		methodDeclaration.returnType = build(node.getReturnType(), TypeReference.class);
		methodDeclaration.annotations = toArray(build(node.getAnnotations()), new Annotation[0]);
		methodDeclaration.selector = node.getName().toCharArray();
		methodDeclaration.thrownExceptions = toArray(build(node.getThrownExceptions()), new TypeReference[0]);
		methodDeclaration.typeParameters = toArray(build(node.getTypeParameters()), new TypeParameter[0]);
		methodDeclaration.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		methodDeclaration.arguments = toArray(build(node.getArguments()), new Argument[0]);
		if (node.isImplementing()) methodDeclaration.modifiers |= AccImplementing;
		if (node.noBody() || ((methodDeclaration.modifiers & AccAbstract) != 0)) {
			methodDeclaration.modifiers |= AccSemicolonBody;
		} else {
			methodDeclaration.statements = toArray(build(node.getStatements()), new Statement[0]);
		}
		return methodDeclaration;
	}

	@Override
	public ASTNode visitNameRef(final lombok.ast.NameRef node, final Void p) {
		final NameReference nameReference;
		if (node.getName().contains(".")) {
			char[][] nameTokens = fromQualifiedName(node.getName());
			nameReference = new QualifiedNameReference(nameTokens, poss(posHintOf(node), nameTokens.length), 0, 0);
		} else {
			nameReference = new SingleNameReference(node.getName().toCharArray(), 0);
		}
		setGeneratedByAndCopyPos(nameReference, source, posHintOf(node));
		return nameReference;
	}

	@Override
	public ASTNode visitNew(final lombok.ast.New node, final Void p) {
		final AllocationExpression allocationExpression;
		if (node.getAnonymousType() != null) {
			allocationExpression = new QualifiedAllocationExpression(build(node.getAnonymousType(), TypeDeclaration.class));
		} else {
			allocationExpression = new AllocationExpression();
		}
		setGeneratedByAndCopyPos(allocationExpression, source, posHintOf(node));
		allocationExpression.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		allocationExpression.type = build(node.getType());
		allocationExpression.typeArguments = toArray(build(node.getTypeArgs()), new TypeReference[0]);
		allocationExpression.arguments = toArray(build(node.getArgs()), new Expression[0]);
		return allocationExpression;
	}

	@Override
	public ASTNode visitNewArray(final lombok.ast.NewArray node, final Void p) {
		ArrayAllocationExpression allocationExpression = new ArrayAllocationExpression();
		setGeneratedByAndCopyPos(allocationExpression, source, posHintOf(node));
		allocationExpression.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		allocationExpression.type = build(node.getType());
		final List<Expression> dims = new ArrayList<Expression>();
		dims.addAll(build(node.getDimensionExpressions(), Expression.class));
		allocationExpression.dimensions = toArray(dims, new Expression[0]);
		final List<Expression> initializerExpressions = build(node.getInitializerExpressions(), Expression.class);
		if (!initializerExpressions.isEmpty()) {
			ArrayInitializer initializer = new ArrayInitializer();
			setGeneratedByAndCopyPos(initializer, source, posHintOf(node));
			initializer.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
			initializer.expressions = initializerExpressions.isEmpty() ? null : toArray(initializerExpressions, new Expression[0]);
			allocationExpression.initializer = initializer;
		}
		return allocationExpression;
	}

	@Override
	public ASTNode visitNullLiteral(final lombok.ast.NullLiteral node, final Void p) {
		final MagicLiteral literal = new NullLiteral(0, 0);
		setGeneratedByAndCopyPos(literal, source, posHintOf(node));
		return literal;
	}

	@Override
	public ASTNode visitNumberLiteral(final lombok.ast.NumberLiteral node, final Void p) {
		final NumberLiteral literal;
		final Number number = node.getNumber();
		if (number instanceof Integer) {
			literal = createIntLiteral(Integer.toString(number.intValue()).toCharArray());
		} else if (number instanceof Long) {
			literal = createLongLiteral((Long.toString(number.longValue()) + "L").toCharArray());
		} else if (number instanceof Float) {
			literal = new FloatLiteral((Float.toString(number.floatValue()) + "f").toCharArray(), 0, 0);
		} else {
			literal = new DoubleLiteral((Double.toString(number.doubleValue()) + "d").toCharArray(), 0, 0);
		}
		setGeneratedByAndCopyPos(literal, source, posHintOf(node));
		return literal;
	}

	private IntLiteral createIntLiteral(final char[] token) {
		IntLiteral result;
		try {
			if (Reflection.intLiteralConstructor != null) {
				result = Reflection.intLiteralConstructor.newInstance(token, 0, 0);
			} else {
				result = (IntLiteral) Reflection.intLiteralFactoryMethod.invoke(null, token, 0, 0);
			}
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
		return result;
	}

	private LongLiteral createLongLiteral(final char[] token) {
		LongLiteral result;
		try {
			if (Reflection.longLiteralConstructor != null) {
				result = Reflection.longLiteralConstructor.newInstance(token, 0, 0);
			} else {
				result = (LongLiteral) Reflection.longLiteralFactoryMethod.invoke(null, token, 0, 0);
			}
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
		return result;
	}

	@Override
	public ASTNode visitReturn(final lombok.ast.Return node, final Void p) {
		final ReturnStatement returnStatement = new ReturnStatement(node.getExpression() == null ? null : build(node.getExpression(), Expression.class), 0, 0);
		setGeneratedByAndCopyPos(returnStatement, source, posHintOf(node));
		return returnStatement;
	}

	@Override
	public ASTNode visitReturnDefault(final lombok.ast.ReturnDefault node, final Void p) {
		lombok.ast.Return returnDefault = Return(Null());
		lombok.ast.TypeRef returnType = node.upTo(lombok.ast.MethodDecl.class).getReturnType();
		if (returnType == null) {
			returnType = Type(methodNodeOf(sourceNode).getName());
		}
		final TypeReference type = build(returnType);
		if (type instanceof SingleTypeReference) {
			final String name = As.string(type.getLastToken());
			if ("int".equals(name)) {
				returnDefault = Return(Number(Integer.valueOf(0)));
			} else if ("byte".equals(name)) {
				returnDefault = Return(Number(Integer.valueOf(0)));
			} else if ("short".equals(name)) {
				returnDefault = Return(Number(Integer.valueOf(0)));
			} else if ("char".equals(name)) {
				returnDefault = Return(Char(""));
			} else if ("long".equals(name)) {
				returnDefault = Return(Number(Long.valueOf(0)));
			} else if ("float".equals(name)) {
				returnDefault = Return(Number(Float.valueOf(0)));
			} else if ("double".equals(name)) {
				returnDefault = Return(Number(Double.valueOf(0)));
			} else if ("boolean".equals(name)) {
				returnDefault = Return(False());
			} else if ("void".equals(name)) {
				returnDefault = Return();
			}
		}
		return build(returnDefault);
	}

	@Override
	public ASTNode visitStringLiteral(final lombok.ast.StringLiteral node, final Void p) {
		final StringLiteral stringLiteral = new StringLiteral(node.getString().toCharArray(), 0, 0, 1);
		setGeneratedByAndCopyPos(stringLiteral, source, posHintOf(node));
		return stringLiteral;
	}

	@Override
	public ASTNode visitSwitch(final lombok.ast.Switch node, final Void p) {
		final SwitchStatement switchStatement = new SwitchStatement();
		switchStatement.expression = build(node.getExpression());
		List<Statement> caseStatements = new ArrayList<Statement>();
		for (lombok.ast.Case caze : node.getCases()) {
			final CaseStatement caseStatement = new CaseStatement(caze.getPattern() == null ? null : build(caze.getPattern(), Expression.class), 0, 0);
			setGeneratedByAndCopyPos(caseStatement, source, posHintOf(node));
			caseStatements.add(caseStatement);
			caseStatements.addAll(build(caze.getStatements(), Statement.class));
		}
		switchStatement.statements = caseStatements.toArray(new Statement[caseStatements.size()]);
		return switchStatement;
	}

	@Override
	public ASTNode visitSynchronized(final lombok.ast.Synchronized node, final Void p) {
		final Block block = new Block(0);
		setGeneratedByAndCopyPos(block, source, posHintOf(node));
		block.statements = toArray(build(node.getStatements()), new Statement[0]);
		final SynchronizedStatement synchronizedStatemenet = new SynchronizedStatement(build(node.getLock(), Expression.class), block, 0, 0);
		setGeneratedByAndCopyPos(synchronizedStatemenet, source, posHintOf(node));
		return synchronizedStatemenet;
	}

	@Override
	public ASTNode visitThis(final lombok.ast.This node, final Void p) {
		final ThisReference thisReference;
		if (node.getType() != null) {
			thisReference = new QualifiedThisReference(build(node.getType(), TypeReference.class), 0, 0);
		} else {
			thisReference = new ThisReference(0, 0);
			if (node.isImplicit()) {
				thisReference.bits |= ASTNode.IsImplicitThis;
			}
		}
		setGeneratedByAndCopyPos(thisReference, source, posHintOf(node));
		return thisReference;
	}

	@Override
	public ASTNode visitThrow(final lombok.ast.Throw node, final Void p) {
		final ThrowStatement throwStatement = new ThrowStatement(build(node.getExpression(), Expression.class), 0, 0);
		setGeneratedByAndCopyPos(throwStatement, source, posHintOf(node));
		return throwStatement;
	}

	@Override
	public ASTNode visitTry(final lombok.ast.Try node, final Void p) {
		final TryStatement tryStatement = new TryStatement();
		setGeneratedByAndCopyPos(tryStatement, source, posHintOf(node));
		tryStatement.tryBlock = build(node.getTryBlock());
		tryStatement.catchArguments = toArray(build(node.getCatchArguments()), new Argument[0]);
		tryStatement.catchBlocks = toArray(build(node.getCatchBlocks()), new Block[0]);
		if (node.getFinallyBlock() != null) {
			tryStatement.finallyBlock = build(node.getFinallyBlock());
		}
		return tryStatement;
	}

	@Override
	public ASTNode visitTypeParam(final lombok.ast.TypeParam node, final Void p) {
		final TypeParameter typeParameter = new TypeParameter();
		typeParameter.name = node.getName().toCharArray();
		final List<lombok.ast.TypeRef> bounds = new ArrayList<lombok.ast.TypeRef>(node.getBounds());
		if (!bounds.isEmpty()) {
			typeParameter.type = build(bounds.get(0));
			bounds.remove(0);
			typeParameter.bounds = toArray(build(bounds), new TypeReference[0]);
		}
		setGeneratedByAndCopyPos(typeParameter, source, posHintOf(node));
		return typeParameter;
	}

	@Override
	public ASTNode visitTypeRef(final lombok.ast.TypeRef node, final Void p) {
		final TypeReference[] paramTypes = build(node.getTypeArgs()).toArray(new TypeReference[0]);
		final TypeReference typeReference;
		if (node.getTypeName().equals("void")) {
			typeReference = new SingleTypeReference(TypeBinding.VOID.simpleName, 0);
		} else if (node.getTypeName().contains(".")) {
			final char[][] typeNameTokens = fromQualifiedName(node.getTypeName());
			if (Is.notEmpty(paramTypes)) {
				final TypeReference[][] typeArguments = new TypeReference[typeNameTokens.length][];
				typeArguments[typeNameTokens.length - 1] = paramTypes;
				typeReference = new ParameterizedQualifiedTypeReference(typeNameTokens, typeArguments, 0, poss(posHintOf(node), typeNameTokens.length));
			} else {
				if (node.getDims() > 0) {
					typeReference = new ArrayQualifiedTypeReference(typeNameTokens, node.getDims(), poss(posHintOf(node), typeNameTokens.length));
				} else {
					typeReference = new QualifiedTypeReference(typeNameTokens, poss(posHintOf(node), typeNameTokens.length));
				}
			}
		} else {
			final char[] typeNameToken = node.getTypeName().toCharArray();
			if (Is.notEmpty(paramTypes)) {
				typeReference = new ParameterizedSingleTypeReference(typeNameToken, paramTypes, 0, 0);
			} else {
				if (node.getDims() > 0) {
					typeReference = new ArrayTypeReference(typeNameToken, node.getDims(), 0);
				} else {
					typeReference = new SingleTypeReference(typeNameToken, 0);
				}
			}
		}
		setGeneratedByAndCopyPos(typeReference, source, posHintOf(node));
		if (node.isSuperType()) typeReference.bits |= IsSuperType;
		return typeReference;
	}

	@Override
	public ASTNode visitUnary(final lombok.ast.Unary node, final Void p) {
		final String operator = node.getOperator();
		final int opCode;
		if ("!".equals(operator)) {
			opCode = OperatorIds.NOT;
		} else if ("+".equals(operator)) {
			opCode = OperatorIds.PLUS;
		} else if ("-".equals(operator)) {
			opCode = OperatorIds.MINUS;
		} else {
			throw new IllegalStateException(String.format("Unknown binary operator '%s'", operator));
		}
		final UnaryExpression unaryExpression = new UnaryExpression(build(node.getExpression(), Expression.class), opCode);
		setGeneratedByAndCopyPos(unaryExpression, source, posHintOf(node));
		return unaryExpression;
	}

	@Override
	public ASTNode visitWhile(final lombok.ast.While node, final Void p) {
		final WhileStatement whileStatement = new WhileStatement(build(node.getCondition(), Expression.class), build(node.getAction(), Statement.class), 0, 0);
		setGeneratedByAndCopyPos(whileStatement, source, posHintOf(node));
		return whileStatement;
	}

	@Override
	public ASTNode visitWildcard(final lombok.ast.Wildcard node, final Void p) {
		int kind = Wildcard.UNBOUND;
		if (node.getBound() != null) {
			switch(node.getBound()) {
			case SUPER:
				kind = Wildcard.SUPER;
				break;
			default:
			case EXTENDS:
				kind = Wildcard.EXTENDS;
			}
		}
		final Wildcard wildcard = new Wildcard(kind);
		setGeneratedByAndCopyPos(wildcard, source, posHintOf(node));
		wildcard.bound = build(node.getType());
		return wildcard;
	}

	@Override
	public ASTNode visitWrappedExpression(final lombok.ast.WrappedExpression node, final Void p) {
		Expression expression = (Expression) node.getWrappedObject();
		setGeneratedBy(expression, source);
		return expression;
	}

	@Override
	public ASTNode visitWrappedMethodDecl(final lombok.ast.WrappedMethodDecl node, final Void p) {
		MethodDeclaration methodDeclaration = new MethodDeclaration(((CompilationUnitDeclaration) sourceNode.top().get()).compilationResult);
		setGeneratedByAndCopyPos(methodDeclaration, source, posHintOf(node));
		MethodBinding abstractMethod = (MethodBinding) node.getWrappedObject();

		if (node.getReturnType() == null) {
			node.withReturnType(Type(abstractMethod.returnType));
		}
		if (node.getThrownExceptions().isEmpty()) for (ReferenceBinding thrownException : Each.elementIn(abstractMethod.thrownExceptions)) {
			node.withThrownException(Type(thrownException));
		}
		if (node.getArguments().isEmpty() && Is.notEmpty(abstractMethod.parameters)) for (int i = 0; i < abstractMethod.parameters.length; i++) {
			node.withArgument(Arg(Type(abstractMethod.parameters[i]), "arg" + i));
		}
		if (node.getTypeParameters().isEmpty()) for (TypeVariableBinding binding : Each.elementIn(abstractMethod.typeVariables)) {
			ReferenceBinding super1 = binding.superclass;
			ReferenceBinding[] super2 = binding.superInterfaces;
			lombok.ast.TypeParam typeParameter = TypeParam(As.string(binding.sourceName));
			if (super2 == null) super2 = new ReferenceBinding[0];
			if (super1 != null || super2.length > 0) {
				if (super1 != null) typeParameter.withBound(Type(super1));
				for (ReferenceBinding bound : super2) {
					typeParameter.withBound(Type(bound).makeSuperType());
				}
			}
			node.withTypeParameter(typeParameter);
		}

		methodDeclaration.modifiers = (abstractMethod.getAccessFlags() & (~AccAbstract));
		methodDeclaration.returnType = build(node.getReturnType(), TypeReference.class);
		methodDeclaration.annotations = toArray(build(node.getAnnotations()), new Annotation[0]);
		methodDeclaration.selector = abstractMethod.selector;
		methodDeclaration.thrownExceptions = toArray(build(node.getThrownExceptions()), new TypeReference[0]);
		methodDeclaration.typeParameters = toArray(build(node.getTypeParameters()), new TypeParameter[0]);
		methodDeclaration.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		methodDeclaration.arguments = toArray(build(node.getArguments()), new Argument[0]);
		if (node.isImplementing()) methodDeclaration.modifiers |= AccImplementing;
		if (node.noBody()) {
			methodDeclaration.modifiers |= AccSemicolonBody;
		} else {
			methodDeclaration.statements = toArray(build(node.getStatements()), new Statement[0]);
		}
		return methodDeclaration;
	}

	@Override
	public ASTNode visitWrappedStatement(final lombok.ast.WrappedStatement node, final Void p) {
		Statement statement = (Statement) node.getWrappedObject();
		setGeneratedBy(statement, source);
		return statement;
	}

	@Override
	public ASTNode visitWrappedTypeRef(final lombok.ast.WrappedTypeRef node, final Void p) {
		TypeReference typeReference = null;
		if (node.getWrappedObject() instanceof TypeBinding) {
			typeReference = makeType((TypeBinding) node.getWrappedObject(), source, false);
		} else if (node.getWrappedObject() instanceof TypeReference) {
			typeReference = copyType((TypeReference) node.getWrappedObject(), source);
		}
		setGeneratedBy(typeReference, source);
		if (node.isSuperType()) typeReference.bits |= IsSuperType;
		return typeReference;
	}

	// to support both eclipse 3.6 and eclipse 3.7+
	private static final class Reflection {
		public static final Constructor<CastExpression> castExpressionConstructor;
		public static final Constructor<IntLiteral> intLiteralConstructor;
		public static final Constructor<LongLiteral> longLiteralConstructor;
		public static final Method intLiteralFactoryMethod;
		public static final Method longLiteralFactoryMethod;
		
		static {
			Class<?>[] parameterTypes = {char[].class, int.class, int.class};
			Constructor<IntLiteral> intLiteralConstructor_ = null;
			Constructor<LongLiteral> longLiteralConstructor_ = null;
			Method intLiteralFactoryMethod_ = null;
			Method longLiteralFactoryMethod_ = null;
			try { 
				intLiteralConstructor_ = IntLiteral.class.getConstructor(parameterTypes);
				longLiteralConstructor_ = LongLiteral.class.getConstructor(parameterTypes);
			} catch (final Exception ignore) {
				// probably eclipse 3.7+
			}
			try { 
				intLiteralFactoryMethod_ = IntLiteral.class.getMethod("buildIntLiteral", parameterTypes);
				longLiteralFactoryMethod_ = LongLiteral.class.getMethod("buildLongLiteral", parameterTypes);
			} catch (final Exception ignore) {
				// probably eclipse versions before 3.7
			}
			castExpressionConstructor = Cast.uncheckedCast(CastExpression.class.getConstructors()[0]);
			intLiteralConstructor = intLiteralConstructor_;
			longLiteralConstructor = longLiteralConstructor_;
			intLiteralFactoryMethod = intLiteralFactoryMethod_;
			longLiteralFactoryMethod = longLiteralFactoryMethod_;
		}
	}
}