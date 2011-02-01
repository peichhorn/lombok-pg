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
package lombok.eclipse.handlers;

import static lombok.core.util.Arrays.*;
import static lombok.eclipse.Eclipse.*;
import static org.eclipse.jdt.core.dom.Modifier.*;
import static org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;

import java.util.ArrayList;
import java.util.List;

import lombok.core.AST.Kind;
import lombok.core.util.Cast;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.EqualExpression;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.Literal;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

//TODO add features if required
public class EclipseNodeBuilder {
	private EclipseNodeBuilder() {
		//Prevent instantiation
	}

	public static void setGeneratedByAndCopyPos(ASTNode target, ASTNode source) {
		setGeneratedBy(target, source);
		copyPosTo(target, source);
	}

	public static void copyPosTo(ASTNode target, ASTNode source) {
		target.sourceStart = source.sourceStart;
		target.sourceEnd = source.sourceEnd;
		if (target instanceof AbstractMethodDeclaration) {
			((AbstractMethodDeclaration)target).bodyStart = source.sourceStart;
			((AbstractMethodDeclaration)target).bodyEnd = source.sourceEnd;
		} else if (target instanceof TypeDeclaration) {
			((TypeDeclaration)target).bodyStart = source.sourceStart;
			((TypeDeclaration)target).bodyEnd = source.sourceEnd;
		} else if (target instanceof AbstractVariableDeclaration) {
			target.sourceStart = target.sourceEnd = 0;
			((AbstractVariableDeclaration)target).declarationSourceEnd  = -1;
		}
		if (target instanceof Expression) {
			((Expression)target).statementEnd = source.sourceEnd;
		}
		if (target instanceof Annotation) {
			((Annotation)target).declarationSourceEnd = source.sourceEnd;
		}
	}

	public static Assignment assignment(ASTNode source, Expression left, Expression right) {
		Assignment assignment = new Assignment(left, right, 0);
		setGeneratedByAndCopyPos(assignment, source);
		return assignment;
	}

	public static Assignment assignment(ASTNode source, String leftName, Expression right) {
		Assignment assignment = new Assignment(nameReference(source, leftName), right, 0);
		setGeneratedByAndCopyPos(assignment, source);
		return assignment;
	}

	public static Assignment assignment(ASTNode source, String leftName, String rightName) {
		Assignment assignment = new Assignment(nameReference(source, leftName), nameReference(source, rightName), 0);
		setGeneratedByAndCopyPos(assignment, source);
		return assignment;
	}

	public static ThrowStatement throwNewException(ASTNode source, String typeName, Expression... args) {
		return throwNewException(source, typeReference(source, typeName), args);
	}

	public static ThrowStatement throwNewException(ASTNode source, TypeReference type, Expression... args) {
		AllocationExpression initException = new AllocationExpression();
		setGeneratedByAndCopyPos(initException, source);
		initException.type = type;
		initException.arguments = args;
		ThrowStatement throwStatement = new ThrowStatement(initException, 0, 0);
		setGeneratedByAndCopyPos(throwStatement, source);
		return throwStatement;
	}

	public static WhileStatement whileStatement(ASTNode source, Expression condition, Statement action) {
		WhileStatement whileStatement = new WhileStatement(condition, action, 0, 0);
		setGeneratedByAndCopyPos(whileStatement, source);
		return whileStatement;
	}

	public static Block block(ASTNode source, Statement... statements) {
		Block block = new Block(0);
		setGeneratedByAndCopyPos(block, source);
		block.statements = statements;
		return block;
	}

	public static IfStatement ifStatement(ASTNode source, Expression condition, Statement then) {
		IfStatement ifStatement = new IfStatement(condition, then, 0, 0);
		setGeneratedByAndCopyPos(ifStatement, source);
		return ifStatement;
	}

	public static IfStatement ifStatement(ASTNode source, Expression condition, Statement then, Statement el$e) {
		IfStatement ifStatement = new IfStatement(condition, then, el$e, 0, 0);
		setGeneratedByAndCopyPos(ifStatement, source);
		return ifStatement;
	}

