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
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ASTBuilder {
	public static AnnotationBuilder Annotation(final ExpressionBuilder<? extends TypeReference> type) {
		return new AnnotationBuilder(type);
	}
	
	public static ArgumentBuilder Arg(final ExpressionBuilder<? extends TypeReference> type, final String name) {
		return new ArgumentBuilder(type, name).makeFinal();
	}
	
	public static AssignmentBuilder Assign(final ExpressionBuilder<? extends Expression> left, final ExpressionBuilder<? extends Expression> right) {
		return new AssignmentBuilder(left, right);
	}
	
	public static BlockBuilder Block() {
		return new BlockBuilder();
	}
	
	public static MessageSendBuilder Call(final String name) {
		return new MessageSendBuilder(name);
	}
	
	public static MessageSendBuilder Call(final ExpressionBuilder<? extends Expression> receiver, final String name) {
		return new MessageSendBuilder(receiver, name);
	}
	
	public static CastExpressionBuilder Cast(final ExpressionBuilder<? extends TypeReference> type, final ExpressionBuilder<? extends Expression> expression) {
		return new CastExpressionBuilder(type, expression);
	}
	
	public static TypeDeclarationBuilder ClassDef(final String name) {
		return new TypeDeclarationBuilder(name);
	}
	
	public static ConstructorDeclarationBuilder ConstructorDef(final String name) {
		return new ConstructorDeclarationBuilder(name);
	}
	
	public static ContinueStatementBuilder Continue() {
		return new ContinueStatementBuilder();
	}
	
	public static ContinueStatementBuilder Continue(final String label) {
		return new ContinueStatementBuilder(label);
	}
	
	public static DoStatementBuilder Do(final StatementBuilder<? extends Statement> action) {
		return new DoStatementBuilder(action);
	}
	
	public static EnumConstantBuilder EnumConstant(final String name) {
		return new EnumConstantBuilder(name);
	}
	
	public static EqualExpressionBuilder Equal(final ExpressionBuilder<? extends Expression> left, final ExpressionBuilder<? extends Expression> right) {
		return new EqualExpressionBuilder(left, right, OperatorIds.EQUAL_EQUAL);
	}
	
	public static EqualExpressionBuilder NotEqual(final ExpressionBuilder<? extends Expression> left, final ExpressionBuilder<? extends Expression> right) {
		return new EqualExpressionBuilder(left, right, OperatorIds.NOT_EQUAL);
	}
	
	public static MagicLiteralBuilder False() {
		return new MagicLiteralBuilder(false);
	}
	
	public static FieldReferenceBuilder Field(final ExpressionBuilder<? extends Expression> receiver, final String name) {
		return new FieldReferenceBuilder(receiver, name);
	}
	
	public static FieldReferenceBuilder Field(final String name) {
		return new FieldReferenceBuilder(name);
	}
	
	public static FieldDeclarationBuilder FieldDef(final ExpressionBuilder<? extends TypeReference> type, final String name) {
		return new FieldDeclarationBuilder(type, name);
	}
	
	public static ForeachStatementBuilder ForEach(final StatementBuilder<? extends LocalDeclaration> elementVariable) {
		return new ForeachStatementBuilder(elementVariable);
	}
	
	public static IfStatementBuilder If(final ExpressionBuilder<? extends Expression> condition) {
		return new IfStatementBuilder(condition);
	}
	
	public static LocalDeclarationBuilder LocalDef(final ExpressionBuilder<? extends TypeReference> type, final String name) {
		return new LocalDeclarationBuilder(type, name);
	}
	
	public static NameReferenceBuilder Name(final String name) {
		return new NameReferenceBuilder(name);
	}
	
	public static AllocationExpressionBuilder New(final ExpressionBuilder<? extends TypeReference> type) {
		return new AllocationExpressionBuilder(type);
	}
	
	public static QualifiedAllocationExpressionBuilder New(final ExpressionBuilder<? extends TypeReference> type, final ASTNodeBuilder<? extends TypeDeclaration> anonymousType) {
		return new QualifiedAllocationExpressionBuilder(type, anonymousType);
	}
	
	public static UnaryExpressionBuilder Not(final ExpressionBuilder<? extends Expression> condition) {
		return new UnaryExpressionBuilder(OperatorIds.NOT, condition);
	}
	
	public static MagicLiteralBuilder Null() {
		return new MagicLiteralBuilder(null);
	}
	
	public static MethodDeclarationBuilder MethodDef(final ExpressionBuilder<? extends TypeReference> returnType, final String name) {
		return new MethodDeclarationBuilder(returnType, name);
	}
	
	public static MethodBindingMethodDeclarationBuilder MethodDef(final MethodBinding abstractMethod) {
		return new MethodBindingMethodDeclarationBuilder(abstractMethod);
	}
	
	public static NumberLiteralBuilder Number(final Number number) {
		return new NumberLiteralBuilder(number);
	}
	
	public static CharLiteralBuilder Char(final String character) {
		return new CharLiteralBuilder(character);
	}
	
	public static ReturnStatementBuilder Return() {
		return new ReturnStatementBuilder();
	}
	
	public static ReturnStatementBuilder Return(final ExpressionBuilder<? extends Expression> expression) {
		return new ReturnStatementBuilder(expression);
	}
	
	public static DefaultReturnStatementBuilder ReturnDefault() {
		return new DefaultReturnStatementBuilder();
	}
	
	public static StringLiteralBuilder String(final String value) {
		return new StringLiteralBuilder(value);
	}
	
	public static ThisReferenceBuilder This() {
		return new ThisReferenceBuilder();
	}
	
	public static ThisReferenceBuilder This(final ExpressionBuilder<? extends TypeReference> type) {
		return new ThisReferenceBuilder(type);
	}
	
	public static ThrowStatementBuilder Throw(final ExpressionBuilder<? extends Expression> init) {
		return new ThrowStatementBuilder(init);
	}
	
	public static MagicLiteralBuilder True() {
		return new MagicLiteralBuilder(true);
	}
	
	public static TryStatementBuilder Try(final StatementBuilder<? extends org.eclipse.jdt.internal.compiler.ast.Block> tryBlock) {
		return new TryStatementBuilder(tryBlock);
	}
	
	public static TypeReferenceBuilder Type(final Class<?> clazz) {
		return new TypeReferenceBuilder(clazz.getName());
	}
	
	public static TypeReferenceBuilder Type(final String typeName) {
		return new TypeReferenceBuilder(typeName);
	}
	
	public static TypeBindingTypeReferenceBuilder Type(final TypeBinding typeBinding) {
		return new TypeBindingTypeReferenceBuilder(typeBinding);
	}
	
	public static TypeReferenceWrapper Type(final TypeReference typeReference) {
		return new TypeReferenceWrapper(typeReference);
	}
	
	public static TypeParameterBuilder TypeParam(final String name) {
		return new TypeParameterBuilder(name);
	}
	
	public static WhileStatementBuilder While(final ExpressionBuilder<? extends Expression> condition) {
		return new WhileStatementBuilder(condition);
	}
}
