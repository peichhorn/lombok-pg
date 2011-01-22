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
package lombok.eclipse.agent;

import static lombok.eclipse.Eclipse.makeType;
import static lombok.eclipse.handlers.EclipseHandlerUtil.injectMethod;
import static lombok.eclipse.handlers.EclipseNodeBuilder.setGeneratedByAndCopyPos;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.CharLiteral;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.jdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.jdt.internal.compiler.ast.FloatLiteral;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.Literal;
import org.eclipse.jdt.internal.compiler.ast.LongLiteral;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;

import lombok.AutoGenMethodStub;
import lombok.eclipse.EclipseAST;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.TransformEclipseAST;
import lombok.patcher.*;
import lombok.patcher.scripts.ScriptBuilder;

// TODO scan for lombok annotations that come after @AutoGenMethodStub and print a warning that @AutoGenMethodStub
// should be the last annotation to avoid major issues, once again.. curve ball
public class PatchAutoGenMethodStub {

	static void addPatches(ScriptManager sm, boolean ecj) {
		String	MethodVerifier = "org.eclipse.jdt.internal.compiler.lookup.MethodVerifier",
				MethodBinding = "org.eclipse.jdt.internal.compiler.lookup.MethodBinding",
				MethodBindings = "org.eclipse.jdt.internal.compiler.lookup.MethodBinding[]",
				SourceTypeBinding = "org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding",
				TypeDeclaration = "org.eclipse.jdt.internal.compiler.ast.TypeDeclaration",
				MethodDeclaration = "org.eclipse.jdt.internal.compiler.ast.MethodDeclaration",
				ProblemReporter = "org.eclipse.jdt.internal.compiler.problem.ProblemReporter";
		sm.addScript(ScriptBuilder.replaceMethodCall()
				.target(new MethodTarget(MethodVerifier, "checkAbstractMethod", "void", MethodBinding))
				.methodToReplace(new Hook(TypeDeclaration, "addMissingAbstractMethodFor", MethodDeclaration, MethodBinding))
				.replacementMethod(new Hook(PatchAutoGenMethodStub.class.getName(), "addMissingAbstractMethodFor", MethodDeclaration, TypeDeclaration, MethodBinding))
				.build());
		sm.addScript(ScriptBuilder.replaceMethodCall()
				.target(new MethodTarget(MethodVerifier, "checkAbstractMethod", "void", MethodBinding))
				.methodToReplace(new Hook(ProblemReporter, "abstractMethodMustBeImplemented", "void", SourceTypeBinding, MethodBinding))
				.replacementMethod(new Hook(PatchAutoGenMethodStub.class.getName(), "abstractMethodMustBeImplemented", "void", ProblemReporter, SourceTypeBinding, MethodBinding))
				.build());
		sm.addScript(ScriptBuilder.replaceMethodCall()
				.target(new MethodTarget(MethodVerifier, "checkInheritedMethods", "void", MethodBindings, "int"))
				.methodToReplace(new Hook(TypeDeclaration, "addMissingAbstractMethodFor", MethodDeclaration, MethodBinding))
				.replacementMethod(new Hook(PatchAutoGenMethodStub.class.getName(), "addMissingAbstractMethodFor", MethodDeclaration, TypeDeclaration, MethodBinding))
				.build());
		sm.addScript(ScriptBuilder.replaceMethodCall()
				.target(new MethodTarget(MethodVerifier, "checkInheritedMethods", "void", MethodBindings, "int"))
				.methodToReplace(new Hook(ProblemReporter, "abstractMethodMustBeImplemented", "void", SourceTypeBinding, MethodBinding))
				.replacementMethod(new Hook(PatchAutoGenMethodStub.class.getName(), "abstractMethodMustBeImplemented", "void", ProblemReporter, SourceTypeBinding, MethodBinding))
				.build());
	}
	
	private static boolean issueWasFixed = false;
	
	public static MethodDeclaration addMissingAbstractMethodFor(TypeDeclaration typeDeclaration, MethodBinding abstractMethod) {
		Annotation[] annotations = typeDeclaration.annotations;
		if (annotations != null) for (Annotation annotation : annotations) {
			if (annotation.type == null) continue;
			TypeBinding tb = annotation.resolvedType;
			if (tb == null) tb = annotation.type.resolveType(typeDeclaration.initializerScope);
			if (new String(tb.readableName()).equals(AutoGenMethodStub.class.getName())) {
				CompilationUnitDeclaration cud = typeDeclaration.scope.compilationUnitScope().referenceContext;
				EclipseAST astNode = TransformEclipseAST.getAST(cud, true);
				EclipseNode typeNode = astNode.get(typeDeclaration);
				MethodDeclaration method = new PatchAutoGenMethodStub().handle(typeNode, abstractMethod, annotation);
				issueWasFixed = true;
				return method;
			}
		}
		return typeDeclaration.addMissingAbstractMethodFor(abstractMethod);
	}
	
	public static void abstractMethodMustBeImplemented(ProblemReporter problemReporter, SourceTypeBinding type, MethodBinding abstractMethod) {
		if (issueWasFixed) {
			issueWasFixed = false;
		} else {
			problemReporter.abstractMethodMustBeImplemented(type, abstractMethod);
		}
	}
	
