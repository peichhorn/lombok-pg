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
package lombok.ast;

import static lombok.ast.Modifier.*;

import java.util.*;

import lombok.*;
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
