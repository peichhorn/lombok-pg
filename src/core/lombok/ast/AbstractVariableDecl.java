package lombok.ast;

import static lombok.ast.Modifier.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import lombok.Getter;
import lombok.core.util.Cast;

public abstract class AbstractVariableDecl<SELF_TYPE> extends Statement {
	@Getter
	protected final EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
	@Getter
	protected final List<Annotation> annotations = new ArrayList<Annotation>();
	protected final TypeRef type;
	@Getter
	protected final String name;
	
	public AbstractVariableDecl(final TypeRef type, final String name) {
		this.type = child(type);
		this.name = name;
	}
	
	protected final SELF_TYPE self() {
		return Cast.<SELF_TYPE>uncheckedCast(this);
	}

	public SELF_TYPE makeFinal() {
		return withModifier(FINAL);
	}

	public SELF_TYPE withModifier(final Modifier modifier) {
		modifiers.add(modifier);
		return self();
	}
	
	public SELF_TYPE withAnnotation(final Annotation annotation) {
		annotations.add(child(annotation));
		return self();
	}
	
	public SELF_TYPE withAnnotations(final List<Annotation> annotations) {
		for (Annotation annotation : annotations) withAnnotation(annotation);
		return self();
	}
}
