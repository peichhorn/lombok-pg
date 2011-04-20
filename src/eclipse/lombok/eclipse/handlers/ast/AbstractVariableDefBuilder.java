/*
 * Copyright Â© 2011 Philipp Eichhorn
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
abstract class AbstractVariableDefBuilder<SELF_TYPE extends AbstractVariableDefBuilder<SELF_TYPE, NODE_TYPE>, NODE_TYPE extends AbstractVariableDeclaration> implements StatementBuilder<NODE_TYPE> {
	protected final List<ExpressionBuilder<? extends Annotation>> annotations = new ArrayList<ExpressionBuilder<? extends Annotation>>();
	protected final ExpressionBuilder<? extends TypeReference> type;
	protected final String name;
	protected ExpressionBuilder<? extends Expression> initialization;
	protected int modifiers;
	protected int bits;
	
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
	
	public SELF_TYPE withInitialization(final Expression initialization) {
		return withInitialization(new ExpressionWrapper<Expression>(initialization));
	}
}
