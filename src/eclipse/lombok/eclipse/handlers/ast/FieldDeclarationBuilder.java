package lombok.eclipse.handlers.ast;

import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import static lombok.eclipse.handlers.Eclipse.typeNodeOf;
import static lombok.eclipse.handlers.EclipseHandlerUtil.injectField;
import static lombok.eclipse.handlers.ast.Arrays.buildArray;

import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

public final class FieldDeclarationBuilder extends AbstractVariableDeclarationBuilder<FieldDeclarationBuilder, FieldDeclaration> {
	
	protected FieldDeclarationBuilder(ExpressionBuilder<? extends TypeReference> type, String name) {
		super(type, name);
	}

	public void injectInto(final EclipseNode node, final ASTNode source) {
		injectField(typeNodeOf(node), build(node, source));
	}
	
	@Override
	public FieldDeclaration build(final EclipseNode node, final ASTNode source) {
		FieldDeclaration proto = new FieldDeclaration(name.toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(proto, source);
		proto.modifiers = modifiers;
		proto.annotations = buildArray(annotations, new Annotation[0], node, source);
		proto.bits |= bits | ECLIPSE_DO_NOT_TOUCH_FLAG;
		proto.type = type.build(node, source);
		if (initialization != null) {
			proto.initialization = initialization.build(node, source);
		}
		return proto;
	}
}
