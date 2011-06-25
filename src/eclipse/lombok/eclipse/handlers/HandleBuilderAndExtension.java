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

import static lombok.ast.AST.*;
import static lombok.core.util.ErrorMessages.canBeUsedOnClassOnly;
import static lombok.core.util.ErrorMessages.canBeUsedOnConcreteMethodOnly;
import static lombok.core.util.ErrorMessages.canBeUsedOnMethodOnly;
import static lombok.core.util.Names.*;
import static lombok.eclipse.handlers.Eclipse.*;
import static org.eclipse.jdt.core.dom.Modifier.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;
import static lombok.eclipse.Eclipse.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ast.AbstractMethodDecl;
import lombok.ast.Argument;
import lombok.ast.Call;
import lombok.ast.ConstructorDecl;
import lombok.ast.FieldDecl;
import lombok.ast.TypeRef;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseASTAdapter;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.EclipseHandlerUtil.MemberExistsResult;
import lombok.eclipse.handlers.ast.EclipseMethod;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.mangosdk.spi.ProviderFor;

public class HandleBuilderAndExtension {
	private final static String OPTIONAL_DEF = "$OptionalDef";
	private final static String BUILDER = "$Builder";

	/**
	 * Handles the {@code lombok.Builder} annotation for eclipse.
	 */
	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleBuilder extends EclipseAnnotationHandler<Builder> {

		@Override public void handle(AnnotationValues<Builder> annotation, Annotation source, EclipseNode annotationNode) {
			final EclipseNode typeNode = annotationNode.up();
	
			final TypeDeclaration typeDecl = typeDeclFiltering(typeNode, AccInterface | AccAnnotation | AccEnum);
			if (typeDecl == null) {
				annotationNode.addError(canBeUsedOnClassOnly(Builder.class));
				return;
			}
	
			switch (methodExists(decapitalize(typeNode.getName()), typeNode, false)) {
			case EXISTS_BY_LOMBOK:
				return;
			case EXISTS_BY_USER:
				annotationNode.addWarning(String.format("Not generating 'public static %s %s()' A method with that name already exists", BUILDER, decapitalize(typeNode.getName())));
				return;
			default:
			case NOT_EXISTS:
				//continue with creating the builder
			}
	
			new HandleBuilderAndExtension().handleBuilder(new BuilderDataCollector(typeNode, source, annotation.getInstance()).collect());
		}
	}
	
	/**
	 * Handles the {@code lombok.Builder.Extension} annotation for eclipse.
	 */
	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleBuilderExtension extends EclipseAnnotationHandler<Builder.Extension> {

		@Override public void handle(AnnotationValues<Builder.Extension> annotation, Annotation source, EclipseNode annotationNode) {
			final Class<? extends java.lang.annotation.Annotation> annotationType = Builder.Extension.class;

			final EclipseMethod method = EclipseMethod.methodOf(annotationNode, source);

			if (method == null) {
				annotationNode.addError(canBeUsedOnMethodOnly(annotationType));
				return;
			}
			if (method.isAbstract() || method.isEmpty()) {
				annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
				return;
			}
			
			final EclipseNode typeNode = typeNodeOf(annotationNode);
			EclipseNode builderNode = null;
			
			for (EclipseNode child : typeNode.down()) {
				if (child.getKind() != Kind.ANNOTATION) continue;
				if (Eclipse.annotationTypeMatches(Builder.class, child)) {
					builderNode = child;
				}
			}
			
			if (builderNode == null) {
				annotationNode.addError("@Builder.Extension is only allowed in types annotated with @Builder");
				return;
			}
			AnnotationValues<Builder> builderAnnotation = Eclipse.createAnnotation(Builder.class, builderNode);
			if (methodExists(decapitalize(typeNode.getName()), typeNode, false) == MemberExistsResult.NOT_EXISTS) {
				new HandleBuilder().handle(builderAnnotation, (Annotation)builderNode.get(), builderNode);
			}
	
			new HandleBuilderAndExtension().handleExtension(new BuilderDataCollector(typeNode, source, builderAnnotation.getInstance()).collect(), method);
		}
		
		@Override
		public boolean deferUntilPostDiet() {
			return true;
		}
	}