	public static IfStatement ifNotStatement(ASTNode source, Expression condition, Statement then) {
		UnaryExpression newCondition = new UnaryExpression(condition, OperatorIds.NOT);
		setGeneratedByAndCopyPos(newCondition, source);
		return ifStatement(source, newCondition, then);
	}

	public static ReturnStatement returnStatement(ASTNode source, boolean b) {
		ReturnStatement returnStatement = new ReturnStatement(booleanLiteral(source, b), 0, 0);
		setGeneratedByAndCopyPos(returnStatement, source);
		return returnStatement;
	}

	public static ReturnStatement returnStatement(ASTNode source, Expression expr) {
		ReturnStatement returnStatement = new ReturnStatement(expr, 0, 0);
		setGeneratedByAndCopyPos(returnStatement, source);
		return returnStatement;
	}

	public static Argument argument(ASTNode source, TypeReference type, String argumentName) {
		Argument arg = new Argument(argumentName.toCharArray(), 0, copyType(type, source), FINAL);
		setGeneratedByAndCopyPos(arg, source);
		return arg;
	}

	public static Argument argument(ASTNode source, String typeName, String argumentName) {
		return argument(source, typeReference(source, typeName), argumentName);
	}

	public static EqualExpression equal(ASTNode source, Expression left, Expression right) {
		EqualExpression equalExpression = new EqualExpression(left, right, OperatorIds.EQUAL_EQUAL);
		setGeneratedByAndCopyPos(equalExpression, source);
		return equalExpression;
	}

	public static EqualExpression notEqual(ASTNode source, Expression left, Expression right) {
		EqualExpression equalExpression = new EqualExpression(left, right, OperatorIds.NOT_EQUAL);
		setGeneratedByAndCopyPos(equalExpression, source);
		return equalExpression;
	}

	public static Annotation annotation(ASTNode source, String typeName) {
		TypeReference typeRef = typeReference(source, typeName);
		MarkerAnnotation ann = new MarkerAnnotation(typeRef, 0);
		setGeneratedByAndCopyPos(ann, source);
		return ann;
	}

	public static Annotation annotation(ASTNode source, String typeName, String value) {
		TypeReference typeRef = typeReference(source, typeName);
		SingleMemberAnnotation ann = new SingleMemberAnnotation(typeRef, 0);
		ann.memberValue = new StringLiteral(value.toCharArray(), 0, 0, 0);
		setGeneratedByAndCopyPos(ann, source);
		return ann;
	}

	public static Literal booleanLiteral(ASTNode source, boolean b) {
		Literal literal;
		if (b) {
			literal = new TrueLiteral(0, 0);
		} else {
			literal = new FalseLiteral(0, 0);
		}
		setGeneratedByAndCopyPos(literal, source);
		return literal;
	}

	public static Literal intLiteral(ASTNode source, int value) {
		Literal literal = new IntLiteral(String.valueOf(value).toCharArray(), 0, 0, value);
		setGeneratedByAndCopyPos(literal, source);
		return literal;
	}

	public static Literal nullLiteral(ASTNode source) {
		Literal literal = new NullLiteral(0, 0);
		setGeneratedByAndCopyPos(literal, source);
		return literal;
	}

	public static FieldReference fieldReference(ASTNode source, Expression receiver, String fieldName) {
		FieldReference fieldRef = new FieldReference(fieldName.toCharArray(), 0);
		fieldRef.receiver = receiver;
		setGeneratedByAndCopyPos(fieldRef, source);
		return fieldRef;
	}

	public static NameReference nameReference(ASTNode source, String name) {
		NameReference nameReference;
		if (name.contains(".")) {
			char[][] nameTokens = fromQualifiedName(name);
			nameReference = new QualifiedNameReference(nameTokens, poss(source, nameTokens.length), 0, 0);
		} else {
			nameReference = new SingleNameReference(name.toCharArray(), 0);
		}
		setGeneratedByAndCopyPos(nameReference, source);

		return nameReference;
	}

