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

import static lombok.javac.handlers.Javac.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.util.ArrayList;
import java.util.List;

import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

import lombok.Validate;
import lombok.core.handlers.IParameterValidator;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacMethod;

public class JavacParameterValidator implements IParameterValidator<JavacMethod> {
	@Override
	public List<lombok.ast.Statement<?>> validateParameterOf(final JavacMethod method) {
		deleteImport(method.node(), Validate.class);
		for (ValidationStrategy validationStrategy : ValidationStrategy.IN_ORDER) {
			deleteImport(method.node(), validationStrategy.getType());
		}
		final List<lombok.ast.Statement<?>> validateStatements = new ArrayList<lombok.ast.Statement<?>>();
		int argumentIndex = 0;
		for (JCVariableDecl argument : method.get().params) {
			final String argumentName = argument.name.toString();
			argumentIndex++;
			for (ValidationStrategy validationStrategy : ValidationStrategy.IN_ORDER) {
				final JCAnnotation ann = getAnnotation(validationStrategy.getType(), argument.mods);
				if (ann == null) continue;
				final JavacNode annotationNode = method.node().getNodeFor(ann);
				final java.lang.annotation.Annotation annotation = createAnnotation(validationStrategy.getType(), annotationNode).getInstance();
				validateStatements.addAll(validationStrategy.getStatementsFor(argumentName, argumentIndex, annotation));
				argument.mods.annotations = remove(argument.mods.annotations, ann);
				break;
			}
			
		}
		return validateStatements;
	}
}
