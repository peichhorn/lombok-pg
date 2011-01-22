/*
 * Copyright © 2010-2011 Philipp Eichhorn
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

import static lombok.javac.handlers.JavacHandlerUtil.*;
import static lombok.javac.handlers.JavacTreeBuilder.*;

import java.util.HashSet;
import java.util.Set;

import lombok.AutoGenMethodStub;
import lombok.core.AnnotationValues;
//import lombok.javac.DeleteLombokAnnotations;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;

/**
 * Handles the {@code lombok.AutoGenMethodStub} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleAutoGenMethodStub implements JavacAnnotationHandler<AutoGenMethodStub> {
	@Override public boolean isResolutionBased() {
		return true;
	}
	
	// TODO scan for lombok annotations that come after @AutoGenMethodStub and print a warning that @AutoGenMethodStub
	// should be the last annotation to avoid major issues, once again.. curve ball
	@Override public boolean handle(AnnotationValues<AutoGenMethodStub> annotation, JCAnnotation source, JavacNode annotationNode) {
		markAnnotationAsProcessed(annotationNode, AutoGenMethodStub.class);
		JavacNode typeNode = annotationNode.up();
		
		JCClassDecl typeDecl = null;
		if (typeNode.get() instanceof JCClassDecl) typeDecl = (JCClassDecl)typeNode.get();
		long flags = typeDecl == null ? 0 : typeDecl.mods.flags;
		boolean notAClass = (flags & (Flags.INTERFACE | Flags.ANNOTATION)) != 0;
		if (typeDecl == null || notAClass) {
			annotationNode.addError("@AutoGenMethodStub is legal only on classes and enums.");
			return true;
		}
		
		final Set<String> potentiallyFixedIssues =  new HashSet<String>();
		
		// try find the first issue
		MethodSymbol methodSymbol = getFirstUndefinedMethod(typeNode);
		
		while ((methodSymbol != null) && (!potentiallyFixedIssues.contains(methodSymbol.toString()))) {
			potentiallyFixedIssues.add(methodSymbol.toString());
			method(typeNode, methodSymbol).withDefaultReturnStatement().injectWithMethodSymbol(methodSymbol);
			// try to fix the next issue
			methodSymbol = getFirstUndefinedMethod(typeNode);
		}
		
		typeNode.rebuild();
		return true;
	}
}