	public void handleBuilder(final IBuilderData builderData) {
		final List<TypeRef> requiredFieldDefTypes = builderData.getRequiredFieldDefTypes();
		final List<TypeRef> interfaceTypes = new ArrayList<TypeRef>(requiredFieldDefTypes);
		interfaceTypes.add(Type(OPTIONAL_DEF));
		TypeRef fieldDefType = builderData.getRequiredFields().isEmpty() ? Type(OPTIONAL_DEF) : requiredFieldDefTypes.get(0);

		createConstructor(builderData);
		createInitializeBuilderMethod(builderData, fieldDefType);
		List<AbstractMethodDecl<?>> builderMethods = new ArrayList<AbstractMethodDecl<?>>();
		createRequiredFieldInterfaces(builderData, builderMethods);
		createOptionalFieldInterface(builderData, builderMethods);
		createBuilder(builderData, interfaceTypes, builderMethods);
	}
	
	public void handleExtension(final IBuilderData builderData, final EclipseMethod method) {
		EclipseType type = builderData.getType();
		ExtensionCollector extensionCollector = new ExtensionCollector().withRequiredFieldNames(builderData.getAllRequiredFieldNames());
		method.node().traverse(extensionCollector);
		if (extensionCollector.isExtension()) {
			EclipseType builderType = type.memberType(BUILDER);
			EclipseType interfaceType;
			if (extensionCollector.isRequiredFieldsExtension()) {
				interfaceType = type.memberType(builderData.getRequiredFieldDefTypeNames().get(0));
			} else {
				interfaceType = type.memberType(OPTIONAL_DEF);
			}
			String methodName = method.name();
			builderType.injectMethod(MethodDecl(Type(OPTIONAL_DEF), methodName).makePublic().implementing().withArguments(method.arguments()).withAnnotations(method.annotations()) //
					.withStatements(method.statements()) //
					.withStatement(Return(This())));
			interfaceType.injectMethod(MethodDecl(Type(OPTIONAL_DEF), method.name()).makePublic().withNoBody().withArguments(method.arguments()));
			type.removeMethod(method);
		}
	}

	private void createConstructor(final IBuilderData builderData) {
		EclipseType type = builderData.getType();
		ConstructorDecl constructorDecl = ConstructorDecl(type.name()).makePrivate().withArgument(Arg(Type(BUILDER), "builder").makeFinal()).withImplicitSuper();
		for (final FieldDeclaration field : builderData.getAllFields()) {
			final String fieldName = new String(field.name);
			constructorDecl.withStatement(Assign(Field(This(), fieldName), Field(Name("builder"), fieldName)));
		}
		type.injectConstructor(constructorDecl);
	}

	private void createInitializeBuilderMethod(final IBuilderData builderData, final TypeRef fieldDefType) {
		final EclipseType type = builderData.getType();
		type.injectMethod(MethodDecl(fieldDefType, decapitalize(type.name())).makeStatic().withAccessLevel(builderData.getLevel()).withStatement(Return(New(Type(BUILDER)))));
	}

	private void createRequiredFieldInterfaces(IBuilderData builderData, List<AbstractMethodDecl<?>> builderMethods) {
		List<FieldDeclaration> fields = builderData.getRequiredFields();
		if (!fields.isEmpty()) {
			EclipseType type = builderData.getType();
			List<String> names = builderData.getRequiredFieldDefTypeNames();
			FieldDeclaration field = fields.get(0);
			String name = names.get(0);
			for (int i = 1, iend = fields.size(); i < iend; i++) {
				List<AbstractMethodDecl<?>> interfaceMethods = new ArrayList<AbstractMethodDecl<?>>();
				createFluentSetter(builderData, names.get(i), field, interfaceMethods, builderMethods);

				type.injectType(InterfaceDecl(name).makePublic().makeStatic().withMethods(interfaceMethods));
				field = fields.get(i);
				name = names.get(i);
			}
			List<AbstractMethodDecl<?>> interfaceMethods = new ArrayList<AbstractMethodDecl<?>>();
			createFluentSetter(builderData, OPTIONAL_DEF, field, interfaceMethods, builderMethods);

			type.injectType(InterfaceDecl(name).makePublic().makeStatic().withMethods(interfaceMethods));
		}
	}

