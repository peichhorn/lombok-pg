package lombok.eclipse.handlers.ast;

import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;

public class ConstructorDeclarationBuilder extends AbstractMethodDeclarationBuilder<ConstructorDeclarationBuilder, ConstructorDeclaration> {
	private ExplicitConstructorCall constructorCall;
	
	public ConstructorDeclarationBuilder(final String typeName) {
		super(typeName);
	}
	
	public ConstructorDeclarationBuilder withImplicitSuper() {
		this.constructorCall = new ExplicitConstructorCall(ExplicitConstructorCall.ImplicitSuper);
		return this;
	}

	@Override
	public ConstructorDeclaration build(final EclipseNode node, final ASTNode source) {
		ConstructorDeclaration proto = new ConstructorDeclaration(((CompilationUnitDeclaration) node.top().get()).compilationResult);
		setGeneratedByAndCopyPos(proto, source);
		proto.modifiers = modifiers;
		proto.annotations = buildAnnotations(node, source);
		proto.constructorCall = constructorCall;
		proto.selector = name.toCharArray();
		proto.thrownExceptions = buildThrownExceptions(node, source);
		proto.typeParameters = buildTypeParameters(node, source);
		proto.bits |=  bits | ECLIPSE_DO_NOT_TOUCH_FLAG;
		proto.arguments = buildArguments(node, source);
		if (!statements.isEmpty()) {
			proto.statements = buildStatements(node, source);
		}
		return proto;
	}
}
