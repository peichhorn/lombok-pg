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
package lombok.eclipse.handlers.ast;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ASTBuilder {
	public static BinaryBuilder Add(final ExpressionBuilder<? extends Expression> left, final ExpressionBuilder<? extends Expression> right) {
		return new BinaryBuilder("+", left, right);
	}

	public static AnnotationBuilder Annotation(final ExpressionBuilder<? extends TypeReference> type) {
		return new AnnotationBuilder(type);
	}

	public static ArgBuilder Arg(final ExpressionBuilder<? extends TypeReference> type, final String name) {
		return new ArgBuilder(type, name).makeFinal();
	}

	public static AssignBuilder Assign(final ExpressionBuilder<? extends Expression> left, final ExpressionBuilder<? extends Expression> right) {
		return new AssignBuilder(left, right);
	}

	public static BlockBuilder Block() {
		return new BlockBuilder();
	}

	public static CallBuilder Call(final String name) {
		return new CallBuilder(name);
	}

	public static CallBuilder Call(final ExpressionBuilder<? extends Expression> receiver, final String name) {
		return new CallBuilder(receiver, name);
	}

	public static CastBuilder Cast(final ExpressionBuilder<? extends TypeReference> type, final ExpressionBuilder<? extends Expression> expression) {
		return new CastBuilder(type, expression);
	}

	public static ClassDefBuilder ClassDef(final String name) {
		return new ClassDefBuilder(name);
	}

	public static ConstructorDefBuilder ConstructorDef(final String name) {
		return new ConstructorDefBuilder(name);
	}

	public static ContinueBuilder Continue() {
		return new ContinueBuilder();
	}

	public static ContinueBuilder Continue(final String label) {
		return new ContinueBuilder(label);
	}

	public static DoBuilder Do(final StatementBuilder<? extends Statement> action) {
		return new DoBuilder(action);
	}

	public static EnumConstantBuilder EnumConstant(final String name) {
		return new EnumConstantBuilder(name);
	}

	public static EqualBuilder Equal(final ExpressionBuilder<? extends Expression> left, final ExpressionBuilder<? extends Expression> right) {
		return new EqualBuilder(left, right, false);
	}

	public static EqualBuilder NotEqual(final ExpressionBuilder<? extends Expression> left, final ExpressionBuilder<? extends Expression> right) {
		return new EqualBuilder(left, right, true);
	}

	public static NullTrueFalseBuilder False() {
		return new NullTrueFalseBuilder(false);
	}

	public static FieldBuilder Field(final ExpressionBuilder<? extends Expression> receiver, final String name) {
		return new FieldBuilder(receiver, name);
	}

	public static FieldBuilder Field(final String name) {
		return new FieldBuilder(name);
	}

	public static FieldDefBuilder FieldDef(final ExpressionBuilder<? extends TypeReference> type, final String name) {
		return new FieldDefBuilder(type, name);
	}

	public static ForeachBuilder Foreach(final StatementBuilder<? extends LocalDeclaration> elementVariable) {
		return new ForeachBuilder(elementVariable);
	}

	public static IfBuilder If(final ExpressionBuilder<? extends Expression> condition) {
		return new IfBuilder(condition);
	}

	public static InstanceOfBuilder InstanceOf(final ExpressionBuilder<? extends Expression> expression, final ExpressionBuilder<? extends TypeReference> type) {
		return new InstanceOfBuilder(expression, type);
	}

	public static LocalDefBuilder LocalDef(final ExpressionBuilder<? extends TypeReference> type, final String name) {
		return new LocalDefBuilder(type, name);
	}

	public static NameBuilder Name(final String name) {
		return new NameBuilder(name);
	}

	public static NewBuilder New(final ExpressionBuilder<? extends TypeReference> type) {
		return new NewBuilder(type);
	}

	public static FullNewBuilder New(final ExpressionBuilder<? extends TypeReference> type, final ASTNodeBuilder<? extends TypeDeclaration> anonymousType) {
		return new FullNewBuilder(type, anonymousType);
	}

	public static UnaryBuilder Not(final ExpressionBuilder<? extends Expression> condition) {
		return new UnaryBuilder("!", condition);
	}

	public static NullTrueFalseBuilder Null() {
		return new NullTrueFalseBuilder(null);
	}

	public static MethodDefBuilder MethodDef(final ExpressionBuilder<? extends TypeReference> returnType, final String name) {
		return new MethodDefBuilder(returnType, name);
	}

	public static ResolutionBasedMethodDefBuilder MethodDef(final MethodBinding abstractMethod) {
		return new ResolutionBasedMethodDefBuilder(abstractMethod);
	}

	public static NumberBuilder Number(final Number number) {
		return new NumberBuilder(number);
	}

	public static CharBuilder Char(final String character) {
		return new CharBuilder(character);
	}

	public static ReturnBuilder Return() {
		return new ReturnBuilder();
	}

	public static ReturnBuilder Return(final ExpressionBuilder<? extends Expression> expression) {
		return new ReturnBuilder(expression);
	}

	public static DefaultReturnBuilder ReturnDefault() {
		return new DefaultReturnBuilder();
	}

	public static StringBuilder String(final String value) {
		return new StringBuilder(value);
	}

	public static ThisBuilder This() {
		return new ThisBuilder();
	}

	public static ThisBuilder This(final ExpressionBuilder<? extends TypeReference> type) {
		return new ThisBuilder(type);
	}

	public static ThrowBuilder Throw(final ExpressionBuilder<? extends Expression> init) {
		return new ThrowBuilder(init);
	}

	public static NullTrueFalseBuilder True() {
		return new NullTrueFalseBuilder(true);
	}

	public static TryBuilder Try(final StatementBuilder<? extends org.eclipse.jdt.internal.compiler.ast.Block> tryBlock) {
		return new TryBuilder(tryBlock);
	}

	public static TypeBuilder Type(final Class<?> clazz) {
		return new TypeBuilder(clazz.getName());
	}

	public static TypeBuilder Type(final String typeName) {
		return new TypeBuilder(typeName);
	}

	public static ResolutionBasedTypeBuilder Type(final TypeBinding typeBinding) {
		return new ResolutionBasedTypeBuilder(typeBinding);
	}

	public static ExpressionWrapper<TypeReference> Type(final TypeReference typeReference) {
		return new ExpressionWrapper<TypeReference>(typeReference);
	}

	public static TypeParamBuilder TypeParam(final String name) {
		return new TypeParamBuilder(name);
	}

	public static WhileBuilder While(final ExpressionBuilder<? extends Expression> condition) {
		return new WhileBuilder(condition);
	}
}
