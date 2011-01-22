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

import java.util.Collection;

import lombok.AccessLevel;
import lombok.FluentSetter;
import lombok.Setter;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.handlers.TransformationsUtil;
import lombok.javac.Javac;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.JavacHandlerUtil.FieldAccess;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

/**
 * Handles the {@code lombok.FluentSetter} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleFluentSetter implements JavacAnnotationHandler<FluentSetter> {
	@Override public boolean isResolutionBased() {
		return false;
	}
	
	@Override public boolean handle(AnnotationValues<FluentSetter> annotation, JCAnnotation ast, JavacNode annotationNode) {
		Collection<JavacNode> fields = annotationNode.upFromAnnotationToFields();
		markAnnotationAsProcessed(annotationNode, FluentSetter.class);
		deleteImportFromCompilationUnit(annotationNode, "lombok.AccessLevel");
		FluentSetter annotationInstance = annotation.getInstance();
		AccessLevel level = annotationInstance.value();
		return handle(ast, annotationNode, level, fields, true);
	}
	
	public boolean generateSetterForType(JavacNode typeNode, JavacNode errorNode, AccessLevel level, boolean checkForTypeLevelSetter, boolean fluent) {
		if (checkForTypeLevelSetter) {
			if (typeNode != null) for (JavacNode child : typeNode.down()) {
				if (child.getKind() == Kind.ANNOTATION) {
					if (Javac.annotationTypeMatches(Setter.class, child)) {
						//The annotation will make it happen, so we can skip it.
						return true;
					}
				}
			}
		}
		
		JCClassDecl typeDecl = null;
		if (typeNode.get() instanceof JCClassDecl) typeDecl = (JCClassDecl) typeNode.get();
		long modifiers = typeDecl == null ? 0 : typeDecl.mods.flags;
		boolean notAClass = (modifiers & (Flags.INTERFACE | Flags.ANNOTATION | Flags.ENUM)) != 0;
		
		if (typeDecl == null || notAClass) {
			errorNode.addError("@Setter is only supported on a class or a field.");
			return false;
		}
		
		for (JavacNode field : typeNode.down()) {
			if (field.getKind() != Kind.FIELD) continue;
			JCVariableDecl fieldDecl = (JCVariableDecl) field.get();
			//Skip fields that start with $
			if (fieldDecl.name.toString().startsWith("$")) continue;
			//Skip static fields.
			if ((fieldDecl.mods.flags & Flags.STATIC) != 0) continue;
			//Skip final fields.
			if ((fieldDecl.mods.flags & Flags.FINAL) != 0) continue;
			
			generateSetterForField(field, errorNode.get(), level, fluent, List.<JCExpression>nil(), List.<JCExpression>nil());
		}
		return true;
	}
	
	/**
	 * Generates a setter on the stated field.
	 * 
	 * Used by {@link HandleData}.
	 * 
	 * The difference between this call and the handle method is as follows:
	 * 
	 * If there is a {@code lombok.Setter} annotation on the field, it is used and the
	 * same rules apply (e.g. warning if the method already exists, stated access level applies).
	 * If not, the setter is still generated if it isn't already there, though there will not
	 * be a warning if its already there. The default access level is used.
	 * 
	 * @param fieldNode The node representing the field you want a setter for.
	 * @param pos The node responsible for generating the setter (the {@code @Data} or {@code @Setter} annotation).
	 */
	public void generateSetterForField(JavacNode fieldNode, DiagnosticPosition pos, AccessLevel level, boolean fluent, List<JCExpression> onMethod, List<JCExpression> onParam) {
		for (JavacNode child : fieldNode.down()) {
			if (child.getKind() == Kind.ANNOTATION) {
				if (Javac.annotationTypeMatches(Setter.class, child)) {
					//The annotation will make it happen, so we can skip it.
					return;
				}
			}
		}
		createSetterForField(level, fieldNode, fieldNode, false, fluent, onMethod, onParam);
	}
	
	public boolean handle(JCAnnotation ast, JavacNode annotationNode, AccessLevel level, Collection<JavacNode> fields, boolean fluent) {
		if (level == AccessLevel.NONE) return true;

		JavacNode node = annotationNode.up();
		if (node == null) return false;
		List<JCExpression> onMethod = getAndRemoveAnnotationParameter(ast, "onMethod");
		List<JCExpression> onParam = getAndRemoveAnnotationParameter(ast, "onParam");
		if (node.getKind() == Kind.FIELD) {
			return createSetterForFields(level, fields, annotationNode, true, fluent, onMethod, onParam);
		}
		if (node.getKind() == Kind.TYPE) {
			if (!onMethod.isEmpty()) annotationNode.addError("'onMethod' is not supported for @Setter on a type.");
			if (!onParam.isEmpty()) annotationNode.addError("'onParam' is not supported for @Setter on a type.");			
			return generateSetterForType(node, annotationNode, level, false, fluent);
		}
		return false;
	}

	private boolean createSetterForFields(AccessLevel level, Collection<JavacNode> fieldNodes, JavacNode errorNode, boolean whineIfExists, boolean fluent,
			List<JCExpression> onMethod, List<JCExpression> onParam) {
		for (JavacNode fieldNode : fieldNodes) {
			createSetterForField(level, fieldNode, errorNode, whineIfExists, fluent, onMethod, onParam);
		}
		
		return true;
	}
	
	private boolean createSetterForField(AccessLevel level, JavacNode fieldNode, JavacNode errorNode, boolean whineIfExists, boolean fluent, 
			List<JCExpression> onMethod, List<JCExpression> onParam) {
		if (fieldNode.getKind() != Kind.FIELD) {
			fieldNode.addError("@Setter is only supported on a class or a field.");
			return true;
		}
		
		JCVariableDecl fieldDecl = (JCVariableDecl)fieldNode.get();

		String methodName = fluent ? fieldDecl.name.toString() : toSetterName(fieldDecl);
		
		switch (methodExists(methodName, fieldNode, false)) {
		case EXISTS_BY_LOMBOK:
			return true;
		case EXISTS_BY_USER:
			if (whineIfExists) errorNode.addWarning(
					String.format("Not generating %s(%s %s): A method with that name already exists",
					methodName, fieldDecl.vartype, fieldDecl.name));
			return true;
		default:
		case NOT_EXISTS:
			//continue with creating the setter
		}
		
		long access = toJavacModifier(level) | (fieldDecl.mods.flags & Flags.STATIC);

		injectMethod(fieldNode.up(), createSetter(access, fluent, fieldNode, fieldNode.getTreeMaker(), onMethod, onParam));
		
		return true;
	}

	private JCMethodDecl createSetter(long access, boolean fluent, JavacNode field, TreeMaker treeMaker, List<JCExpression> onMethod, List<JCExpression> onParam) {
		JCVariableDecl fieldDecl = (JCVariableDecl) field.get();
		
		JCExpression fieldRef = createFieldAccessor(treeMaker, field, FieldAccess.ALWAYS_FIELD);
		JCAssign assign = treeMaker.Assign(fieldRef, treeMaker.Ident(fieldDecl.name));
		
		List<JCStatement> statements;
		List<JCAnnotation> nonNulls = findAnnotations(field, TransformationsUtil.NON_NULL_PATTERN);
		List<JCAnnotation> nullables = findAnnotations(field, TransformationsUtil.NULLABLE_PATTERN);
		
		if (nonNulls.isEmpty()) {
			statements = List.<JCStatement>of(treeMaker.Exec(assign));
		} else {
			JCStatement nullCheck = generateNullCheck(treeMaker, field);
			if (nullCheck != null) statements = List.<JCStatement>of(nullCheck, treeMaker.Exec(assign));
			else statements = List.<JCStatement>of(treeMaker.Exec(assign));
		}
		
		if (fluent) {
			JCStatement returnStatement = treeMaker.Return(treeMaker.Ident(field.toName("this")));
			statements = statements.append(returnStatement);
		}
		
		JCBlock methodBody = treeMaker.Block(0, statements);
		Name methodName = fluent ? fieldDecl.name : field.toName(toSetterName(fieldDecl));
		List<JCAnnotation> annsOnParam = copyAnnotations(onParam);
		annsOnParam = annsOnParam.appendList(nonNulls).appendList(nullables);
		JCVariableDecl param = treeMaker.VarDef(treeMaker.Modifiers(Flags.FINAL, annsOnParam), fieldDecl.name, fieldDecl.vartype, null);
		//WARNING: Do not use field.getSymbolTable().voidType - that field has gone through non-backwards compatible API changes within javac1.6.
		JCExpression methodType;
		if (fluent) {
			JCClassDecl classNode = (JCClassDecl) field.up().get();
			
			if (!classNode.typarams.isEmpty()) {
				// Paramterized type
				List<JCExpression> typeArgs = List.nil();
				for (JCTypeParameter typeparam : classNode.typarams) {
					typeArgs = typeArgs.append(treeMaker.Ident(typeparam.name));
				}
				methodType = treeMaker.TypeApply(treeMaker.Ident(classNode.name), typeArgs);
			} else {
				methodType = treeMaker.Ident(classNode.name);
			}
			
			
		} else {
			methodType = treeMaker.TypeIdent(TypeTags.VOID);
		}

		
		List<JCTypeParameter> methodGenericParams = List.nil();
		List<JCVariableDecl> parameters = List.of(param);
		List<JCExpression> throwsClauses = List.nil();
		JCExpression annotationMethodDefaultValue = null;
		
		return treeMaker.MethodDef(treeMaker.Modifiers(access, copyAnnotations(onMethod)), methodName, methodType,
				methodGenericParams, parameters, throwsClauses, methodBody, annotationMethodDefaultValue);
	}
}
