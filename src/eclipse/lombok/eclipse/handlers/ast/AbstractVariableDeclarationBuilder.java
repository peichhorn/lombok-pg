package lombok.eclipse.handlers.ast;

import static org.eclipse.jdt.core.dom.Modifier.FINAL;
import static org.eclipse.jdt.core.dom.Modifier.PRIVATE;
import static org.eclipse.jdt.core.dom.Modifier.PUBLIC;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.core.util.Cast;

import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
abstract class AbstractVariableDeclarationBuilder<SELF_TYPE extends AbstractVariableDeclarationBuilder<SELF_TYPE, NODE_TYPE>, NODE_TYPE extends AbstractVariableDeclaration> implements StatementBuilder<NODE_TYPE> {
	protected final ExpressionBuilder<? extends TypeReference> type;
	protected final String name;
	protected int modifiers;
	protected int bits;
	protected final List<ExpressionBuilder<? extends Annotation>> annotations = new ArrayList<ExpressionBuilder<? extends Annotation>>();
	protected ExpressionBuilder<? extends Expression> initialization;
	
	protected final SELF_TYPE self() {
		return Cast.<SELF_TYPE>uncheckedCast(this);
	}
	
	public SELF_TYPE makeFinal() {
		return withModifiers(FINAL);
	}
	
	public SELF_TYPE makePublic() {
		return withModifiers(PUBLIC);
	}
	
	public SELF_TYPE makePrivate() {
		return withModifiers(PRIVATE);
	}
	
	public SELF_TYPE makePublicFinal() {
		return withModifiers(PUBLIC | FINAL);
	}
	
	public SELF_TYPE makePrivateFinal() {
		return withModifiers(PRIVATE | FINAL);
	}
	
	public SELF_TYPE withModifiers(final int modifiers) {
		this.modifiers |= modifiers;
		return self();
	}
	
	public SELF_TYPE withBits(int bits) {
		this.bits |= bits;
		return self();
	}
	
	public SELF_TYPE withAnnotation(final ExpressionBuilder<? extends Annotation> annotation) {
		annotations.add(annotation);
		return self();
	}
	
	public SELF_TYPE withAnnotations(Annotation... annotations) {
		if (annotations != null) for (Annotation annotation : annotations) {
			this.annotations.add(new ExpressionWrapper<Annotation>(annotation));
		}
		return self();
	}
	
	public SELF_TYPE withInitialization(final ExpressionBuilder<? extends Expression> initialization) {
		this.initialization = initialization;
		return self();
	}
	
	public SELF_TYPE withInitialization(Expression initialization) {
		return withInitialization(new ExpressionWrapper<Expression>(initialization));
	}
}
