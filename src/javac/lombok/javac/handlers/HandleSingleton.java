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

import static lombok.core.util.ErrorMessages.*;
import static lombok.javac.handlers.Javac.typeDeclFiltering;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static lombok.javac.handlers.JavacTreeBuilder.*;
import static com.sun.tools.javac.code.Flags.*;
import lombok.Singleton;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.util.List;

@ProviderFor(JavacAnnotationHandler.class)
public class HandleSingleton extends JavacAnnotationHandler<Singleton> {
	
	@Override public void handle(AnnotationValues<Singleton> annotation, JCAnnotation source, JavacNode annotationNode) {
		deleteAnnotationIfNeccessary(annotationNode, Singleton.class);

		if (isNoConcreteClass(annotationNode)) {
			return;
		}

		if (hasMultiArgumentConstructor(annotationNode)) {
			return;
		}
		
		JavacNode typeNode = annotationNode.up();
		JCClassDecl type = (JCClassDecl)typeNode.get();
		Singleton singleton = annotation.getInstance();
		String typeName = typeNode.getName();
		
		switch(singleton.style()) {
		case HOLDER: {
			String holderName = typeName + "Holder";
			clazz(typeNode, PRIVATE | STATIC, holderName) //
				.withField(field(typeNode, "private final static %s INSTANCE = new %s()", typeName, typeName).build()).inject(source);
			makeConstructorNonPublicAndNonProtected(type);
			method(typeNode, "public static %s getInstance() { return %s.INSTANCE; }", typeName, holderName).inject(source);
		break;
		}
		default:
		case ENUM: {
			type.mods.flags |= ENUM;
			makeConstructorNonPublicAndNonProtected(type);

			TreeMaker maker = typeNode.getTreeMaker();
			JCExpression typeRef = maker.Ident(type.name);
			List<JCExpression> nilExp = List.nil();
			JCNewClass init = maker.NewClass(null, nilExp, typeRef, nilExp, null);
			JCModifiers mods = maker.Modifiers(PUBLIC | STATIC | FINAL| ENUM);
			injectField(typeNode, lombok.javac.Javac.recursiveSetGeneratedBy(maker.VarDef(mods, typeNode.toName("INSTANCE"), typeRef, init), source));
			method(typeNode, "public static %s getInstance() { return INSTANCE; }", typeName).inject(source);
		}
		}

		typeNode.rebuild();
	}

	private boolean isNoConcreteClass(JavacNode annotationNode) {
		JavacNode typeNode = annotationNode.up();
		JCClassDecl typeDecl = typeDeclFiltering(typeNode, INTERFACE | ANNOTATION | ENUM);
		if (typeDecl == null) {
			annotationNode.addError(canBeUsedOnClassOnly(Singleton.class));
			return true;
		}
		if (typeDecl.extending != null) {
			annotationNode.addError(canBeUsedOnConcreteClassOnly(Singleton.class));
			return true;
		}
		return false;
	}

	private boolean hasMultiArgumentConstructor(JavacNode annotationNode) {
		JavacNode typeNode = annotationNode.up();
		JCClassDecl type = (JCClassDecl)typeNode.get();
		if (hasMultiArgumentConstructor(type)) {
			annotationNode.addError(requiresDefaultOrNoArgumentConstructor(Singleton.class));
			return true;
		}
		return false;
	}

	private void makeConstructorNonPublicAndNonProtected(JCClassDecl type) {
		for (JCTree def : type.defs) {
			if (isConstructor(def)) {
				((JCMethodDecl)def).mods.flags &= ~(PUBLIC | PROTECTED);
			}
		}
	}

	private boolean hasMultiArgumentConstructor(JCClassDecl type) {
		for (JCTree def : type.defs) {
			if (isConstructor(def)) {
				if (!((JCMethodDecl)def).params.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isConstructor(JCTree def) {
		if (def instanceof JCMethodDecl) {
			JCMethodDecl method = (JCMethodDecl)def;
			return method.name.contentEquals("<init>");
		}
		return false;
	}
}