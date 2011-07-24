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

import java.util.List;

import lombok.core.LombokNode;

public interface IType<METHOD_TYPE extends IMethod<?, ?, ?, ?>, LOMBOK_NODE_TYPE extends LombokNode<?, ?, ?>, NATIVE_AST_BASE_TYPE, NATIVE_AST_TYPE_DECL_TYPE, NATIVE_AST_METHOD_DECL_TYPE> {
	public <T extends NATIVE_AST_BASE_TYPE> T build(Node node);

	public <T extends NATIVE_AST_BASE_TYPE> T build(Node node, Class<T> extectedType);

	public <T extends NATIVE_AST_BASE_TYPE> List<T> build(List<? extends Node> nodes);

	public <T extends NATIVE_AST_BASE_TYPE> List<T> build(List<? extends Node> nodes, Class<T> extectedType);

	public boolean isInterface();

	public boolean isEnum();

	public boolean isAnnotation();

	public boolean hasSuperClass();

	public <T extends IType<?, ?, ?, ?, ?>> T memberType(String typeName);

	public List<METHOD_TYPE> methods();

	public boolean hasMultiArgumentConstructor();

	public NATIVE_AST_TYPE_DECL_TYPE get();

	public LOMBOK_NODE_TYPE node();

	public void injectField(FieldDecl fieldDecl);

	public void injectField(EnumConstant enumConstant);

	public NATIVE_AST_METHOD_DECL_TYPE injectMethod(MethodDecl methodDecl);

	public NATIVE_AST_METHOD_DECL_TYPE injectConstructor(ConstructorDecl constructorDecl);

	public void injectType(ClassDecl typeDecl);

	public void removeMethod(METHOD_TYPE method);

	public String name();

	public List<TypeRef> typeParameters();

	public boolean hasField(String fieldName);

	public boolean hasMethod(String methodName);
	
	public void makeEnum();

	public void makePrivate();

	public void makePackagePrivate();

	public void makeProtected();

	public void makePublic();

	public void rebuild();
}
