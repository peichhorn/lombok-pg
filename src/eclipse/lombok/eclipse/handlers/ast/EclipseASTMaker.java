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

import static org.eclipse.jdt.core.dom.Modifier.*;
import static org.eclipse.jdt.internal.compiler.ast.OperatorIds.EQUAL_EQUAL;
import static org.eclipse.jdt.internal.compiler.ast.OperatorIds.NOT_EQUAL;
import static org.eclipse.jdt.internal.compiler.ast.ASTNode.IsSuperType;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccAbstract;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccInterface;
import static org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers.AccSemicolonBody;
import static org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers.AccImplementing;

import static lombok.ast.AST.*;
import static lombok.core.util.Arrays.*;
import static lombok.core.util.Names.isEmpty;
import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.Eclipse.makeType;
import static lombok.eclipse.Eclipse.poss;
import static lombok.eclipse.Eclipse.setGeneratedBy;
import static lombok.eclipse.handlers.Eclipse.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;
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

import lombok.RequiredArgsConstructor;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseNode;

@RequiredArgsConstructor
public class EclipseASTMaker implements lombok.ast.ASTVisitor<ASTNode, Void> {
	private final EclipseNode sourceNode;
	private final ASTNode source;

	public <T extends ASTNode> T build(lombok.ast.Node node) {
		return this.<T>build(node, null);
	}

	@SuppressWarnings("unchecked")
	public <T extends ASTNode> T build(lombok.ast.Node node, Class<T> extectedType) {
		 if (node == null) return null;
		return (T) node.accept(this, null);
	}

	public <T extends ASTNode> List<T> build(List<? extends lombok.ast.Node> nodes) {
		return this.<T>build(nodes, null);
	}

	public <T extends ASTNode> List<T> build(List<? extends lombok.ast.Node> nodes, Class<T> extectedType) {
		if (nodes == null) return null;
		List<T> list = new ArrayList<T>();
		for (lombok.ast.Node node : nodes) {
			list.add(build(node, extectedType));
		}
		return list;
	}

	private int modifiersFor(Set<lombok.ast.Modifier> modifiers) {
		int mods = 0;
		mods |= modifiers.contains(lombok.ast.Modifier.PRIVATE) ? PRIVATE : 0;
		mods |= modifiers.contains(lombok.ast.Modifier.PROTECTED) ? PROTECTED : 0;
		mods |= modifiers.contains(lombok.ast.Modifier.PUBLIC) ? PUBLIC : 0;
		mods |= modifiers.contains(lombok.ast.Modifier.STATIC) ? STATIC : 0;
		mods |= modifiers.contains(lombok.ast.Modifier.FINAL) ? FINAL : 0;
		return mods;
	}

	private Statement getEmptyStatement() {
		final EmptyStatement emptyStatement = new EmptyStatement(0, 0);
		setGeneratedByAndCopyPos(emptyStatement, source);
		return emptyStatement;
	}

	private static <ELEMENT_TYPE> ELEMENT_TYPE[] toArray(final List<?> list, final ELEMENT_TYPE[] array) {
		if ((list != null) && !list.isEmpty()) {
			return list.toArray(array);
		}
		return null;
	}

	@Override
	public ASTNode visitAnnotation(lombok.ast.Annotation node, Void p) {
		final Annotation ann;
		if (node.getValues().isEmpty()) {
			ann = new MarkerAnnotation(build(node.getType(), TypeReference.class), 0);
		} else if (node.getValues().containsKey("value") && node.getValues().size() == 1) {
			ann = new SingleMemberAnnotation(build(node.getType(), TypeReference.class), 0);
			((SingleMemberAnnotation)ann).memberValue = build(node.getValues().get("value"));
		} else {
			ann = new NormalAnnotation(build(node.getType(), TypeReference.class), 0);
			List<MemberValuePair> valuePairs = new ArrayList<MemberValuePair>();
			for (Entry<String, lombok.ast.Expression> entry : node.getValues().entrySet()) {
				MemberValuePair valuePair = new MemberValuePair(entry.getKey().toCharArray(), 0, 0, build(entry.getValue(), Expression.class));
				setGeneratedByAndCopyPos(valuePair, source);
				valuePairs.add(valuePair);
			}
			((NormalAnnotation)ann).memberValuePairs = valuePairs.toArray(new MemberValuePair[0]);
		}
		setGeneratedByAndCopyPos(ann, source);
		return ann;
	}

