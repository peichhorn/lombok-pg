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

import lombok.core.AnnotationValues;
import lombok.core.LombokNode;

public interface IType<METHOD_TYPE extends IMethod<?, ?, ?, ?>, FIELD_TYPE extends IField<?, ?, ?, ?>, LOMBOK_NODE_TYPE extends LombokNode<?, ?, ?>, AST_BASE_TYPE, AST_TYPE_DECL_TYPE, AST_METHOD_DECL_TYPE> {

	public ITypeEditor<METHOD_TYPE, AST_BASE_TYPE, AST_TYPE_DECL_TYPE, AST_METHOD_DECL_TYPE> editor();

	public boolean isInterface();

	public boolean isEnum();

	public boolean isAnnotation();

	public boolean isClass();

	public boolean hasSuperClass();

	public <T extends IType<?, ?, ?, ?, ?, ?>> T memberType(String typeName);

	public <T extends IType<?, ?, ?, ?, ?, ?>> T surroundingType();

	public List<METHOD_TYPE> methods();

	public List<FIELD_TYPE> fields();

	public boolean hasMultiArgumentConstructor();

	public AST_TYPE_DECL_TYPE get();

	public LOMBOK_NODE_TYPE node();

	public <A extends java.lang.annotation.Annotation> AnnotationValues<A> getAnnotationValue(Class<A> expectedType);

	public LOMBOK_NODE_TYPE getAnnotation(Class<? extends java.lang.annotation.Annotation> clazz);

	public LOMBOK_NODE_TYPE getAnnotation(String typeName);

	public String name();

	public String qualifiedName();

	public List<TypeRef> typeArguments();

	public List<TypeParam> typeParameters();

	public List<Annotation> annotations();

	public boolean hasField(String fieldName);

	public boolean hasMethod(String methodName, TypeRef... argumentTypes);

	/** be aware, this method works with type resolution */
	public boolean hasMethodIncludingSupertypes(String methodName, TypeRef... argumentTypes);
}
