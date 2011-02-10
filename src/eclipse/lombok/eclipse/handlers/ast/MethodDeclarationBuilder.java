package lombok.eclipse.handlers.ast;

import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccAbstract;
import static org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers.AccSemicolonBody;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

public class MethodDeclarationBuilder extends AbstractMethodDeclarationBuilder<MethodDeclarationBuilder, MethodDeclaration> {
	protected ExpressionBuilder<? extends TypeReference> returnType;
	protected boolean noBody;
	
	public MethodDeclarationBuilder(final ExpressionBuilder<? extends TypeReference> returnType, final String name) {
		super(name);
		this.returnType = returnType;
	}
	
	public MethodDeclarationBuilder withReturnType(final ExpressionBuilder<? extends TypeReference> returnType) {
		this.returnType = returnType;
		return self();
	}
	
	public MethodDeclarationBuilder withNoBody() {
		noBody = true;
		return self();
	}

	@Override
	public MethodDeclaration build(final EclipseNode node, final ASTNode source) {
		MethodDeclaration proto = new MethodDeclaration(((CompilationUnitDeclaration) node.top().get()).compilationResult);
		setGeneratedByAndCopyPos(proto, source);
		for (StatementBuilder<? extends Statement> statement : statements) {
			if (statement instanceof DefaultReturnStatementBuilder) {
				((DefaultReturnStatementBuilder) statement).withReturnType(returnType);
			}
		}
		proto.modifiers = modifiers;
		proto.returnType = returnType.build(node, source);
		proto.annotations = buildAnnotations(node, source);
		proto.selector = name.toCharArray();
		proto.thrownExceptions = buildThrownExceptions(node, source);
		proto.typeParameters = buildTypeParameters(node, source);
		proto.bits |=  bits | ECLIPSE_DO_NOT_TOUCH_FLAG;
		proto.arguments = buildArguments(node, source);
		if (noBody || (modifiers & AccAbstract) != 0) {
			proto.modifiers |= AccSemicolonBody;
		} else {
			proto.statements = buildStatements(node, source);
		}
		return proto;
	}
}
