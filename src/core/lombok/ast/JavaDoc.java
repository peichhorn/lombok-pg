/*
 * Copyright © 2012 Philipp Eichhorn
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

import java.util.*;
import lombok.*;

@RequiredArgsConstructor
@Getter
public class JavaDoc extends Node<JavaDoc> {
	private final Map<String, String> argumentReferences = new HashMap<String, String>();
	private final Map<String, String> paramTypeReferences = new HashMap<String, String>();
	private final Map<TypeRef, String> exceptionReferences = new HashMap<TypeRef, String>();
	private final String message;
	private String returnMessage;

	public JavaDoc() {
		this(null);
	}

	public JavaDoc withTypeParameter(final String typeParameter) {
		return withTypeParameter(typeParameter, "");
	}

	public JavaDoc withTypeParameter(final String typeParameter, final String message) {
		paramTypeReferences.put(typeParameter, message);
		return this;
	}

	public JavaDoc withArgument(final String argument) {
		return withArgument(argument, "");
	}

	public JavaDoc withArgument(final String argument, final String message) {
		argumentReferences.put(argument, message);
		return this;
	}

	public JavaDoc withException(final TypeRef exceptionRef) {
		return withException(exceptionRef, "");
	}

	public JavaDoc withException(final TypeRef exceptionRef, final String message) {
		exceptionReferences.put(child(exceptionRef), message);
		return this;
	}

	public JavaDoc withReturnMessage(final String returnMessage) {
		this.returnMessage = returnMessage;
		return this;
	}

	@Override
	public <RETURN_TYPE, PARAMETER_TYPE> RETURN_TYPE accept(final ASTVisitor<RETURN_TYPE, PARAMETER_TYPE> v, final PARAMETER_TYPE p) {
		return v.visitJavaDoc(this, p);
	}
}
