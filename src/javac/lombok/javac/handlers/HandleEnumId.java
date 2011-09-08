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
package lombok.javac.handlers;

import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.handlers.EnumIdHandler;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacMethod;
import lombok.javac.handlers.ast.JavacType;

import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.EnumId} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleEnumId extends JavacAnnotationHandler<EnumId> {

	@Override public void handle(final AnnotationValues<EnumId> annotation, final JCAnnotation source, final JavacNode annotationNode) {
		deleteAnnotationIfNeccessary(annotationNode, EnumId.class);
		JavacNode fieldNode = annotationNode.up();
		if (fieldNode.getKind() != Kind.FIELD) {
			annotationNode.addError(canBeUsedOnFieldOnly(EnumId.class));
			return;
		}
		JCVariableDecl fieldDecl = (JCVariableDecl)fieldNode.get();
		new EnumIdHandler<JavacType, JavacMethod>(JavacType.typeOf(annotationNode, source), annotationNode).handle(string(fieldDecl.name), Type(fieldDecl.vartype), boxedType(fieldDecl.vartype));
	}

	private lombok.ast.TypeRef boxedType(JCExpression type) {
		lombok.ast.TypeRef boxedType = Type(type);
		if (type instanceof JCPrimitiveTypeTree) {
			final String name = type.toString();
			if ("int".equals(name)) {
				boxedType = Type(Integer.class);
			} else if ("char".equals(name)) {
				boxedType = Type(Character.class);
			} else {
				boxedType = Type("java.lang." + capitalize(name));
			}
		}
		return boxedType;
	}
}
