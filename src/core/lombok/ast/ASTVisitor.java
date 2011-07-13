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

public interface ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> {

	RETURN_TYPE visitAnnotation(Annotation node, PARAMETER_TYPE p);

	RETURN_TYPE visitArgument(Argument node, PARAMETER_TYPE p);

	RETURN_TYPE visitAssignment(Assignment node, PARAMETER_TYPE p);

	RETURN_TYPE visitBinary(Binary node, PARAMETER_TYPE p);

	RETURN_TYPE visitBlock(Block node, PARAMETER_TYPE p);

	RETURN_TYPE visitBooleanLiteral(BooleanLiteral node, PARAMETER_TYPE p);

	RETURN_TYPE visitCall(Call node, PARAMETER_TYPE p);

	RETURN_TYPE visitCase(Case node, PARAMETER_TYPE p);

	RETURN_TYPE visitCast(Cast node, PARAMETER_TYPE p);

	RETURN_TYPE visitCharLiteral(CharLiteral node, PARAMETER_TYPE p);

	RETURN_TYPE visitClassDecl(ClassDecl node, PARAMETER_TYPE p);

	RETURN_TYPE visitConstructorDecl(ConstructorDecl node, PARAMETER_TYPE p);

	RETURN_TYPE visitContinue(Continue node, PARAMETER_TYPE p);

	RETURN_TYPE visitDoWhile(DoWhile node, PARAMETER_TYPE p);

	RETURN_TYPE visitEnumConstant(EnumConstant node, PARAMETER_TYPE p);

	RETURN_TYPE visitEqual(Equal node, PARAMETER_TYPE p);

	RETURN_TYPE visitFieldDecl(FieldDecl node, PARAMETER_TYPE p);

	RETURN_TYPE visitFieldRef(FieldRef node, PARAMETER_TYPE p);

	RETURN_TYPE visitForeach(Foreach node, PARAMETER_TYPE p);

	RETURN_TYPE visitIf(If node, PARAMETER_TYPE p);

	RETURN_TYPE visitArrayRef(ArrayRef node, PARAMETER_TYPE p);

	RETURN_TYPE visitInstanceOf(InstanceOf node, PARAMETER_TYPE p);

	RETURN_TYPE visitLocalDecl(LocalDecl node, PARAMETER_TYPE p);

	RETURN_TYPE visitMethodDecl(MethodDecl node, PARAMETER_TYPE p);

	RETURN_TYPE visitNameRef(NameRef node, PARAMETER_TYPE p);

	RETURN_TYPE visitNew(New node, PARAMETER_TYPE p);

	RETURN_TYPE visitNewArray(NewArray node, PARAMETER_TYPE p);

	RETURN_TYPE visitNullLiteral(NullLiteral node, PARAMETER_TYPE p);

	RETURN_TYPE visitNumberLiteral(NumberLiteral node, PARAMETER_TYPE p);

	RETURN_TYPE visitReturn(Return node, PARAMETER_TYPE p);

	RETURN_TYPE visitReturnDefault(ReturnDefault node, PARAMETER_TYPE p);

	RETURN_TYPE visitStringLiteral(StringLiteral node, PARAMETER_TYPE p);

	RETURN_TYPE visitSwitch(Switch node, PARAMETER_TYPE p);

	RETURN_TYPE visitThis(This node, PARAMETER_TYPE p);

	RETURN_TYPE visitThrow(Throw node, PARAMETER_TYPE p);

	RETURN_TYPE visitTry(Try node, PARAMETER_TYPE p);

	RETURN_TYPE visitTypeParam(TypeParam node, PARAMETER_TYPE p);

	RETURN_TYPE visitTypeRef(TypeRef node, PARAMETER_TYPE p);

	RETURN_TYPE visitUnary(Unary node, PARAMETER_TYPE p);

	RETURN_TYPE visitWhile(While node, PARAMETER_TYPE p);

	RETURN_TYPE visitWildcard(Wildcard node, PARAMETER_TYPE p);

	RETURN_TYPE visitWrappedExpression(WrappedExpression node, PARAMETER_TYPE p);

	RETURN_TYPE visitWrappedMethodDecl(WrappedMethodDecl node, PARAMETER_TYPE p);

	RETURN_TYPE visitWrappedStatement(WrappedStatement node, PARAMETER_TYPE p);

	RETURN_TYPE visitWrappedTypeRef(WrappedTypeRef node, PARAMETER_TYPE p);
}