	public static TypeReference typeReference(ASTNode source, String typeName, String firstParamTypeName, String... paramTypeNames) {
		TypeReference[] paramTypes = new TypeReference[paramTypeNames == null ? 1 : paramTypeNames.length + 1];
		paramTypes[0] = typeReference(source, firstParamTypeName);
		if (paramTypeNames != null) for (int i = 0; i < paramTypeNames.length; i++) {
			paramTypes[i + 1] = typeReference(source, paramTypeNames[i]);
		}
		return typeReference(source, typeName, paramTypes);
	}

	public static TypeReference typeReference(ASTNode source, String typeName, TypeReference... paramTypes) {
		TypeReference typeReference;
		int arrayDimensions = 0;
		while(typeName.endsWith("[]")) {
			arrayDimensions++;
			typeName = typeName.substring(0, typeName.length() - 2);
		}
		if (typeName.equals("void")) {
			return new SingleTypeReference(TypeBinding.VOID.simpleName, 0);
		} else if (typeName.contains(".")) {
			char[][] typeNameTokens = fromQualifiedName(typeName);
			if (isNotEmpty(paramTypes)) {
				TypeReference[][] typeArguments = new TypeReference[typeNameTokens.length][];
				typeArguments[typeNameTokens.length - 1] = paramTypes;
				typeReference = new ParameterizedQualifiedTypeReference(typeNameTokens, typeArguments, 0, poss(source, typeNameTokens.length));
			} else {
				if (arrayDimensions > 0) {
					typeReference = new ArrayQualifiedTypeReference(typeNameTokens, arrayDimensions, poss(source, typeNameTokens.length));
				} else {
					typeReference = new QualifiedTypeReference(typeNameTokens, poss(source, typeNameTokens.length));
				}
			}
		} else {
			char[] typeNameToken = typeName.toCharArray();
			if (isNotEmpty(paramTypes)) {
				typeReference = new ParameterizedSingleTypeReference(typeNameToken, paramTypes, 0, 0);
			} else {
				if (arrayDimensions > 0) {
					typeReference = new ArrayTypeReference(typeNameToken, arrayDimensions, 0);
				} else {
					typeReference = new SingleTypeReference(typeNameToken, 0);
				}
			}
		}

		setGeneratedByAndCopyPos(typeReference, source);
		return typeReference;
	}

	public static MessageSend methodCall(ASTNode source, Expression receiver, String method, Expression... args) {
		MessageSend methodCall = new MessageSend();
		setGeneratedByAndCopyPos(methodCall, source);
		if ((args != null) && (args.length > 0)) {
			methodCall.arguments = args;
		}
		methodCall.receiver = receiver;
		methodCall.selector = method.toCharArray();
		return methodCall;
	}

	public static MessageSend methodCall(ASTNode source, String receiver, String method, Expression... args) {
		return methodCall(source, nameReference(source, receiver), method, args);
	}

	public static MessageSend methodCall(ASTNode source, String method, Expression... args) {
		ThisReference thisReference = thisReference(source);
		thisReference.bits |= ASTNode.IsImplicitThis;
		return methodCall(source, thisReference, method, args);
	}

	public static ThisReference thisReference(ASTNode source) {
		ThisReference ref = new ThisReference(0, 0);
		setGeneratedByAndCopyPos(ref, source);
		return ref;
	}

	public static QualifiedThisReference thisReference(ASTNode source, TypeReference typeReference) {
		QualifiedThisReference qualThisRef = new QualifiedThisReference(typeReference, 0, 0);
		setGeneratedByAndCopyPos(qualThisRef, source);
		return qualThisRef;
	}

	public static ConstructorBuilder constructor(EclipseNode node, ASTNode source, int modifiers, String typeName) {
		return new ConstructorBuilder(node, source, modifiers, typeName);
	}

