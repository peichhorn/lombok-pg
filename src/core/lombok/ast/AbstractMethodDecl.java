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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.core.util.Cast;

@Getter
public abstract class AbstractMethodDecl<SELF_TYPE> extends Node {
	protected final EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
	protected final List<Annotation> annotations = new ArrayList<Annotation>();
	protected final List<TypeParam> typeParameters = new ArrayList<TypeParam>();
	protected final List<Argument> arguments = new ArrayList<Argument>();
	protected final List<TypeRef> thrownExceptions = new ArrayList<TypeRef>();
	protected final List<Statement> statements = new ArrayList<Statement>();
	protected final String name;

	public AbstractMethodDecl(final String name) {
		this.name = name;
	}

	protected final SELF_TYPE self() {
		return Cast.<SELF_TYPE>uncheckedCast(this);
	}

	public SELF_TYPE makePrivate() {
		return withModifier(PRIVATE);
	}

	public SELF_TYPE makeProtected() {
		return withModifier(PROTECTED);
	}

	public SELF_TYPE makePublic() {
		return withModifier(PUBLIC);
	}

	public SELF_TYPE makeStatic() {
		return withModifier(STATIC);
	}

	public SELF_TYPE withAccessLevel(final AccessLevel level) {
		switch (level) {
		case PUBLIC: return makePublic();
		case PROTECTED: return makeProtected();
		case PRIVATE: return makePrivate();
		default:
			return self();
		}
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

	public SELF_TYPE withArgument(final Argument argument) {
		arguments.add(child(argument));
		return self();
	}

	public SELF_TYPE withArguments(final List<Argument> arguments) {
		for (Argument argument : arguments) withArgument(argument);
		return self();
	}

	public SELF_TYPE withStatement(final Statement statement) {
		statements.add(child(statement));
		return self();
	}

	public SELF_TYPE withStatements(final List<Statement> statements) {
		for (Statement statement : statements) withStatement(statement);
		return self();
	}

	public SELF_TYPE withThrownException(final TypeRef thrownException) {
		thrownExceptions.add(child(thrownException));
		return self();
	}

	public SELF_TYPE withThrownExceptions(final List<TypeRef> thrownExceptions) {
		for (TypeRef thrownException : thrownExceptions) withThrownException(thrownException);
		return self();
	}

	public SELF_TYPE withTypeParameter(final TypeParam typeParameter) {
		typeParameters.add(child(typeParameter));
		return self();
	}
}