	private void createOptionalFieldInterface(IBuilderData builderData, List<AbstractMethodDecl<?>> builderMethods) {
		EclipseType type = builderData.getType();
		TypeDeclaration typeDecl = (TypeDeclaration)type.get();
		List<AbstractMethodDecl<?>> interfaceMethods = new ArrayList<AbstractMethodDecl<?>>();
		for (FieldDeclaration field : builderData.getOptionalFields()) {
			if (isInitializedMapOrCollection(field)) {
				if (builderData.isGenerateConvenientMethodsEnabled()) {
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

		type.injectType(InterfaceDecl(OPTIONAL_DEF).makePublic().makeStatic().withMethods(interfaceMethods));
	}

	private void createFluentSetter(IBuilderData builderData, String typeName, FieldDeclaration field, List<AbstractMethodDecl<?>> interfaceMethods, List<AbstractMethodDecl<?>> builderMethods) {
		String fieldName = new String(field.name);
		String methodName = camelCase(builderData.getPrefix(), fieldName);
		final Argument arg0 = Arg(Type(field.type), fieldName).makeFinal();
		builderMethods.add(MethodDecl(Type(typeName), methodName).makePublic().implementing().withArgument(arg0) //
			.withStatement(Assign(Field(This(), fieldName), Name(fieldName))) //
			.withStatement(Return(This())));
		interfaceMethods.add(MethodDecl(Type(typeName), methodName).makePublic().withNoBody().withArgument(arg0));
	}

	private void createCollectionSignaturesAndMethods(IBuilderData builderData, FieldDeclaration field, List<AbstractMethodDecl<?>> interfaceMethods, List<AbstractMethodDecl<?>> builderMethods) {
		EclipseType type = builderData.getType();
		ASTNode source = builderData.getSource();

		TypeReference elementType = type.build(Type("java.lang.Object"));
		TypeReference collectionType = type.build(Type("java.util.Collection"));
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
			final Argument arg0 = Arg(Type(elementType), "arg0").makeFinal();
			builderMethods.add(MethodDecl(Type(OPTIONAL_DEF), addMethodName).makePublic().implementing().withArgument(arg0) //
				.withStatement(Call(Field(This(), fieldName), "add").withArgument(Name("arg0"))) //
				.withStatement(Return(This())));
			interfaceMethods.add(MethodDecl(Type(OPTIONAL_DEF), addMethodName).makePublic().withNoBody().withArgument(arg0));
		}
		{ // addAll
			String addAllMethodName = camelCase(builderData.getPrefix(), fieldName);
			final Argument arg0 = Arg(Type(collectionType), "arg0").makeFinal();
			builderMethods.add(MethodDecl(Type(OPTIONAL_DEF), addAllMethodName).makePublic().implementing().withArgument(arg0) //
				.withStatement(Call(Field(This(), fieldName), "addAll").withArgument(Name("arg0"))) //
				.withStatement(Return(This())));
			interfaceMethods.add(MethodDecl(Type(OPTIONAL_DEF), addAllMethodName).makePublic().withNoBody().withArgument(arg0));
		}
	}

	private void createMapSignaturesAndMethods(IBuilderData builderData, FieldDeclaration field, List<AbstractMethodDecl<?>> interfaceMethods, List<AbstractMethodDecl<?>> builderMethods) {
		EclipseType type = builderData.getType();
		ASTNode source = builderData.getSource();

		TypeReference keyType = type.build(Type("java.lang.Object"));
		TypeReference valueType = type.build(Type("java.lang.Object"));
		TypeReference mapType = type.build(Type("java.util.Map"));
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
			final String putMethodName = singular(camelCase(builderData.getPrefix(), fieldName));
			final Argument arg0 = Arg(Type(keyType), "arg0").makeFinal();
			final Argument arg1 = Arg(Type(valueType), "arg1").makeFinal();
			builderMethods.add(MethodDecl(Type(OPTIONAL_DEF), putMethodName).makePublic().implementing().withArgument(arg0).withArgument(arg1) //
				.withStatement(Call(Field(This(), fieldName), "put").withArgument(Name("arg0")).withArgument(Name("arg1"))) //
				.withStatement(Return(This())));
			interfaceMethods.add(MethodDecl(Type(OPTIONAL_DEF), putMethodName).makePublic().withNoBody().withArgument(arg0).withArgument(arg1));
		}
		{ // putAll
			String putAllMethodName = camelCase(builderData.getPrefix(), fieldName);
			final Argument arg0 = Arg(Type(mapType), "arg0").makeFinal();
			builderMethods.add(MethodDecl(Type(OPTIONAL_DEF), putAllMethodName).makePublic().implementing().withArgument(arg0) //
				.withStatement(Call(Field(This(), fieldName), "putAll").withArgument(Name("arg0"))) //
				.withStatement(Return(This())));
			interfaceMethods.add(MethodDecl(Type(OPTIONAL_DEF), putAllMethodName).makePublic().withNoBody().withArgument(arg0));
		}
	}

	private TypeReference addWildCards(ASTNode source, ParameterizedSingleTypeReference typeRef, char[][] tokens) {
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

	private TypeReference addWildCards(ASTNode source, ParameterizedQualifiedTypeReference type, char[][] tokens) {
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

	private void createBuildMethod(IBuilderData builderData, String typeName, List<AbstractMethodDecl<?>> interfaceMethods, List<AbstractMethodDecl<?>> builderMethods) {
		builderMethods.add(MethodDecl(Type(typeName), "build").makePublic().implementing() //
			.withStatement(Return(New(Type(typeName)).withArgument(This()))));
		interfaceMethods.add(MethodDecl(Type(typeName), "build").makePublic().withNoBody());
	}

	private void createMethodCall(IBuilderData builderData, String methodName, List<AbstractMethodDecl<?>> interfaceMethods, List<AbstractMethodDecl<?>> builderMethods) {
		EclipseType type = builderData.getType();

		TypeRef returnType = Type("void");
		List<TypeRef> thrownExceptions = new ArrayList<TypeRef>();
		if ("toString".equals(methodName)) {
			returnType = Type("java.lang.String");
		} else {
			for (EclipseMethod method : type.methods()) {
				if (methodName.equals(method.name()) && !method.hasArguments()) {
					returnType = method.returns();
					thrownExceptions.addAll(method.thrownExceptions());
					break;
				}
			}
		}

		Call call = Call(Call("build"), methodName);
		if ("void".equals(type.build(returnType).toString())) {
			builderMethods.add(MethodDecl(returnType, methodName).makePublic().implementing().withThrownExceptions(thrownExceptions) //
				.withStatement(call));
		} else {
			builderMethods.add(MethodDecl(returnType, methodName).makePublic().implementing().withThrownExceptions(thrownExceptions) //
				.withStatement(Return(call)));
		}
		interfaceMethods.add(MethodDecl(returnType, methodName).makePublic().withNoBody().withThrownExceptions(thrownExceptions));
	}

	private void createBuilder(IBuilderData builderData, List<TypeRef> interfaceTypes, List<AbstractMethodDecl<?>> builderMethods) {
		EclipseType type = builderData.getType();
		builderMethods.add(ConstructorDecl(BUILDER).makePrivate().withImplicitSuper());
		type.injectType(ClassDecl(BUILDER).makePrivate().makeStatic().implementing(interfaceTypes) //
			.withFields(createBuilderFields(builderData)).withMethods(builderMethods));
	}

	private List<FieldDecl> createBuilderFields(IBuilderData builderData) {
		List<FieldDecl> fields = new ArrayList<FieldDecl>();
		for (FieldDeclaration field : builderData.getAllFields()) {
			FieldDecl builder = FieldDecl(Type(field.type), new String(field.name)).makePrivate();
			if (field.initialization != null) {
				builder.withInitialization(Expr(field.initialization));
				field.initialization = null;
			}
			fields.add(builder);
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

	private static class BuilderDataCollector extends EclipseASTAdapterWithTypeDepth implements IBuilderData {
		@Getter
		private final EclipseType type;
		@Getter
		private final EclipseNode typeNode;
		@Getter
		private final ASTNode source;
		@Getter
		private final String prefix;
		@Getter
		private final List<String> callMethods;
		@Getter
		private final List<FieldDeclaration> requiredFields = new ArrayList<FieldDeclaration>();
		@Getter
		private final List<FieldDeclaration> optionalFields = new ArrayList<FieldDeclaration>();
		@Getter
		private final List<TypeRef> requiredFieldDefTypes = new ArrayList<TypeRef>();
		@Getter
		private final List<String> allRequiredFieldNames = new ArrayList<String>();
		@Getter
		private final List<String> requiredFieldDefTypeNames = new ArrayList<String>();
		@Getter
		private final boolean generateConvenientMethodsEnabled;
		@Getter
		private final AccessLevel level;
		private final Set<String> exclude;

		public BuilderDataCollector(EclipseNode typeNode, ASTNode source, Builder builder) {
			super(1);
			type = EclipseType.typeOf(typeNode, source);
			this.typeNode = typeNode;
			this.source = source;
			exclude = new HashSet<String>(Arrays.asList(builder.exclude()));
			generateConvenientMethodsEnabled = builder.convenientMethods();
			prefix = builder.prefix();
			callMethods = Arrays.asList(builder.callMethods());
			level = builder.value();
		}

		public IBuilderData collect() {
			typeNode.traverse(this);
			return this;
		}

		@Override
		public List<FieldDeclaration> getAllFields() {
			List<FieldDeclaration> allFields = new ArrayList<FieldDeclaration>(getRequiredFields());
			allFields.addAll(getOptionalFields());
			return allFields;
		}
		
		@Override public void visitField(EclipseNode fieldNode, FieldDeclaration field) {
			if (isOfInterest()) {
				if ((field.modifiers & STATIC) != 0) return;
				String fieldName = new String(field.name);
				if (exclude.contains(fieldName)) return;
				if ((field.initialization == null) && ((field.modifiers & FINAL) != 0)) {
					requiredFields.add(field);
					allRequiredFieldNames.add(fieldName);
					String typeName = camelCase("$", fieldName, "def");
					requiredFieldDefTypeNames.add(typeName);
					requiredFieldDefTypes.add(Type(typeName));
				}
				boolean append = isInitializedMapOrCollection(field) && generateConvenientMethodsEnabled;
				append |= (field.modifiers & FINAL) == 0;
				if (append) optionalFields.add(field);
			}
		}
	}

	private static class ExtensionCollector extends EclipseASTAdapterWithTypeDepth {
		@Getter
		private boolean isRequiredFieldsExtension;
		@Getter
		private boolean isExtension;
		private final Set<String> allRequiredFieldNames = new HashSet<String>();
		private final Set<String> requiredFieldNames = new HashSet<String>();
		private boolean containsRequiredFields;

		public ExtensionCollector() {
			super(1);
		}

		public ExtensionCollector withRequiredFieldNames(final List<String> fieldNames) {
			allRequiredFieldNames.clear();
			allRequiredFieldNames.addAll(fieldNames);
			return this;
		}

		@Override public void visitMethod(EclipseNode methodNode, AbstractMethodDeclaration method) {
			if (isOfInterest() && (method instanceof MethodDeclaration)) {
				containsRequiredFields = false;
				isRequiredFieldsExtension = false;
				isExtension = false;
				requiredFieldNames.clear();
				requiredFieldNames.addAll(allRequiredFieldNames);
			}
		}
		
		@Override public void visitStatement(EclipseNode statementNode, Statement statement) {
			if (isOfInterest()) {
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
			if (isOfInterest() && (method instanceof MethodDeclaration)) {
				MethodDeclaration meth = (MethodDeclaration) method;
				if (((meth.modifiers & PRIVATE) != 0) && "void".equals(meth.returnType.toString())) {
					if (containsRequiredFields) {
						if (requiredFieldNames.isEmpty()) {
							isRequiredFieldsExtension = true;
							isExtension = true;
						} else {
							methodNode.addWarning("@Builder.Extension: The method '" + methodNode.getName() + "' does not contain all required fields and was skipped.", method.sourceStart, method.sourceEnd);
						}
					} else {
						isExtension = true;
					}
				} else {
					methodNode.addWarning("@Builder.Extension: The method '" + methodNode.getName() + "' is not a valid extension and was skipped.", method.sourceStart, method.sourceEnd);
				}
			}
		}
	}

	@RequiredArgsConstructor
	private static class EclipseASTAdapterWithTypeDepth extends EclipseASTAdapter {
		private final int maxTypeDepth;
		private int typeDepth;

		@Override public void visitType(EclipseNode typeNode, TypeDeclaration type) {
			typeDepth++;
		}

		@Override public void endVisitType(EclipseNode typeNode, TypeDeclaration type) {
			typeDepth--;
		}

		public boolean isOfInterest() {
			return typeDepth <= maxTypeDepth;
		}

		@Override
		public boolean deferUntilPostDiet() {
			return false;
		}
	}

	private static interface IBuilderData {
		public EclipseType getType();
		
		public EclipseNode getTypeNode();

		public ASTNode getSource();

		public AccessLevel getLevel();

		public String getPrefix();

		public List<String> getCallMethods();

		public List<FieldDeclaration> getAllFields();
		
		public List<FieldDeclaration> getRequiredFields();

		public List<FieldDeclaration> getOptionalFields();

		public List<TypeRef> getRequiredFieldDefTypes();
		
		public List<String> getAllRequiredFieldNames();

		public List<String> getRequiredFieldDefTypeNames();

		public boolean isGenerateConvenientMethodsEnabled();
	}
}