	public static MethodBuilder method(EclipseNode node, ASTNode source, int modifiers, String returnTypeName, String methodName) {
		return new MethodBuilder(node, source, modifiers, typeReference(source, returnTypeName), methodName);
	}

	public static MethodBuilder method(EclipseNode node, ASTNode source, int modifiers, TypeReference returnType, String methodName) {
		return new MethodBuilder(node, source, modifiers, returnType, methodName);
	}

	public static ClassBuilder clazz(EclipseNode node, ASTNode source, int modifiers, String typeName) {
		return new ClassBuilder(node, source, modifiers, typeName);
	}

	public static ClassBuilder interfaze(EclipseNode node, ASTNode source, int modifiers, String typeName) {
		return new ClassBuilder(node, source, modifiers | AccInterface, typeName);
	}

	public static FieldBuilder field(EclipseNode node, ASTNode source, int modifiers, String typeName, String fieldName) {
		return new FieldBuilder(node, source, modifiers, typeReference(source, typeName), fieldName);
	}

	public static FieldBuilder field(EclipseNode node, ASTNode source, int modifiers, TypeReference type, String fieldName) {
		return new FieldBuilder(node, source, modifiers, type, fieldName);
	}

	public static LocalBuilder local(EclipseNode node, ASTNode source, int modifiers, String typeName, String fieldName) {
		return new LocalBuilder(node, source, modifiers, typeReference(source, typeName), fieldName);
	}

	public static LocalBuilder local(EclipseNode node, ASTNode source, int modifiers, TypeReference type, String fieldName) {
		return new LocalBuilder(node, source, modifiers, type, fieldName);
	}

	public static class ClassBuilder extends AbstractNodeBuilder<ClassBuilder, TypeDeclaration> {
		protected List<TypeParameter> typeParameters = new ArrayList<TypeParameter>();
		protected List<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();
		protected List<AbstractMethodDeclaration> methods = new ArrayList<AbstractMethodDeclaration>();
		protected List<TypeDeclaration> memberTypes = new ArrayList<TypeDeclaration>();
		protected TypeReference superclass;
		protected List<TypeReference> superInterfaces = new ArrayList<TypeReference>();

		public ClassBuilder(EclipseNode node, ASTNode source, int modifiers, String typeName) {
			super(node, source, modifiers, typeName);
		}

		public ClassBuilder implementing(TypeReference type) {
			this.superInterfaces.add(type);
			return self();
		}

		public ClassBuilder implementing(List<String> interfazes) {
			for (String typeName : interfazes) {
				this.superInterfaces.add(typeReference(source, typeName));
			}
			return self();
		}

		public ClassBuilder withMethods(List<AbstractMethodDeclaration> methods) {
			this.methods.addAll(methods);
			return self();
		}

		public ClassBuilder withMethod(AbstractMethodDeclaration method) {
			this.methods.add(method);
			return self();
		}

		public ClassBuilder withFields(List<FieldDeclaration> fields) {
			this.fields.addAll(fields);
			return self();
		}

		public ClassBuilder withField(FieldDeclaration field) {
			this.fields.add(field);
			return self();
		}

		public ClassBuilder withTypes(List<TypeDeclaration> types) {
			this.memberTypes.addAll(types);
			return self();
		}

