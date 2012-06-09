/*
 * Copyright © 2011-2012 Philipp Eichhorn
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

import java.util.List;

import lombok.AccessLevel;
import lombok.core.LombokNode;

public interface IMethod<TYPE_TYPE extends IType<?, ?, ?, ?, ?, ?>, LOMBOK_NODE_TYPE extends LombokNode<?, ?, ?>, AST_BASE_TYPE, AST_METHOD_DECL_TYPE> {
	public <T extends AST_BASE_TYPE> T build(Node<?> node);

	public <T extends AST_BASE_TYPE> T build(Node<?> node, Class<T> extectedType);

	public <T extends AST_BASE_TYPE> List<T> build(List<? extends Node<?>> nodes);

	public <T extends AST_BASE_TYPE> List<T> build(List<? extends Node<?>> nodes, Class<T> extectedType);

	public TypeRef returns();

	public TypeRef boxedReturns();

	public boolean returns(Class<?> clazz);

	public boolean returns(final String typeName);

	public void replaceReturnType(final TypeRef returnType);

	public void replaceReturns(Statement<?> replacement);

	public void replaceVariableName(String oldName, String newName);

	public void replaceBody(Statement<?>... statements);

	public void replaceBody(List<Statement<?>> statements);

	public void replaceBody(final Block body);

	public void forceQualifiedThis();

	public AccessLevel accessLevel();

	public boolean isSynchronized();

	public boolean isStatic();

	public boolean isConstructor();

	public boolean isAbstract();

	public boolean isEmpty();

	public AST_METHOD_DECL_TYPE get();

	public LOMBOK_NODE_TYPE node();

	public LOMBOK_NODE_TYPE getAnnotation(Class<? extends java.lang.annotation.Annotation> clazz);

	public LOMBOK_NODE_TYPE getAnnotation(final String typeName);

	public boolean hasNonFinalArgument();

	public boolean hasArguments();

	public String name();

	public void makePrivate();

	public void makePackagePrivate();

	public void makeProtected();

	public void makePublic();

	public void rebuild();

	public TYPE_TYPE surroundingType();

	public List<Statement<?>> statements();

	public List<Annotation> annotations();

	public List<Argument> arguments(ArgumentStyle... style);

	public List<TypeParam> typeParameters();

	public List<TypeRef> thrownExceptions();

	public enum ArgumentStyle {
		INCLUDE_ANNOTATIONS, BOXED_TYPES;
	}
}