	public MethodDeclaration handle(EclipseNode typeNode, MethodBinding abstractMethod, ASTNode source) {
		TypeDeclaration type = (TypeDeclaration)typeNode.get();
		MethodDeclaration methodStub = new MethodDeclaration(type.compilationResult);
		setGeneratedByAndCopyPos(methodStub, source);
		methodStub.selector = abstractMethod.selector;
		methodStub.modifiers = (abstractMethod.getAccessFlags() ^ ClassFileConstants.AccAbstract) | ExtraCompilerModifiers.AccImplementing;
		methodStub.bits |= ASTNode.Bit24; // Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
		if (abstractMethod.parameters != null && abstractMethod.parameters.length > 0) {
			methodStub.arguments = new Argument[abstractMethod.parameters.length];
			for (int i = 0; i < methodStub.arguments.length; i++) {
				String argName = "arg" + i;
				methodStub.arguments[i] = new Argument(
						argName.toCharArray(), 0,
						makeType(abstractMethod.parameters[i], source, false),
						ClassFileConstants.AccFinal);
				setGeneratedByAndCopyPos(methodStub.arguments[i], source);
			}
		}
		if (abstractMethod.returnType != TypeBinding.VOID) {
			methodStub.statements = new Statement[] {new ReturnStatement(getDefaultValue(abstractMethod.returnType, source), 0, 0)};
		}
		methodStub.returnType = makeType(abstractMethod.returnType, source, false);
		if (abstractMethod.thrownExceptions != null && abstractMethod.thrownExceptions.length > 0) {
			methodStub.thrownExceptions = new TypeReference[abstractMethod.thrownExceptions.length];
			for (int i = 0; i < methodStub.thrownExceptions.length; i++) {
				methodStub.thrownExceptions[i] = makeType(abstractMethod.thrownExceptions[i], source, false);
			}
		}
		if (abstractMethod.typeVariables != null && abstractMethod.typeVariables.length > 0) {
			methodStub.typeParameters = new TypeParameter[abstractMethod.typeVariables.length];
			for (int i = 0; i < methodStub.typeParameters.length; i++) {
				methodStub.typeParameters[i] = new TypeParameter();
				setGeneratedByAndCopyPos(methodStub.typeParameters[i], source);
				methodStub.typeParameters[i].name = abstractMethod.typeVariables[i].sourceName;
				ReferenceBinding super1 = abstractMethod.typeVariables[i].superclass;
				ReferenceBinding[] super2 = abstractMethod.typeVariables[i].superInterfaces;
				if (super2 == null) super2 = new ReferenceBinding[0];
				if (super1 != null || super2.length > 0) {
					int offset = super1 == null ? 0 : 1;
					methodStub.typeParameters[i].bounds = new TypeReference[super2.length + offset - 1];
					if (super1 != null) methodStub.typeParameters[i].type = makeType(super1, source, false);
					else methodStub.typeParameters[i].type = makeType(super2[0], source, false);
					int ctr = 0;
					for (int j = (super1 == null) ? 1 : 0; j < super2.length; j++) {
						methodStub.typeParameters[i].bounds[ctr] = makeType(super2[j], source, false);
						methodStub.typeParameters[i].bounds[ctr++].bits |= ASTNode.IsSuperType;
					}
				}
			}
		}
		injectMethod(typeNode, methodStub);
		
		SourceTypeBinding sourceType = type.scope.referenceContext.binding;
		MethodScope methodScope = new MethodScope(type.scope, methodStub, false);
		SourceTypeBinding declaringClass = methodScope.referenceType().binding;
		MethodBinding methodBinding = new MethodBinding(methodStub.modifiers, methodStub.selector, abstractMethod.returnType, abstractMethod.parameters, abstractMethod.thrownExceptions, declaringClass);
		methodStub.binding = methodBinding;
		methodStub.scope = methodScope;

		Argument[] argTypes = methodStub.arguments;
		int argLength = argTypes == null ? 0 : argTypes.length;
		if ((argLength > 0) && argTypes[--argLength].isVarArgs())
			methodBinding.modifiers |= ClassFileConstants.AccVarargs;

		TypeParameter[] typeParameters = methodStub.typeParameters();
		if (typeParameters != null) {
			methodBinding.typeVariables = methodScope.createTypeVariables(typeParameters, methodStub.binding);
			methodBinding.modifiers |= ExtraCompilerModifiers.AccGenericSignature;
		}
		
		MethodBinding[] allMethods = new MethodBinding[sourceType.methods().length + 1];
		System.arraycopy(sourceType.methods(), 0, allMethods, 0, sourceType.methods().length);
		allMethods[sourceType.methods().length] = methodBinding;
		sourceType.setMethods(allMethods);
		 
		return methodStub;
	}
	
	private static Literal getDefaultValue(TypeBinding binding, ASTNode source) {
		Literal literal = new NullLiteral(0, 0);
		if (binding.isBaseType()) {
			if (binding == BaseTypeBinding.INT) {
				literal = new IntLiteral("0".toCharArray(), 0, 0);
			} else if (binding == BaseTypeBinding.BYTE) {
				literal = new IntLiteral("0".toCharArray(), 0, 0);
			} else if (binding == BaseTypeBinding.SHORT) {
				literal = new IntLiteral("0".toCharArray(), 0, 0);
			} else if (binding == BaseTypeBinding.CHAR) {
				literal = new CharLiteral("".toCharArray(), 0, 0);
			} else if (binding == BaseTypeBinding.LONG) {
				literal = new LongLiteral("0L".toCharArray(), 0, 0);
			} else if (binding == BaseTypeBinding.FLOAT) {
				literal = new FloatLiteral("0.0f".toCharArray(), 0, 0);
			} else if (binding == BaseTypeBinding.DOUBLE) {
				literal = new DoubleLiteral("0.0d".toCharArray(), 0, 0);
			} else if (binding == BaseTypeBinding.BOOLEAN) {
				literal = new FalseLiteral(0, 0);
			}
		}
		setGeneratedByAndCopyPos(literal, source);
		return literal;
	}
}