		@Override
		public TypeDeclaration build() {
			TypeDeclaration proto = new TypeDeclaration(((CompilationUnitDeclaration) node.top().get()).compilationResult);
			setGeneratedByAndCopyPos(proto, source);
			proto.modifiers = modifiers;
			proto.bits |= bits | ECLIPSE_DO_NOT_TOUCH_FLAG;
			if ((name == null) || name.isEmpty()) {
				proto.name = CharOperation.NO_CHAR;
			} else {
				proto.name = name.toCharArray();
			}
			if ((bits & (ASTNode.IsAnonymousType | ASTNode.IsLocalType)) != 0) {
				proto.sourceEnd = 0;
				proto.bodyEnd = source.sourceEnd + 2;
			}
			proto.annotations = annotations.isEmpty() ? null : annotations.toArray(new Annotation[annotations.size()]);
			proto.typeParameters = typeParameters.isEmpty() ? null : typeParameters.toArray(new TypeParameter[typeParameters.size()]);
			proto.fields = fields.isEmpty() ? null : fields.toArray(new FieldDeclaration[fields.size()]);
			proto.methods = methods.isEmpty() ? null : methods.toArray(new AbstractMethodDeclaration[methods.size()]);
			proto.memberTypes = memberTypes.isEmpty() ? null : memberTypes.toArray(new TypeDeclaration[memberTypes.size()]);
			proto.superInterfaces = superInterfaces.isEmpty() ? null : superInterfaces.toArray(new TypeReference[superInterfaces.size()]);
			proto.superclass = superclass;
			return proto; // TODO defensive copy ftw
		}

		@Override
		public void inject() {
			injectType(node, build());
		}
	}

	public static class FieldBuilder extends AbstractFieldBuilder<FieldBuilder, FieldDeclaration> {
		protected FieldBuilder(EclipseNode node, ASTNode source, int modifiers, TypeReference type, String fieldName) {
			super(node, source, modifiers, type, fieldName);
		}

		@Override public FieldDeclaration build() {
			FieldDeclaration proto = new FieldDeclaration(name.toCharArray(), 0, 0);
			setGeneratedByAndCopyPos(proto, source);
			proto.modifiers = modifiers;
			proto.annotations = annotations.isEmpty() ? null : annotations.toArray(new Annotation[annotations.size()]);
			proto.bits |= bits | ECLIPSE_DO_NOT_TOUCH_FLAG;
			proto.type = type;
			proto.initialization = initialization;
			return proto; // TODO defensive copy ftw
		}

		@Override public void inject() {
			injectField(node, build());
		}
	}

	public static class LocalBuilder extends AbstractFieldBuilder<LocalBuilder, LocalDeclaration> {
		protected LocalBuilder(EclipseNode node, ASTNode source, int modifiers, TypeReference type, String fieldName) {
			super(node, source, modifiers, type, fieldName);
		}

		@Override public LocalDeclaration build() {
			LocalDeclaration proto = new LocalDeclaration(name.toCharArray(), 0, 0);
			setGeneratedByAndCopyPos(proto, source);
			proto.modifiers = modifiers;
			proto.annotations = annotations.isEmpty() ? null : annotations.toArray(new Annotation[annotations.size()]);
			proto.bits |= bits | ECLIPSE_DO_NOT_TOUCH_FLAG;
			proto.type = type;
			proto.initialization = initialization;
			return proto; // TODO defensive copy ftw
		}

		@Override public void inject() {
			throw new UnsupportedOperationException();
		}
	}

	private static abstract class AbstractFieldBuilder<SELF_TYPE extends AbstractFieldBuilder<SELF_TYPE, BUILDER_RETURN_TYPE>, BUILDER_RETURN_TYPE extends AbstractVariableDeclaration> extends AbstractNodeBuilder<SELF_TYPE, BUILDER_RETURN_TYPE> {
		protected TypeReference type;
		protected Expression initialization;

		protected AbstractFieldBuilder(EclipseNode node, ASTNode source, int modifiers, TypeReference type, String fieldName) {
			super(node, source, modifiers, fieldName);
			this.type = copyType(type, source);
		}

		public SELF_TYPE withInitialization(Expression initialization) {
			this.initialization = initialization;
			return self();
		}
	}

	public static class ConstructorBuilder extends AbstractMethodBuilder<ConstructorBuilder, ConstructorDeclaration> {
		protected ExplicitConstructorCall constructorCall;

		public ConstructorBuilder(EclipseNode node, ASTNode source, int modifiers, String typeName) {
			super(node, source, modifiers, typeName);
		}

