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

import lombok.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AST {
	public static Binary Add(final Expression left, final Expression right) {
		return new Binary(left, "+", right);
	}

	public static Binary And(final Expression left, final Expression right) {
		return new Binary(left, "&&", right);
	}

	public static Annotation Annotation(final TypeRef type) {
		return new Annotation(type);
	}

	public static Argument Arg(final TypeRef type, final String name) {
		return new Argument(type, name).makeFinal();
	}

	public static Assignment Assign(final Expression left, final Expression right) {
		return new Assignment(left, right);
	}

	public static Block Block() {
		return new Block();
	}

	public static Call Call(final String name) {
		return new Call(name);
	}

	public static Call Call(final Expression receiver, final String name) {
		return new Call(receiver, name);
	}

	public static Cast Cast(final TypeRef type, final Expression expression) {
		return new Cast(type, expression);
	}

	public static Case Case(final Expression expression) {
		return new Case(expression);
	}

	public static Case Case() {
		return new Case();
	}

	public static CharLiteral Char(final String character) {
		return new CharLiteral(character);
	}

	public static ClassDecl ClassDecl(final String name) {
		return new ClassDecl(name);
	}

	public static ConstructorDecl ConstructorDecl(final String name) {
		return new ConstructorDecl(name);
	}

	public static Continue Continue() {
		return new Continue();
	}

	public static Continue Continue(final String label) {
		return new Continue(label);
	}

	public static DoWhile Do(final Statement action) {
		return new DoWhile(action);
	}

	public static EnumConstant EnumConstant(final String name) {
		return new EnumConstant(name);
	}

	public static Equal Equal(final Expression left, final Expression right) {
		return new Equal(left, right, false);
	}

	public static Equal NotEqual(final Expression left, final Expression right) {
		return new Equal(left, right, true);
	}

	public static Expression Expr(final Object wrappedObject) {
		return new WrappedExpression(wrappedObject);
	}

	public static BooleanLiteral False() {
		return new BooleanLiteral(false);
	}

	public static FieldRef Field(final Expression receiver, final String name) {
		return new FieldRef(receiver, name);
	}

	public static FieldRef Field(final String name) {
		return new FieldRef(name);
	}

	public static FieldDecl FieldDecl(final TypeRef type, final String name) {
		return new FieldDecl(type, name);
	}

	public static Foreach Foreach(final LocalDecl elementVariable) {
		return new Foreach(elementVariable);
	}

	public static If If(final Expression condition) {
		return new If(condition);
	}

	public static InstanceOf InstanceOf(final Expression expression, final TypeRef type) {
		return new InstanceOf(expression, type);
	}

	public static ArrayRef ArrayRef(final Expression indexed, final Expression index) {
		return new ArrayRef(indexed, index);
	}

	public static ClassDecl InterfaceDecl(final String name) {
		return new ClassDecl(name).makeInterface();
	}

	public static LocalDecl LocalDecl(final TypeRef type, final String name) {
		return new LocalDecl(type, name);
	}

	public static NameRef Name(final String name) {
		return new NameRef(name);
	}

	public static New New(final TypeRef type) {
		return new New(type);
	}

	public static NewArray NewArray(final TypeRef type) {
		return new NewArray(type);
	}

	public static NewArray NewArray(final TypeRef type, final int dimensions) {
		return new NewArray(type, dimensions);
	}

	public static Unary Not(final Expression condition) {
		return new Unary("!", condition);
	}

	public static NullLiteral Null() {
		return new NullLiteral();
	}

	public static MethodDecl MethodDecl(final TypeRef returnType, final String name) {
		return new MethodDecl(returnType, name);
	}

	public static MethodDecl MethodDecl(final Object wrappedObject) {
		return new WrappedMethodDecl(wrappedObject);
	}

	public static Argument NonFinalArg(final TypeRef type, final String name) {
		return new Argument(type, name);
	}

	public static NumberLiteral Number(final Number number) {
		return new NumberLiteral(number);
	}

	public static Binary Or(final Expression left, final Expression right) {
		return new Binary(left, "||", right);
	}

	public static Return Return() {
		return new Return();
	}

	public static Return Return(final Expression expression) {
		return new Return(expression);
	}

	public static ReturnDefault ReturnDefault() {
		return new ReturnDefault();
	}

	public static Statement Stat(final Object wrappedObject) {
		return new WrappedStatement(wrappedObject);
	}

	public static StringLiteral String(final String value) {
		return new StringLiteral(value);
	}

	public static Switch Switch(final Expression expression) {
		return new Switch(expression);
	}

	public static This This() {
		return new This();
	}

	public static This This(final TypeRef type) {
		return new This(type);
	}

	public static Throw Throw(final Expression init) {
		return new Throw(init);
	}

	public static BooleanLiteral True() {
		return new BooleanLiteral(true);
	}

	public static Synchronized Synchronized(final Expression lock) {
		return new Synchronized(lock);
	}

	public static Try Try(final Block tryBlock) {
		return new Try(tryBlock);
	}

	public static TypeRef Type(final Class<?> clazz) {
		return new TypeRef(clazz);
	}

	public static TypeRef Type(final String typeName) {
		return new TypeRef(typeName);
	}

	public static TypeRef Type(final Object wrappedObject) {
		return new WrappedTypeRef(wrappedObject);
	}

	public static TypeParam TypeParam(final String name) {
		return new TypeParam(name);
	}

	public static While While(final Expression condition) {
		return new While(condition);
	}

	public static Wildcard Wildcard() {
		return new Wildcard();
	}

	public static Wildcard Wildcard(final Wildcard.Bound bound, final TypeRef type) {
		return new Wildcard(bound, type);
	}
}
