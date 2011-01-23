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

import static lombok.core.util.ErrorMessages.canBeUsedOnClassOnly;
import static lombok.core.util.Names.*;
import static org.eclipse.jdt.core.dom.Modifier.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;
import static org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers.*;
import static lombok.eclipse.Eclipse.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;
import static lombok.eclipse.handlers.EclipseNodeBuilder.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.BuilderExtension;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseASTAdapter;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.Builder} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleBuilder implements EclipseAnnotationHandler<Builder> {
	private final static String OPTIONAL_DEF = "$OptionalDef";
	private final static String BUILDER = "$Builder";
	
	@Override public boolean handle(AnnotationValues<Builder> annotation, Annotation source, EclipseNode annotationNode) {
		EclipseNode typeNode = annotationNode.up();
		
		TypeDeclaration typeDecl = null;
		if (typeNode.get() instanceof TypeDeclaration) typeDecl = (TypeDeclaration) typeNode.get();
		int modifiers = typeDecl == null ? 0 : typeDecl.modifiers;
		boolean notAClass = (modifiers & (AccInterface | AccAnnotation | AccEnum)) != 0;
		
		if (typeDecl == null || notAClass) {
			annotationNode.addError(canBeUsedOnClassOnly(Builder.class));
			return true;
		}
		
		switch (methodExists("create", typeNode, false)) {
		case EXISTS_BY_LOMBOK:
			return true;
		case EXISTS_BY_USER:
			annotationNode.addWarning(String.format("Not generating 'public static %s create()' A method with that name already exists", BUILDER));
			return true;
		default:
		case NOT_EXISTS:
			//continue with creating the entrypoint
		}
		
		handleBuilder(new HandleBuilderDataCollector(typeNode, source, annotation.getInstance()).collect());
		return true;
	}
		
	private static void handleBuilder(IBuilderData builderData) {
		List<String> requiredFieldDefTypeNames = builderData.getRequiredFieldDefTypeNames();
		List<String> typeNames = new ArrayList<String>(requiredFieldDefTypeNames);
		typeNames.add(OPTIONAL_DEF);
		String fieldDefTypeName = builderData.getRequiredFields().isEmpty() ? OPTIONAL_DEF : requiredFieldDefTypeNames.get(0);
		
		createConstructor(builderData);
		createCreateMethod(builderData, fieldDefTypeName);
		List<AbstractMethodDeclaration> builderMethods = new ArrayList<AbstractMethodDeclaration>();
		createRequiredFieldInterfaces(builderData, builderMethods);
		createOptionalFieldInterface(builderData, builderMethods);
		createBuilder(builderData, typeNames, builderMethods);
	}
	
	private static void createConstructor(IBuilderData builderData) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();
		ConstructorBuilder builder = constructor(typeNode, source, PRIVATE, typeNode.getName()).withImplicitSuper().withParameter(BUILDER, "builder");
		for (FieldDeclaration field : builderData.getAllFields()) {
			String fieldName = new String(field.name);
			builder.withAssignStatement(fieldReference(source, thisReference(source), fieldName), fieldReference(source, nameReference(source, "builder"), fieldName));
		}
		builder.inject();
	}
	
	private static void createCreateMethod(IBuilderData builderData, String fieldDefTypeName) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();
		AllocationExpression newClassExp = new AllocationExpression();
		setGeneratedByAndCopyPos(newClassExp, source);
		newClassExp.type = typeReference(source, BUILDER);
		method(typeNode, source, STATIC | builderData.getCreateModifier(), fieldDefTypeName, "create").withReturnStatement(newClassExp).inject();
	}
	
	private static void createRequiredFieldInterfaces(IBuilderData builderData, List<AbstractMethodDeclaration> builderMethods) {
		List<FieldDeclaration> fields = builderData.getRequiredFields();
		if (!fields.isEmpty()) {
			EclipseNode typeNode = builderData.getTypeNode();
			ASTNode source = builderData.getSource();
			List<String> names = builderData.getRequiredFieldDefTypeNames();
			boolean createFieldExtension = true;
			FieldDeclaration field = fields.get(0);
			String name = names.get(0);
			for (int i = 1, iend = fields.size(); i < iend; i++) {
				List<AbstractMethodDeclaration> interfaceMethods = new ArrayList<AbstractMethodDeclaration>();
				createFluentSetter(builderData, names.get(i), field, interfaceMethods, builderMethods);
				if (createFieldExtension) {
					for (MethodDeclaration extension : builderData.getRequiredFieldExtensions()) {
						createExtension(builderData, extension, interfaceMethods, builderMethods);
					}
					createFieldExtension = false;
				}
				
				interfaze(typeNode, source, PUBLIC | STATIC, name).withMethods(interfaceMethods).inject();
				field = fields.get(i);
				name = names.get(i);
			}
			List<AbstractMethodDeclaration> interfaceMethods = new ArrayList<AbstractMethodDeclaration>();
			createFluentSetter(builderData, OPTIONAL_DEF, field, interfaceMethods, builderMethods);
			
			interfaze(typeNode, source, PUBLIC | STATIC, name).withMethods(interfaceMethods).inject();
		}
	}

	private static void createOptionalFieldInterface(IBuilderData builderData, List<AbstractMethodDeclaration> builderMethods) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();
		TypeDeclaration typeDecl = (TypeDeclaration)typeNode.get();
		List<AbstractMethodDeclaration> interfaceMethods = new ArrayList<AbstractMethodDeclaration>();
		for (FieldDeclaration field : builderData.getOptionalFields()) {
			if (isInitializedMapOrCollection(field)) {
				if (builderData.generateConvenientMethods()) {
					if (isCollection(field)) {
						createCollectionSignaturesAndMethods(builderData, field, interfaceMethods, builderMethods);
					}  else if (isMap(field)) {
						createMapSignaturesAndMethods(builderData, field, interfaceMethods, builderMethods);
					}
				}
			} else {
				createFluentSetter(builderData, OPTIONAL_DEF, field, interfaceMethods, builderMethods);
			}
		}
		createBuildMethod(builderData, new String(typeDecl.name), interfaceMethods, builderMethods);
		
		for (String callMethod : builderData.getCallMethods()) {
			createMethodCall(builderData, callMethod, interfaceMethods, builderMethods);
		}
		
		for (MethodDeclaration extension : builderData.getOptionalFieldExtensions()) {
			createExtension(builderData, extension, interfaceMethods, builderMethods);
		}
		
		interfaze(typeNode, source, PUBLIC | STATIC, OPTIONAL_DEF).withMethods(interfaceMethods).inject();
	}
	
	private static void createExtension(IBuilderData builderData, MethodDeclaration extension, List<AbstractMethodDeclaration> interfaceMethods, List<AbstractMethodDeclaration> builderMethods) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();
		String methodName = new String(extension.selector);
		List<Argument> arguments = extension.arguments == null ? new ArrayList<Argument>() : Arrays.asList(extension.arguments);
		List<Annotation> annotations = extension.annotations == null ? new ArrayList<Annotation>() : Arrays.asList(extension.annotations);
		List<Statement> statements = extension.statements == null ? new ArrayList<Statement>() : Arrays.asList(extension.statements);
		builderMethods.add(method(typeNode, source, PUBLIC | AccImplementing, OPTIONAL_DEF, methodName).withParameters(arguments).withAnnotations(annotations)
			.withStatements(statements)
			.withReturnStatement(thisReference(source)).build());
		interfaceMethods.add(method(typeNode, source, PUBLIC, OPTIONAL_DEF, methodName)
			.withParameters(arguments).build());
	}
	
	private static void createFluentSetter(IBuilderData builderData, String typeName, FieldDeclaration field, List<AbstractMethodDeclaration> interfaceMethods,
			List<AbstractMethodDeclaration> builderMethods) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();
		String fieldName = new String(field.name);
		String methodName = camelCase(builderData.getPrefix(), fieldName);
		builderMethods.add(method(typeNode, source, PUBLIC | AccImplementing, typeName, methodName).withParameter(field.type, fieldName)
			.withAssignStatement(fieldReference(source, thisReference(source), fieldName), nameReference(source, fieldName))
			.withReturnStatement(thisReference(source)).build());
		interfaceMethods.add(method(typeNode, source, PUBLIC, typeName, methodName).withParameter(field.type, fieldName).build());
	}
	
	private static void createCollectionSignaturesAndMethods(IBuilderData builderData, FieldDeclaration field, List<AbstractMethodDeclaration> interfaceMethods, List<AbstractMethodDeclaration> builderMethods) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();
		
		TypeReference elementType = typeReference(source, "java.lang.Object");
		TypeReference collectionType = typeReference(source, "java.util.Collection");
		if (field.type instanceof ParameterizedQualifiedTypeReference) {
			ParameterizedQualifiedTypeReference typeRef = (ParameterizedQualifiedTypeReference)field.type;
			if ((typeRef.typeArguments != null)) {
				TypeReference[] typeArguments = typeRef.typeArguments[typeRef.typeArguments.length - 1];
				if ((typeArguments != null) && (typeArguments.length == 1)) {
					elementType = copyType(typeArguments[0], source);
					if (elementType instanceof Wildcard) {
						elementType = ((Wildcard)elementType).bound;
					}
					collectionType = addWildCards(source, typeRef, collectionType.getTypeName());;
				}
			}
		} else if (field.type instanceof ParameterizedSingleTypeReference) {
			ParameterizedSingleTypeReference typeRef = (ParameterizedSingleTypeReference)field.type;
			if ((typeRef.typeArguments != null) && (typeRef.typeArguments.length == 1)) {
				elementType = copyType(typeRef.typeArguments[0], source);
				if (elementType instanceof Wildcard) {
					elementType = ((Wildcard)elementType).bound;
				}
				collectionType = addWildCards(source, typeRef, collectionType.getTypeName());
			}
		}
		
		String fieldName = new String(field.name);
		
		{ // add
			String addMethodName = singular(camelCase(builderData.getPrefix(), fieldName));
			builderMethods.add(method(typeNode, source, PUBLIC | AccImplementing, OPTIONAL_DEF, addMethodName).withParameter(elementType, "arg0")
				.withStatement(methodCall(source, fieldReference(source, thisReference(source), fieldName), "add", nameReference(source, "arg0")))
				.withReturnStatement(thisReference(source)).build());
			interfaceMethods.add(method(typeNode, source, PUBLIC, OPTIONAL_DEF, addMethodName).withParameter(elementType, "arg0").build());
		}
		{ // addAll
			String addAllMethodName = camelCase(builderData.getPrefix(), fieldName);
			builderMethods.add(method(typeNode, source, PUBLIC | AccImplementing, OPTIONAL_DEF, addAllMethodName).withParameter(collectionType, "arg0")
				.withStatement(methodCall(source, fieldReference(source, thisReference(source), fieldName), "addAll", nameReference(source, "arg0")))
				.withReturnStatement(thisReference(source)).build());
			interfaceMethods.add(method(typeNode, source, PUBLIC, OPTIONAL_DEF, addAllMethodName).withParameter(collectionType, "arg0").build());
		}
	}
	
	private static void createMapSignaturesAndMethods(IBuilderData builderData, FieldDeclaration field, List<AbstractMethodDeclaration> interfaceMethods,
			List<AbstractMethodDeclaration> builderMethods) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();
		
		TypeReference keyType = typeReference(source, "java.lang.Object");
		TypeReference valueType = typeReference(source, "java.lang.Object");
		TypeReference mapType = typeReference(source, "java.util.Map");
		if (field.type instanceof ParameterizedQualifiedTypeReference) {
			ParameterizedQualifiedTypeReference typeRef = (ParameterizedQualifiedTypeReference)field.type;
			if ((typeRef.typeArguments != null)) {
				TypeReference[] typeArguments = typeRef.typeArguments[typeRef.typeArguments.length - 1];
				if ((typeArguments != null) && (typeArguments.length == 2)) {
					keyType = copyType(typeArguments[0], source);
					if (keyType instanceof Wildcard) {
						keyType = ((Wildcard)keyType).bound;
					}
					valueType = copyType(typeArguments[1], source);
					if (valueType instanceof Wildcard) {
						valueType = ((Wildcard)valueType).bound;
					}
					mapType = addWildCards(source, typeRef, mapType.getTypeName());
				}
			}
		} else if (field.type instanceof ParameterizedSingleTypeReference) {
			ParameterizedSingleTypeReference typeRef = (ParameterizedSingleTypeReference)field.type;
			if ((typeRef.typeArguments != null) && (typeRef.typeArguments.length == 2)) {
				keyType = copyType(typeRef.typeArguments[0], source);
				if (keyType instanceof Wildcard) {
					keyType = ((Wildcard)keyType).bound;
				}
				valueType = copyType(typeRef.typeArguments[1], source);
				if (valueType instanceof Wildcard) {
					valueType = ((Wildcard)valueType).bound;
				}
				mapType = addWildCards(source, typeRef, mapType.getTypeName());
			}
		}
		
		String fieldName = new String(field.name);
		
		{ // put
			String putMethodName = singular(camelCase(builderData.getPrefix(), fieldName));
			builderMethods.add(method(typeNode, source, PUBLIC | AccImplementing, OPTIONAL_DEF, putMethodName).withParameter(keyType, "arg0").withParameter(valueType, "arg1")
				.withStatement(methodCall(source, fieldReference(source, thisReference(source), fieldName), "put", nameReference(source, "arg0"), nameReference(source, "arg1")))
				.withReturnStatement(thisReference(source)).build());
			interfaceMethods.add(method(typeNode, source, PUBLIC, OPTIONAL_DEF, putMethodName).withParameter(keyType, "arg0").withParameter(valueType, "arg1").build());
		}
		{ // putAll
			String putAllMethodName = camelCase(builderData.getPrefix(), fieldName);
			builderMethods.add(method(typeNode, source, PUBLIC | AccImplementing, OPTIONAL_DEF, putAllMethodName).withParameter(mapType, "arg0")
				.withStatement(methodCall(source, fieldReference(source, thisReference(source), fieldName), "putAll", nameReference(source, "arg0")))
				.withReturnStatement(thisReference(source)).build());
			interfaceMethods.add(method(typeNode, source, PUBLIC, OPTIONAL_DEF, putAllMethodName).withParameter(mapType, "arg0").build());
		}
	}
	
	private static TypeReference addWildCards(ASTNode source, ParameterizedSingleTypeReference typeRef, char[][] tokens) {
		TypeReference[][] args = new TypeReference[3][];
		if (typeRef.typeArguments != null) {
			args[2] = new TypeReference[typeRef.typeArguments.length];
			int idx = 0;
			for (TypeReference inRef : typeRef.typeArguments) {
				if (inRef == null) args[idx++] = null;
				if (!(inRef instanceof Wildcard)) {
					Wildcard wildcard = new Wildcard(Wildcard.EXTENDS);
					setGeneratedByAndCopyPos(wildcard, source);
					wildcard.bound = copyType(inRef, source);
					args[2][idx++] = wildcard;
				} else {
					args[2][idx++] = copyType(inRef, source);
				}
			}
		}
		ParameterizedQualifiedTypeReference newTypeRef = new ParameterizedQualifiedTypeReference(tokens, args, 0, poss(source, tokens.length));
		setGeneratedByAndCopyPos(newTypeRef, source);
		return newTypeRef;
	}
	
	private static TypeReference addWildCards(ASTNode source, ParameterizedQualifiedTypeReference type, char[][] tokens) {
		ParameterizedQualifiedTypeReference typeRef = (ParameterizedQualifiedTypeReference) copyType(type, source);
		typeRef.tokens = tokens;
		int size = typeRef.typeArguments[typeRef.typeArguments.length - 1].length;
		for (int i = 0; i < size; i++) {
			if (!(typeRef.typeArguments[typeRef.typeArguments.length - 1][i] instanceof Wildcard)) {
				Wildcard wildcard = new Wildcard(Wildcard.EXTENDS);
				setGeneratedByAndCopyPos(wildcard, source);
				wildcard.bound = copyType(typeRef.typeArguments[typeRef.typeArguments.length - 1][i], source);
				typeRef.typeArguments[typeRef.typeArguments.length - 1][i] = wildcard;
			}
		}
		return typeRef;
	}
	
	private static void createBuildMethod(IBuilderData builderData, String typeName, 
			List<AbstractMethodDeclaration> interfaceMethods, List<AbstractMethodDeclaration> builderMethods) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();
		AllocationExpression newClassExp = new AllocationExpression();
		setGeneratedByAndCopyPos(newClassExp, source);
		newClassExp.type = typeReference(source, typeName);
		newClassExp.arguments = new Expression[]{thisReference(source)};
		builderMethods.add(method(typeNode, source, PUBLIC | AccImplementing, typeName, "build").withReturnStatement(newClassExp).build());
		interfaceMethods.add(method(typeNode, source, PUBLIC, typeName, "build").build());
	}
	
	private static void createMethodCall(IBuilderData builderData, String method, List<AbstractMethodDeclaration> interfaceMethods, 
			List<AbstractMethodDeclaration> builderMethods) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();
		TypeDeclaration typeDecl = (TypeDeclaration)typeNode.get();
		
		TypeReference returnType = null;
		List<TypeReference> thrown = new ArrayList<TypeReference>();
		if ("toString".equals(method)) {
			returnType = typeReference(source, "java.lang.String");
		} else {
			for (AbstractMethodDeclaration def : typeDecl.methods) {
				if ((def instanceof MethodDeclaration) && method.equals(new String(def.selector)) && ((def.arguments == null) || (def.arguments.length == 0))) {
					returnType = ((MethodDeclaration)def).returnType;
					thrown = def.thrownExceptions == null ? new ArrayList<TypeReference>() : Arrays.asList(def.thrownExceptions);
				}
			}
			if (returnType == null) {
				typeNode.addWarning("@Builder was unable to find method '" + method + "()' within this class.");
				return;
			}
		}
		
		MessageSend callMethod = methodCall(source, methodCall(source, thisReference(source), "build"), method);
		Statement statement;
		if ("void".equals(returnType.toString())) {
			statement = callMethod;
		} else {
			statement = new ReturnStatement(callMethod, 0, 0);
			setGeneratedByAndCopyPos(statement, source);
		}
		
		builderMethods.add(method(typeNode, source, PUBLIC | AccImplementing, returnType, method).withThrownExceptions(thrown)
				.withStatement(statement).build());
		interfaceMethods.add(method(typeNode, source, PUBLIC, returnType, method).withThrownExceptions(thrown).build());
	}
	
	private static void createBuilder(IBuilderData builderData, List<String> typeNames, List<AbstractMethodDeclaration> builderMethods) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();
		builderMethods.add(constructor(typeNode, source, PRIVATE, BUILDER).build());
		clazz(typeNode, source, PRIVATE | STATIC, BUILDER).implementing(typeNames)
			.withFields(createBuilderFields(builderData)).withMethods(builderMethods).inject();
	}
	
	private static List<FieldDeclaration> createBuilderFields(IBuilderData builderData) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();
		List<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();
		for (FieldDeclaration field : builderData.getAllFields()) {
			FieldBuilder builder = field(typeNode, source, PRIVATE, field.type, new String(field.name));
			if (field.initialization != null) {
				setGeneratedByAndCopyPos(field.initialization, source);
				builder.withInitialization(field.initialization);
				field.initialization = null;
			}
			fields.add(builder.build());
		}
		return fields;
	}
	
	private static boolean isInitializedMapOrCollection(FieldDeclaration field) {
		return (isMap(field) || isCollection(field)) && (field.initialization != null);
	}
	
	// TODO use LombokNode.getImportStatements()
	private static boolean isCollection(FieldDeclaration field) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (char[] elem : field.type.getTypeName()) {
			if (first) first = false;
			else sb.append('.');
			sb.append(elem);
		}
		String type = sb.toString();
		return type.endsWith("Collection") || type.endsWith("List") || type.endsWith("Set");
	}
	
	// TODO use LombokNode.getImportStatements()
	private static boolean isMap(FieldDeclaration field) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (char[] elem : field.type.getTypeName()) {
			if (first) first = false;
			else sb.append('.');
			sb.append(elem);
		}
		String type = sb.toString();
		return type.endsWith("Map");
	}
	
	private static class HandleBuilderDataCollector extends EclipseASTAdapter implements IBuilderData {
		private final EclipseNode typeNode;
		private final ASTNode source;
		private final Set<String> exclude;
		private final String prefix;
		private final boolean convenientMethods;
		private final List<String> callMethods;
		private final AccessLevel level;
		private final List<FieldDeclaration> requiredFields = new ArrayList<FieldDeclaration>();
		private final List<FieldDeclaration> optionalFields = new ArrayList<FieldDeclaration>();
		private final List<String> requiredFieldDefTypeNames = new ArrayList<String>();
		private final Set<String> requiredFieldNames = new HashSet<String>();
		private final Set<String> allRequiredFieldNames = new HashSet<String>();
		private final List<MethodDeclaration> requiredFieldExtensions = new ArrayList<MethodDeclaration>();
		private final List<MethodDeclaration> optionalFieldExtensions = new ArrayList<MethodDeclaration>();
		private boolean isExtensionMethod;
		private boolean containsRequiredFields;
		private int typeDepth;
		private int phase;
		
		public HandleBuilderDataCollector(EclipseNode typeNode, ASTNode source, Builder builder) {
			super();
			this.typeNode = typeNode;
			this.source = source;
			exclude = new HashSet<String>(Arrays.asList(builder.exclude()));
			prefix = builder.prefix();
			convenientMethods = builder.convenientMethods();
			callMethods = Arrays.asList(builder.callMethods());
			level = builder.value();
		}
		
		public IBuilderData collect() {
			phase = 1; typeNode.traverse(this);
			phase = 2; typeNode.traverse(this);
			return this;
		}
		
		public EclipseNode getTypeNode() {
			return typeNode;
		}
		
		public ASTNode getSource() {
			return source;
		}
		
		public List<FieldDeclaration> getRequiredFields() {
			return requiredFields;
		}
		
		public List<FieldDeclaration> getOptionalFields() {
			return optionalFields;
		}
		
		public List<FieldDeclaration> getAllFields() {
			List<FieldDeclaration> allFields = new ArrayList<FieldDeclaration>(requiredFields);
			allFields.addAll(optionalFields);
			return allFields;
		}
		
		public List<String> getRequiredFieldDefTypeNames() {
			return requiredFieldDefTypeNames;
		}
		
		public List<MethodDeclaration> getRequiredFieldExtensions() {
			return requiredFieldExtensions;
		}
		
		public List<MethodDeclaration> getOptionalFieldExtensions() {
			return optionalFieldExtensions;
		}
		
		public int getCreateModifier() {
			return toEclipseModifier(level);
		}
		
		public String getPrefix() {
			return prefix;
		}
		
		public boolean generateConvenientMethods() {
			return convenientMethods;
		}
		
		public List<String> getCallMethods() {
			return callMethods;
		}
		
		@Override public void visitType(EclipseNode typeNode, TypeDeclaration type) {
			typeDepth++;
		}

		@Override public void visitMethod(EclipseNode methodNode, AbstractMethodDeclaration method) {
			if ((phase == 2) && (typeDepth == 1)) {
				isExtensionMethod = false;
				containsRequiredFields = false;
				requiredFieldNames.clear();
				requiredFieldNames.addAll(allRequiredFieldNames);
				boolean isAnImport = methodNode.getImportStatements().contains(BuilderExtension.class.getName());
				if (method.annotations != null) for (Annotation annotation : method.annotations) {
					if (annotation.type.toString().equals(BuilderExtension.class.getName())
							|| (isAnImport && annotation.type.toString().equals(BuilderExtension.class.getSimpleName()))) {
						isExtensionMethod = true;
						return;
					}
				}
			}
		}
		
		@Override public void visitStatement(EclipseNode statementNode, Statement statement) {
			if ((phase == 2) && (typeDepth == 1) && (isExtensionMethod)) {
				if (statement instanceof Assignment) {
					Assignment assign = (Assignment) statement;
					String fieldName = assign.lhs.toString();
					if (fieldName.startsWith("this.")) {
						fieldName = fieldName.substring(5);
					}
					if (requiredFieldNames.remove(fieldName)) {
						containsRequiredFields = true;
					}
				}
			}
		}
		
		@Override public void endVisitMethod(EclipseNode methodNode, AbstractMethodDeclaration method) {
			if ((phase == 2) && (typeDepth == 1) && (isExtensionMethod) && (method instanceof MethodDeclaration)) {
				MethodDeclaration meth = (MethodDeclaration) method;
				if (((meth.modifiers & PRIVATE) != 0) && "void".equals(meth.returnType.toString())) {
					if (containsRequiredFields) {
						if (requiredFieldNames.isEmpty()) {
							requiredFieldExtensions.add(meth);
						} else {
							methodNode.addWarning("@BuilderExtension: The method '" + methodNode.getName() + "' does not contain all required fields and was skipped.", method.sourceStart, method.sourceEnd);
						}
					} else {
						optionalFieldExtensions.add(meth);
					}
				} else {
					methodNode.addWarning("@BuilderExtension: The method '" + methodNode.getName() + "' is not a valid extension and was skipped.", method.sourceStart, method.sourceEnd);
				}
			}
		}
		
		@Override public void endVisitType(EclipseNode typeNode, TypeDeclaration type) {
			if ((phase == 2) && (typeDepth == 1)) {
				List<AbstractMethodDeclaration> methods = new ArrayList<AbstractMethodDeclaration>();
				for (AbstractMethodDeclaration method : type.methods) {
					if (method instanceof MethodDeclaration) {
						if (!requiredFieldExtensions.contains(method) && !(optionalFieldExtensions.contains(method))) {
							methods.add(method);
						}
					} else {
						methods.add(method);
					}
				}
				type.methods = methods.toArray(new AbstractMethodDeclaration[methods.size()]);
			}
			typeDepth--;
		}
		
		@Override public void visitField(EclipseNode fieldNode, FieldDeclaration field) {
			if ((phase == 1) && (typeDepth == 1)) {
				if ((field.modifiers & STATIC) != 0) return;
				String fieldName = new String(field.name);
				if (exclude.contains(fieldName)) return;
				if ((field.initialization == null) && ((field.modifiers & FINAL) != 0)) {
					requiredFields.add(field);
					allRequiredFieldNames.add(fieldName);
					requiredFieldDefTypeNames.add(camelCase("$", fieldName, "def"));
				}
				boolean append = isInitializedMapOrCollection(field) && convenientMethods;
				append |= (field.modifiers & FINAL) == 0;
				if (append) optionalFields.add(field);
			}
		}
	}
	
	private static interface IBuilderData {
		public EclipseNode getTypeNode();
		
		public ASTNode getSource();
		
		public List<FieldDeclaration> getRequiredFields();
		
		public List<FieldDeclaration> getOptionalFields();
		
		public List<FieldDeclaration> getAllFields();
		
		public List<String> getRequiredFieldDefTypeNames();
		
		public List<MethodDeclaration> getRequiredFieldExtensions();
		
		public List<MethodDeclaration> getOptionalFieldExtensions();
		
		public int getCreateModifier();
		
		public String getPrefix();
		
		public boolean generateConvenientMethods();
		
		public List<String> getCallMethods();
	}
}