		public ConstructorBuilder withImplicitSuper() {
			this.constructorCall = new ExplicitConstructorCall(ExplicitConstructorCall.ImplicitSuper);
			return self();
		}

		@Override public ConstructorDeclaration build() {
			ConstructorDeclaration proto = new ConstructorDeclaration(((CompilationUnitDeclaration) node.top().get()).compilationResult);
			setGeneratedByAndCopyPos(proto, source);
			proto.modifiers = modifiers;
			proto.annotations = annotations.isEmpty() ? null : annotations.toArray(new Annotation[annotations.size()]);
			proto.constructorCall = constructorCall;
			proto.selector = name.toCharArray();
			proto.thrownExceptions = thrownExceptions.isEmpty() ? null : thrownExceptions.toArray(new TypeReference[thrownExceptions.size()]);
			proto.typeParameters = typeParameters.isEmpty() ? null : typeParameters.toArray(new TypeParameter[typeParameters.size()]);
			proto.bits |= bits | ECLIPSE_DO_NOT_TOUCH_FLAG;
			proto.arguments = parameters.isEmpty() ? null : parameters.toArray(new Argument[parameters.size()]);
			if (!statements.isEmpty()) {
				proto.statements = statements.toArray(new Statement[statements.size()]);
			}
			return proto; // TODO defensive copy ftw
		}
	}

	public static class MethodBuilder extends AbstractMethodBuilder<MethodBuilder, MethodDeclaration> {
		protected TypeReference returnType;

		protected MethodBuilder(EclipseNode node, ASTNode source, int modifiers, TypeReference returnType, String methodName) {
			super(node, source, modifiers, methodName);
			this.returnType = copyType(returnType, source);
		}

		public MethodBuilder withReturnType(String returnTypeName) {
			this.returnType = typeReference(source, returnTypeName);
			return self();
		}

		public MethodBuilder withReturnStatement(Expression expr) {
			return withStatement(returnStatement(source, expr));
		}

		@Override
		public MethodDeclaration build() {
			MethodDeclaration proto = new MethodDeclaration(((CompilationUnitDeclaration) node.top().get()).compilationResult);
			setGeneratedByAndCopyPos(proto, source);
			proto.modifiers = modifiers;
			proto.returnType = returnType;
			proto.annotations = annotations.isEmpty() ? null : annotations.toArray(new Annotation[annotations.size()]);
			proto.selector = name.toCharArray();
			proto.thrownExceptions = thrownExceptions.isEmpty() ? null : thrownExceptions.toArray(new TypeReference[thrownExceptions.size()]);
			proto.typeParameters = typeParameters.isEmpty() ? null : typeParameters.toArray(new TypeParameter[typeParameters.size()]);
			proto.bits |=  bits | ECLIPSE_DO_NOT_TOUCH_FLAG;
			proto.arguments = parameters.isEmpty() ? null : parameters.toArray(new Argument[parameters.size()]);
			if (!statements.isEmpty()) {
				proto.statements = statements.toArray(new Statement[statements.size()]);
			} else {
				proto.modifiers |= AccSemicolonBody;
			}
			return proto; // TODO defensive copy ftw
		}
	}

	private static abstract class AbstractMethodBuilder<SELF_TYPE extends AbstractMethodBuilder<SELF_TYPE, BUILDER_RETURN_TYPE>, BUILDER_RETURN_TYPE extends AbstractMethodDeclaration> extends AbstractNodeBuilder<SELF_TYPE, BUILDER_RETURN_TYPE> {
		protected List<TypeParameter> typeParameters = new ArrayList<TypeParameter>();
		protected List<Argument> parameters = new ArrayList<Argument>();
		protected List<TypeReference> thrownExceptions = new ArrayList<TypeReference>();
		protected List<Statement> statements = new ArrayList<Statement>();

		protected AbstractMethodBuilder(EclipseNode node, ASTNode source, int modifiers, String methodName) {
			super(node, source, modifiers, methodName);
		}

