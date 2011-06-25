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

import lombok.Getter;

@Getter
public class ClassDecl extends Statement {
	protected final EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
	private final List<Annotation> annotations = new ArrayList<Annotation>();
	private final List<TypeParam> typeParameters = new ArrayList<TypeParam>();
	private final List<FieldDecl> fields = new ArrayList<FieldDecl>();
	private final List<AbstractMethodDecl<?>> methods = new ArrayList<AbstractMethodDecl<?>>();
	private final List<ClassDecl> memberTypes = new ArrayList<ClassDecl>();
	private final List<TypeRef> superInterfaces = new ArrayList<TypeRef>();
	private final String name;
	private TypeRef superclass;
	private boolean local;
	private boolean anonymous;
	private boolean isInterface;
	
	public ClassDecl(final String name) {
		this.name = name;
	}

	public ClassDecl implementing(final TypeRef type) {
		superInterfaces.add(child(type));
		return this;
	}

	public ClassDecl implementing(final List<TypeRef> types) {
		for (TypeRef type : types) implementing(type);
		return this;
	}

	public ClassDecl makeLocal() {
		local = true;
		return this;
	}

	public ClassDecl makeAnonymous() {
		anonymous = true;
		return this;
	}
	
	public ClassDecl makeInterface() {
		isInterface = true;
		return this;
	}
	
	public ClassDecl makePrivate() {
		return withModifier(PRIVATE);
	}

	public ClassDecl makeProtected() {
		return withModifier(PROTECTED);
	}

	public ClassDecl makePublic() {
		return withModifier(PUBLIC);
	}

	public ClassDecl makeStatic() {
		return withModifier(STATIC);
	}
	
	public ClassDecl makeFinal() {
		return withModifier(FINAL);
	}

	public ClassDecl withModifier(final Modifier modifier) {
		modifiers.add(modifier);
		return this;
	}

	public ClassDecl withMethod(final AbstractMethodDecl<?> method) {
		methods.add(child(method));
		return this;
	}
	
	public ClassDecl withMethods(final List<AbstractMethodDecl<?>> methods) {
		for (AbstractMethodDecl<?> method : methods) withMethod(method);
		return this;
	}

	public ClassDecl withField(final FieldDecl field) {
		fields.add(child(field));
		return this;
	}
	
	public ClassDecl withFields(final List<FieldDecl> fields) {
		for (FieldDecl field : fields) withField(field);
		return this;
	}

	public ClassDecl withType(final ClassDecl type) {
		memberTypes.add(child(type));
		return this;
	}
	
	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, PARAMETER_TYPE p) {
		return v.visitClassDecl(this, p);
	}
}