	@Override
	public ASTNode visitArgument(lombok.ast.Argument node, Void p) {
		final Argument argument = new Argument(node.getName().toCharArray(), 0, null, 0);
		setGeneratedByAndCopyPos(argument, source);
		argument.modifiers = modifiersFor(node.getModifiers());
		argument.annotations = toArray(build(node.getAnnotations()), new Annotation[0]);
		argument.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		argument.type = build(node.getType());
		return argument;
	}

	@Override
	public ASTNode visitArrayRef(lombok.ast.ArrayRef node, Void p) {
		final ArrayReference arrayReference = new ArrayReference(build(node.getIndexed(), Expression.class), build(node.getIndex(), Expression.class));
		setGeneratedByAndCopyPos(arrayReference, source);
		return arrayReference;
	}

	@Override
	public ASTNode visitAssignment(lombok.ast.Assignment node, Void p) {
		final Assignment assignment = new Assignment(build(node.getLeft(), Expression.class), build(node.getRight(), Expression.class), 0);
		setGeneratedByAndCopyPos(assignment, source);
		return assignment;
	}

	@Override
	public ASTNode visitBinary(lombok.ast.Binary node, Void p) {
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
		} else {
			opCode = 0;
		}
		BinaryExpression binaryExpression = new BinaryExpression(build(node.getLeft(), Expression.class), build(node.getRight(), Expression.class), opCode);
		setGeneratedByAndCopyPos(binaryExpression, source);
		return binaryExpression;
	}

	@Override
	public ASTNode visitBlock(lombok.ast.Block node, Void p) {
		final Block block = new Block(0);
		setGeneratedByAndCopyPos(block, source);
		block.statements = toArray(build(node.getStatements()), new Statement[0]);
		return block;
	}

	@Override
	public ASTNode visitBooleanLiteral(lombok.ast.BooleanLiteral node, Void p) {
		final MagicLiteral literal;
		if (node.isTrue()) {
			literal = new TrueLiteral(0, 0);
		} else {
			literal = new FalseLiteral(0, 0);
		}
		setGeneratedByAndCopyPos(literal, source);
		return literal;
	}

	@Override
	public ASTNode visitCall(lombok.ast.Call node, Void p) {
		final MessageSend messageSend = new MessageSend();
		setGeneratedByAndCopyPos(messageSend, source);
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
	public ASTNode visitCast(lombok.ast.Cast node, Void p) {
		final CastExpression castExpression = new CastExpression(build(node.getExpression(), Expression.class), build(node.getType(), TypeReference.class));
		setGeneratedByAndCopyPos(castExpression, source);
		return castExpression;
	}

	@Override
	public ASTNode visitCase(lombok.ast.Case node, Void p) {
		return null;
	}

	@Override
	public ASTNode visitCharLiteral(lombok.ast.CharLiteral node, Void p) {
		final CharLiteral literal = new CharLiteral(node.getCharacter().toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(literal, source);
		return literal;
	}

	@Override
	public ASTNode visitClassDecl(lombok.ast.ClassDecl node, Void p) {
		final TypeDeclaration typeDeclaration = new TypeDeclaration(((CompilationUnitDeclaration) sourceNode.top().get()).compilationResult);
		setGeneratedByAndCopyPos(typeDeclaration, source);
		typeDeclaration.modifiers = modifiersFor(node.getModifiers());
		if (node.isInterface()) typeDeclaration.modifiers |= AccInterface;
		typeDeclaration.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		if (node.isLocal()) typeDeclaration.bits |= ASTNode.IsLocalType;
		if (node.isAnonymous()) {
			typeDeclaration.bits |= ASTNode.IsAnonymousType;
			typeDeclaration.sourceEnd = 0;
			typeDeclaration.bodyEnd = source.sourceEnd + 2;
		}
		if (isEmpty(node.getName())) {
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
	public ASTNode visitConstructorDecl(lombok.ast.ConstructorDecl node, Void p) {
		final ConstructorDeclaration constructorDeclaration = new ConstructorDeclaration(((CompilationUnitDeclaration) sourceNode.top().get()).compilationResult);
		setGeneratedByAndCopyPos(constructorDeclaration, source);
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
	public ASTNode visitContinue(lombok.ast.Continue node, Void p) {
		final ContinueStatement continueStatement = new ContinueStatement(node.getLabel() == null ? null : node.getLabel().toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(continueStatement, source);
		return continueStatement;
	}

	@Override
	public ASTNode visitDoWhile(lombok.ast.DoWhile node, Void p) {
		final DoStatement doStatement = new DoStatement(build(node.getCondition(), Expression.class), build(node.getAction(), Statement.class), 0, 0);
		setGeneratedByAndCopyPos(doStatement, source);
		return doStatement;
	}

	@Override
	public ASTNode visitEnumConstant(lombok.ast.EnumConstant node, Void p) {
		final AllocationExpression allocationExpression = new AllocationExpression();
		setGeneratedByAndCopyPos(allocationExpression, source);
		allocationExpression.arguments = toArray(build(node.getArgs()), new Expression[0]);
		allocationExpression.enumConstant = new FieldDeclaration(node.getName().toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(allocationExpression.enumConstant, source);
		allocationExpression.enumConstant.initialization = allocationExpression;
		return allocationExpression.enumConstant;
	}

	@Override
	public ASTNode visitEqual(lombok.ast.Equal node, Void p) {
		final EqualExpression equalExpression = new EqualExpression(build(node.getLeft(), Expression.class), build(node.getRight(), Expression.class), node.isNotEqual() ? NOT_EQUAL : EQUAL_EQUAL);
		setGeneratedByAndCopyPos(equalExpression, source);
		return equalExpression;
	}

	@Override
	public ASTNode visitFieldDecl(lombok.ast.FieldDecl node, Void p) {
		final FieldDeclaration fieldDeclaration = new FieldDeclaration(node.getName().toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(fieldDeclaration, source);
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
	public ASTNode visitFieldRef(lombok.ast.FieldRef node, Void p) {
		FieldReference fieldRef = new FieldReference(node.getName().toCharArray(), 0);
		fieldRef.receiver = build(node.getReceiver());
		setGeneratedByAndCopyPos(fieldRef, source);
		return fieldRef;
	}

	@Override
	public ASTNode visitForeach(lombok.ast.Foreach node, Void p) {
		final ForeachStatement forEach = new ForeachStatement(build(node.getElementVariable(), LocalDeclaration.class), 0);
		setGeneratedByAndCopyPos(forEach, source);
		forEach.collection = build(node.getCollection());
		forEach.action = build(node.getAction());
		return forEach;
	}

	@Override
	public ASTNode visitIf(lombok.ast.If node, Void p) {
		final IfStatement ifStatement = new IfStatement(build(node.getCondition(), Expression.class), node.getThenStatement() == null ? getEmptyStatement() : build(node.getThenStatement(), Statement.class), 0, 0);
		if (node.getElseStatement() != null) {
			ifStatement.elseStatement = build(node.getElseStatement());
		}
		setGeneratedByAndCopyPos(ifStatement, source);
		return ifStatement;
	}

	@Override
	public ASTNode visitInstanceOf(lombok.ast.InstanceOf node, Void p) {
		final InstanceOfExpression instanceOfExpression = new InstanceOfExpression(build(node.getExpression(), Expression.class), build(node.getType(), TypeReference.class));
		setGeneratedByAndCopyPos(instanceOfExpression, source);
		return instanceOfExpression;
	}

	@Override
	public ASTNode visitLocalDecl(lombok.ast.LocalDecl node, Void p) {
		final LocalDeclaration localDeclaration = new LocalDeclaration(node.getName().toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(localDeclaration, source);
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
	public ASTNode visitMethodDecl(lombok.ast.MethodDecl node, Void p) {
		MethodDeclaration methodDeclaration = new MethodDeclaration(((CompilationUnitDeclaration) sourceNode.top().get()).compilationResult);
		setGeneratedByAndCopyPos(methodDeclaration, source);
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
	public ASTNode visitNameRef(lombok.ast.NameRef node, Void p) {
		final NameReference nameReference;
		if (node.getName().contains(".")) {
			char[][] nameTokens = fromQualifiedName(node.getName());
			nameReference = new QualifiedNameReference(nameTokens, poss(source, nameTokens.length), 0, 0);
		} else {
			nameReference = new SingleNameReference(node.getName().toCharArray(), 0);
		}
		setGeneratedByAndCopyPos(nameReference, source);
		return nameReference;
	}

	@Override
	public ASTNode visitNew(lombok.ast.New node, Void p) {
		final AllocationExpression allocationExpression;
		if (node.getAnonymousType() != null) {
			allocationExpression = new QualifiedAllocationExpression(build(node.getAnonymousType(), TypeDeclaration.class));
		} else {
			allocationExpression = new AllocationExpression();
		}
		setGeneratedByAndCopyPos(allocationExpression, source);
		allocationExpression.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		allocationExpression.type = build(node.getType());
		allocationExpression.typeArguments = toArray(build(node.getTypeArgs()), new TypeReference[0]);
		allocationExpression.arguments = toArray(build(node.getArgs()), new Expression[0]);
		return allocationExpression;
	}

	@Override
	public ASTNode visitNewArray(lombok.ast.NewArray node, Void p) {
		ArrayAllocationExpression allocationExpression = new ArrayAllocationExpression();
		setGeneratedByAndCopyPos(allocationExpression, source);
		allocationExpression.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		allocationExpression.type = build(node.getType());
		allocationExpression.dimensions = resize(toArray(build(node.getDimensionExpressions()), new Expression[0]), node.getDimensions() - node.getDimensionExpressions().size());
		if (!node.getInitializerExpressions().isEmpty()) {
			ArrayInitializer initializer = new ArrayInitializer();
			setGeneratedByAndCopyPos(initializer, source);
			initializer.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
			initializer.expressions = toArray(build(node.getInitializerExpressions()), new Expression[0]);
			allocationExpression.initializer = initializer;
		}
		return allocationExpression;
	}

	@Override
	public ASTNode visitNullLiteral(lombok.ast.NullLiteral node, Void p) {
		final MagicLiteral literal = new NullLiteral(0, 0);
		setGeneratedByAndCopyPos(literal, source);
		return literal;
	}

	@Override
	public ASTNode visitNumberLiteral(lombok.ast.NumberLiteral node, Void p) {
		final NumberLiteral literal;
		final Number number = node.getNumber();
		if (number instanceof Integer) {
			literal = new IntLiteral(Integer.toString(number.intValue()).toCharArray(), 0, 0);
		} else if (number instanceof Long) {
			literal = new LongLiteral((Long.toString(number.longValue()) + "L").toCharArray(), 0, 0);
		} else if (number instanceof Float) {
			literal = new FloatLiteral((Float.toString(number.floatValue()) + "f").toCharArray(), 0, 0);
		} else {
			literal = new DoubleLiteral((Double.toString(number.doubleValue()) + "d").toCharArray(), 0, 0);
		}
		setGeneratedByAndCopyPos(literal, source);
		return literal;
	}

	@Override
	public ASTNode visitReturn(lombok.ast.Return node, Void p) {
		final ReturnStatement returnStatement = new ReturnStatement(node.getExpression() == null ? null : build(node.getExpression(), Expression.class), 0, 0);
		setGeneratedByAndCopyPos(returnStatement, source);
		return returnStatement;
	}

	@Override
	public ASTNode visitReturnDefault(lombok.ast.ReturnDefault node, Void p) {
		lombok.ast.Return returnDefault = Return(Null());
		lombok.ast.TypeRef returnType = node.upTo(lombok.ast.MethodDecl.class).getReturnType();
		if (returnType == null) {
			returnType = Type(methodNodeOf(sourceNode).getName());
		}
		final TypeReference type = build(returnType);
		if (type instanceof SingleTypeReference) {
			final String name = new String(type.getLastToken());
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
	public ASTNode visitStringLiteral(lombok.ast.StringLiteral node, Void p) {
		final StringLiteral stringLiteral = new StringLiteral(node.getString().toCharArray(), 0, 0, 1);
		setGeneratedByAndCopyPos(stringLiteral, source);
		return stringLiteral;
	}

	@Override
	public ASTNode visitSwitch(lombok.ast.Switch node, Void p) {
		final SwitchStatement switchStatement = new SwitchStatement();
		switchStatement.expression = build(node.getExpression());
		List<Statement> caseStatements = new ArrayList<Statement>();
		for (lombok.ast.Case caze : node.getCases()) {
			final CaseStatement caseStatement = new CaseStatement(caze.getPattern() == null ? null : build(caze.getPattern(), Expression.class), 0, 0);
			setGeneratedByAndCopyPos(caseStatement, source);
			caseStatements.add(caseStatement);
			caseStatements.addAll(build(caze.getStatements(), Statement.class));
		}
		switchStatement.statements = caseStatements.toArray(new Statement[caseStatements.size()]);
		return switchStatement;
	}

	@Override
	public ASTNode visitThis(lombok.ast.This node, Void p) {
		final ThisReference thisReference;
		if (node.getType() != null) {
			thisReference = new QualifiedThisReference(build(node.getType(), TypeReference.class), 0, 0);
		} else {
			thisReference = new ThisReference(0, 0);
			if (node.isImplicit()) {
				thisReference.bits |= ASTNode.IsImplicitThis;
			}
		}
		setGeneratedByAndCopyPos(thisReference, source);
		return thisReference;
	}

	@Override
	public ASTNode visitThrow(lombok.ast.Throw node, Void p) {
		final ThrowStatement throwStatement = new ThrowStatement(build(node.getExpression(), Expression.class), 0, 0);
		setGeneratedByAndCopyPos(throwStatement, source);
		return throwStatement;
	}

	@Override
	public ASTNode visitTry(lombok.ast.Try node, Void p) {
		final TryStatement tryStatement = new TryStatement();
		setGeneratedByAndCopyPos(tryStatement, source);
		tryStatement.tryBlock = build(node.getTryBlock());
		tryStatement.catchArguments = toArray(build(node.getCatchArguments()), new Argument[0]);
		tryStatement.catchBlocks = toArray(build(node.getCatchBlocks()), new Block[0]);
		if (node.getFinallyBlock() != null) {
			tryStatement.finallyBlock = build(node.getFinallyBlock());
		}
		return tryStatement;
	}

	@Override
	public ASTNode visitTypeParam(lombok.ast.TypeParam node, Void p) {
		final TypeParameter typeParameter = new TypeParameter();
		typeParameter.name = node.getName().toCharArray();
		final List<lombok.ast.TypeRef> bounds = new ArrayList<lombok.ast.TypeRef>(node.getBounds());
		if (!bounds.isEmpty()) {
			typeParameter.type = build(bounds.get(0));
			bounds.remove(0);
			typeParameter.bounds = toArray(build(bounds), new TypeReference[0]);
		}
		setGeneratedByAndCopyPos(typeParameter, source);
		return typeParameter;
	}

	@Override
	public ASTNode visitTypeRef(lombok.ast.TypeRef node, Void p) {
		final TypeReference[] paramTypes = build(node.getTypeArgs()).toArray(new TypeReference[0]);
		final TypeReference typeReference;
		if (node.getTypeName().equals("void")) {
			typeReference = new SingleTypeReference(TypeBinding.VOID.simpleName, 0);
		} else if (node.getTypeName().contains(".")) {
			final char[][] typeNameTokens = fromQualifiedName(node.getTypeName());
			if (isNotEmpty(paramTypes)) {
				final TypeReference[][] typeArguments = new TypeReference[typeNameTokens.length][];
				typeArguments[typeNameTokens.length - 1] = paramTypes;
				typeReference = new ParameterizedQualifiedTypeReference(typeNameTokens, typeArguments, 0, poss(source, typeNameTokens.length));
			} else {
				if (node.getDims() > 0) {
					typeReference = new ArrayQualifiedTypeReference(typeNameTokens, node.getDims(), poss(source, typeNameTokens.length));
				} else {
					typeReference = new QualifiedTypeReference(typeNameTokens, poss(source, typeNameTokens.length));
				}
			}
		} else {
			final char[] typeNameToken = node.getTypeName().toCharArray();
			if (isNotEmpty(paramTypes)) {
				typeReference = new ParameterizedSingleTypeReference(typeNameToken, paramTypes, 0, 0);
			} else {
				if (node.getDims() > 0) {
					typeReference = new ArrayTypeReference(typeNameToken, node.getDims(), 0);
				} else {
					typeReference = new SingleTypeReference(typeNameToken, 0);
				}
			}
		}
		setGeneratedByAndCopyPos(typeReference, source);
		if (node.isSuperType()) typeReference.bits |= IsSuperType;
		return typeReference;
	}

	@Override
	public ASTNode visitUnary(lombok.ast.Unary node, Void p) {
		final int opCode;
		if ("!".equals(node.getOperator())) {
			opCode = OperatorIds.NOT;
		} else if ("+".equals(node.getOperator())) {
			opCode = OperatorIds.PLUS;
		} else {
			opCode = 0;
		}
		final UnaryExpression unaryExpression = new UnaryExpression(build(node.getExpression(), Expression.class), opCode);
		setGeneratedByAndCopyPos(unaryExpression, source);
		return unaryExpression;
	}

	@Override
	public ASTNode visitWhile(lombok.ast.While node, Void p) {
		final WhileStatement whileStatement = new WhileStatement(build(node.getCondition(), Expression.class), build(node.getAction(), Statement.class), 0, 0);
		setGeneratedByAndCopyPos(whileStatement, source);
		return whileStatement;
	}

	@Override
	public ASTNode visitWildcard(lombok.ast.Wildcard node, Void p) {
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
		setGeneratedByAndCopyPos(wildcard, source);
		wildcard.bound = build(node.getType());
		return wildcard;
	}

	@Override
	public ASTNode visitWrappedExpression(lombok.ast.WrappedExpression node, Void p) {
		Expression expression = (Expression) node.getWrappedObject();
		setGeneratedBy(expression, source);
		return expression;
	}

	@Override
	public ASTNode visitWrappedMethodDecl(lombok.ast.WrappedMethodDecl node, Void p) {
		MethodDeclaration methodDeclaration = new MethodDeclaration(((CompilationUnitDeclaration) sourceNode.top().get()).compilationResult);
		setGeneratedByAndCopyPos(methodDeclaration, source);
		MethodBinding abstractMethod = (MethodBinding) node.getWrappedObject();

		if (node.getReturnType() == null) {
			node.withReturnType(Type(abstractMethod.returnType));
		}
		if (node.getThrownExceptions().isEmpty() && isNotEmpty(abstractMethod.thrownExceptions)) for (int i = 0; i < abstractMethod.thrownExceptions.length; i++) {
			node.withThrownException(Type(abstractMethod.thrownExceptions[i]));
		}
		if (node.getArguments().isEmpty() && isNotEmpty(abstractMethod.parameters)) for (int i = 0; i < abstractMethod.parameters.length; i++) {
			node.withArgument(Arg(Type(abstractMethod.parameters[i]), "arg" + i));
		}
		if (node.getTypeParameters().isEmpty() && isNotEmpty(abstractMethod.typeVariables)) for (int i = 0; i < abstractMethod.typeVariables.length; i++) {
			TypeVariableBinding binding = abstractMethod.typeVariables[i];
			ReferenceBinding super1 = binding.superclass;
			ReferenceBinding[] super2 = binding.superInterfaces;
			lombok.ast.TypeParam typeParameter = TypeParam(new String(binding.sourceName));
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
	public ASTNode visitWrappedStatement(lombok.ast.WrappedStatement node, Void p) {
		Statement statement = (Statement) node.getWrappedObject();
		setGeneratedBy(statement, source);
		return statement;
	}

	@Override
	public ASTNode visitWrappedTypeRef(lombok.ast.WrappedTypeRef node, Void p) {
		TypeReference typeReference = null;
		if (node.getWrappedObject() instanceof TypeBinding) {
			typeReference = makeType((TypeBinding) node.getWrappedObject(), source, false);
		} else if (node.getWrappedObject() instanceof TypeReference) {
			typeReference = Eclipse.copyType((TypeReference) node.getWrappedObject(), source);
		}
		setGeneratedBy(typeReference, source);
		if (node.isSuperType()) typeReference.bits |= IsSuperType;
		return typeReference;
	}
}