		public SELF_TYPE withThrownException(String thrownException) {
			this.thrownExceptions.add(typeReference(source, thrownException));
			return self();
		}

		public SELF_TYPE withThrownExceptions(List<TypeReference> thrownExceptions) {
			for (TypeReference thrownException : thrownExceptions) {
				this.thrownExceptions.add(copyType(thrownException, source));
			}
			return self();
		}

		public SELF_TYPE withParameter(String typeName, String argumentName) {
			this.parameters.add(argument(source, typeName, argumentName));
			return self();
		}

		public SELF_TYPE withParameter(TypeReference type, String argumentName) {
			this.parameters.add(argument(source, type, argumentName));
			return self();
		}

		public SELF_TYPE withParameter(Argument parameter) {
			this.parameters.add(parameter);
			return self();
		}

		public SELF_TYPE withParameters(List<Argument> parameters) {
			for (Argument parameter : parameters) {
				withParameter(parameter.type, new String(parameter.name));
			}
			return self();
		}

		public SELF_TYPE withStatement(Statement statement) {
			this.statements.add(statement);
			return self();
		}

		public SELF_TYPE withAssignStatement(Expression left, Expression right) {
			return withStatement(assignment(source, left, right));
		}

		public SELF_TYPE withAssignStatement(String leftName, Expression right) {
			return withStatement(assignment(source, leftName, right));
		}

		public SELF_TYPE withAssignStatement(String leftName, String rightName) {
			return withStatement(assignment(source, leftName, rightName));
		}

		public SELF_TYPE withStatements(List<Statement> statements) {
			this.statements.addAll(statements);
			return self();
		}

		@Override
		public void inject() {
			injectMethod(node, build());
		}
	}

	public static void injectType(EclipseNode typeNode, TypeDeclaration type) {
		type.annotations = createSuppressWarningsAll(type, type.annotations);
		TypeDeclaration parent = (TypeDeclaration) typeNode.get();

		if (parent.memberTypes == null) {
			parent.memberTypes = new TypeDeclaration[]{ type };
		} else {
			TypeDeclaration[] newArray = new TypeDeclaration[parent.memberTypes.length + 1];
			System.arraycopy(parent.memberTypes, 0, newArray, 0, parent.memberTypes.length);
			newArray[parent.memberTypes.length] = type;
			parent.memberTypes = newArray;
		}
		typeNode.add(type, Kind.TYPE).recursiveSetHandled();
	}

	private static abstract class AbstractNodeBuilder<SELF_TYPE, BUILDER_RETURN_TYPE> {
		protected final EclipseNode node;
		protected final ASTNode source;
		protected final String name;
		protected int modifiers;
		protected int bits;
		protected List<Annotation> annotations = new ArrayList<Annotation>();

		protected AbstractNodeBuilder(EclipseNode node, ASTNode source, int modifiers, String name) {
			this.node = node;
			this.source = source;
			this.name = name;
			this.modifiers = modifiers;
		}

		public SELF_TYPE withModifiers(int modifiers) {
			this.modifiers = modifiers;
			return self();
		}

		public SELF_TYPE withBits(int bits) {
			this.bits |= bits;
			return self();
		}

		public SELF_TYPE withAnnotation(String typeName) {
			this.annotations.add(annotation(source, typeName));
			return self();
		}

		public SELF_TYPE withAnnotation(String typeName, String value) {
			this.annotations.add(annotation(source, typeName, value));
			return self();
		}

		public SELF_TYPE withAnnotations(List<Annotation> annotations) {
			for (Annotation annotation : annotations) {
				this.annotations.add(annotation(source, annotation.type.toString()));
			}
			return self();
		}

		protected final SELF_TYPE self() {
			return Cast.<SELF_TYPE>uncheckedCast(this);
		}

		public abstract BUILDER_RETURN_TYPE build();

		public abstract void inject();

		@Override
		public String toString() {
			return build().toString();
		}
	}
}
