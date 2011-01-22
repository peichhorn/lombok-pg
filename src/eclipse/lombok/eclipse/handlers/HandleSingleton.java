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
package lombok.eclipse.handlers;

import static lombok.eclipse.handlers.EclipseHandlerUtil.*;
import static lombok.eclipse.handlers.EclipseNodeBuilder.setGeneratedByAndCopyPos;
import static org.eclipse.jdt.core.dom.Modifier.*;

import lombok.Singleton;
import lombok.core.AnnotationValues;

import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.mangosdk.spi.ProviderFor;

@ProviderFor(EclipseAnnotationHandler.class)
public class HandleSingleton implements EclipseAnnotationHandler<Singleton> {
	@Override public boolean handle(AnnotationValues<Singleton> annotation, Annotation source, EclipseNode annotationNode) {
		EclipseNode typeNode = annotationNode.up();
		TypeDeclaration type = null;
		switch (typeNode.getKind()) {
		case TYPE:
			if (typeNode.get() instanceof TypeDeclaration) type = (TypeDeclaration) typeNode.get();
			int modifiers = type == null ? 0 : type.modifiers;
			boolean notAClass = (modifiers & (ClassFileConstants.AccInterface | ClassFileConstants.AccAnnotation | ClassFileConstants.AccEnum)) != 0;
			
			if (type == null || notAClass) {
				annotationNode.addError("@Singleton is legal only on classes.");
				return true;
			}
			if (type.superclass != null) {
				annotationNode.addError("@Singleton works only on concrete classes.");
				return true;
			}
			if (hasMultiArgumentConstructor(type)) {
				annotationNode.addError("@Singleton works only on classes with default or no argument constructor.");
				return true;
			}
			break;
		default:
			annotationNode.addError("@Singleton is legal only on types.");
			return true;
		}
		
		type.modifiers |= 0x00004000; // Modifier.ENUM
		replaceConstructorVisibility(type);
		
		AllocationExpression initialization = new AllocationExpression();
		setGeneratedByAndCopyPos(initialization, source);
		
		FieldDeclaration field = new FieldDeclaration("INSTANCE".toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(field, source);
		field.initialization = initialization;
		initialization.enumConstant = field;
		
		injectField(typeNode, field);
		
		typeNode.rebuild();
		
		return true;
	}
	
	private void replaceConstructorVisibility(TypeDeclaration type) {
		if (type.methods != null) for (AbstractMethodDeclaration def : type.methods) {
			if (def instanceof ConstructorDeclaration) {
				def.modifiers &= ~(PUBLIC | PROTECTED);
			}
		}
	}
	
	private boolean hasMultiArgumentConstructor(TypeDeclaration type) {
		if (type.methods != null) for (AbstractMethodDeclaration def : type.methods) {
			if (def instanceof ConstructorDeclaration) {
				if ((def.arguments != null) && (def.arguments.length > 0)) {
					return true;
				}
			}
		}
		return false;
	}
}
