package lombok.eclipse.handlers.ast;
import static lombok.core.util.Arrays.isNotEmpty;
import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import static lombok.eclipse.handlers.Eclipse.typeNodeOf;
import static lombok.eclipse.handlers.EclipseHandlerUtil.injectMethod;
import static lombok.eclipse.handlers.ast.ASTBuilder.Arg;
import static lombok.eclipse.handlers.ast.ASTBuilder.Type;
import static lombok.eclipse.handlers.ast.ASTBuilder.TypeParam;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccAbstract;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccVarargs;
import static org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers.AccGenericSignature;
import static org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers.AccSemicolonBody;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;

public class MethodBindingMethodDeclarationBuilder extends AbstractMethodDeclarationBuilder<MethodBindingMethodDeclarationBuilder, MethodDeclaration> {
	protected final MethodBinding abstractMethod;
	protected ExpressionBuilder<? extends TypeReference> returnType;
	protected boolean noBody;
	
	MethodBindingMethodDeclarationBuilder(final MethodBinding abstractMethod) {
		super(new String(abstractMethod.selector));
		this.abstractMethod = abstractMethod;
	}
	
	public MethodBindingMethodDeclarationBuilder withReturnType(final ExpressionBuilder<? extends TypeReference> returnType) {
		this.returnType = returnType;
		return this;
	}
	
	public MethodBindingMethodDeclarationBuilder withNoBody() {
		noBody = true;
		return this;
	}
	
	@Override
	public MethodDeclaration build(final EclipseNode node, final ASTNode source) {
		MethodDeclaration proto = new MethodDeclaration(((CompilationUnitDeclaration) node.top().get()).compilationResult);
		setGeneratedByAndCopyPos(proto, source);

		if (returnType == null) {
			returnType = Type(abstractMethod.returnType);
		}
		for (StatementBuilder<? extends Statement> statement : statements) {
			if (statement instanceof DefaultReturnStatementBuilder) {
				((DefaultReturnStatementBuilder) statement).withReturnType(returnType);
			}
		}
		if (isNotEmpty(abstractMethod.thrownExceptions)) for (int i = 0; i < abstractMethod.thrownExceptions.length; i++) {
			thrownExceptions.add(Type(abstractMethod.thrownExceptions[i]));
		}
		if (isNotEmpty(abstractMethod.parameters)) for (int i = 0; i < abstractMethod.parameters.length; i++) {
			arguments.add(Arg(Type(abstractMethod.parameters[i]), "arg" + i).makeFinal());
		}
		
		if (isNotEmpty(abstractMethod.typeVariables)) for (int i = 0; i < abstractMethod.typeVariables.length; i++) {
			TypeVariableBinding binding = abstractMethod.typeVariables[i];
			ReferenceBinding super1 = binding.superclass;
			ReferenceBinding[] super2 = binding.superInterfaces;
			TypeParameterBuilder typeParameter = TypeParam(new String(binding.sourceName));
			if (super2 == null) super2 = new ReferenceBinding[0];
			if (super1 != null || super2.length > 0) {
				if (super1 != null) typeParameter.withType(Type(super1));
				else typeParameter.withType(Type(super2[0]));
				for (int j = (super1 == null) ? 1 : 0; j < super2.length; j++) {
					typeParameter.withBound(Type(super2[j]).makeSuperType());
				}
			}
		}
		
		proto.modifiers = (abstractMethod.getAccessFlags() ^ AccAbstract) | modifiers;
		proto.returnType = returnType.build(node, source);
		proto.annotations = buildAnnotations(node, source);
		proto.selector = name.toCharArray();
		proto.thrownExceptions = buildThrownExceptions(node, source);
		proto.typeParameters = buildTypeParameters(node, source);
		proto.bits |=  bits | ECLIPSE_DO_NOT_TOUCH_FLAG;
		proto.arguments = buildArguments(node, source);
		
		if (noBody || ((modifiers & AccAbstract) != 0)) {
			proto.modifiers |= AccSemicolonBody;
		} else {
			proto.statements = buildStatements(node, source);
		}
		return proto;
	}
	
	public MethodDeclaration injectInto(final EclipseNode node, final ASTNode source) {
		final EclipseNode typeNode = typeNodeOf(node);
		final TypeDeclaration type = (TypeDeclaration) typeNode.get();
		final MethodDeclaration method = build(node, source);
		
		SourceTypeBinding sourceType = type.scope.referenceContext.binding;
		MethodScope methodScope = new MethodScope(type.scope, method, false);
		SourceTypeBinding declaringClass = methodScope.referenceType().binding;
		MethodBinding methodBinding = new MethodBinding(method.modifiers, method.selector, abstractMethod.returnType, abstractMethod.parameters, abstractMethod.thrownExceptions, declaringClass);
		method.binding = methodBinding;
		method.scope = methodScope;

		Argument[] argTypes = method.arguments;
		int argLength = argTypes == null ? 0 : argTypes.length;
		if ((argLength > 0) && argTypes[--argLength].isVarArgs())
			methodBinding.modifiers |= AccVarargs;

		TypeParameter[] typeParameters = method.typeParameters();
		if (typeParameters != null) {
			methodBinding.typeVariables = methodScope.createTypeVariables(typeParameters, method.binding);
			methodBinding.modifiers |= AccGenericSignature;
		}

		MethodBinding[] allMethods = new MethodBinding[sourceType.methods().length + 1];
		System.arraycopy(sourceType.methods(), 0, allMethods, 0, sourceType.methods().length);
		allMethods[sourceType.methods().length] = methodBinding;
		sourceType.setMethods(allMethods);
		
		injectMethod(typeNode, method);
		return method;
	}
}
