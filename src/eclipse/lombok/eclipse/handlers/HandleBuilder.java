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

import static lombok.eclipse.handlers.ast.ASTBuilder.*;
import static lombok.core.util.ErrorMessages.canBeUsedOnClassOnly;
import static lombok.core.util.Names.*;
import static lombok.eclipse.handlers.Eclipse.*;
import static org.eclipse.jdt.core.dom.Modifier.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;
import static org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers.*;
import static lombok.eclipse.Eclipse.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.BuilderExtension;
import lombok.Delegate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseASTAdapter;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.ASTNodeBuilder;
import lombok.eclipse.handlers.ast.ArgBuilder;
import lombok.eclipse.handlers.ast.ConstructorDefBuilder;
import lombok.eclipse.handlers.ast.ExpressionBuilder;
import lombok.eclipse.handlers.ast.FieldDefBuilder;
import lombok.eclipse.handlers.ast.CallBuilder;
import lombok.eclipse.handlers.ast.StatementBuilder;

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

/**
 * Handles the {@code lombok.Builder} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleBuilder implements EclipseAnnotationHandler<Builder> {
	private final static String OPTIONAL_DEF = "$OptionalDef";
	private final static String BUILDER = "$Builder";

	@Override public boolean handle(AnnotationValues<Builder> annotation, Annotation source, EclipseNode annotationNode) {
		final EclipseNode typeNode = annotationNode.up();

		final TypeDeclaration typeDecl = typeDeclFiltering(typeNode, AccInterface | AccAnnotation | AccEnum);
		if (typeDecl == null) {
			annotationNode.addError(canBeUsedOnClassOnly(Builder.class));
			return true;
		}

		switch (methodExists(decapitalize(typeNode.getName()), typeNode, false)) {
		case EXISTS_BY_LOMBOK:
			return true;
		case EXISTS_BY_USER:
			annotationNode.addWarning(String.format("Not generating 'public static %s %s()' A method with that name already exists", BUILDER, decapitalize(typeNode.getName())));
			return true;
		default:
		case NOT_EXISTS:
			//continue with creating the builder
		}

		handleBuilder(new HandleBuilderDataCollector(typeNode, source, annotation.getInstance()).collect());
		return true;
	}

	private void handleBuilder(final IBuilderData builderData) {
		final List<ExpressionBuilder<? extends TypeReference>> requiredFieldDefTypes = builderData.getRequiredFieldDefTypes();
		final List<ExpressionBuilder<? extends TypeReference>> interfaceTypes = new ArrayList<ExpressionBuilder<? extends TypeReference>>(requiredFieldDefTypes);
		interfaceTypes.add(Type(OPTIONAL_DEF));
		ExpressionBuilder<? extends TypeReference> fieldDefType = builderData.getRequiredFields().isEmpty() ? Type(OPTIONAL_DEF) : requiredFieldDefTypes.get(0);

		createConstructor(builderData);
		createInitializeBuilderMethod(builderData, fieldDefType);
		List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> builderMethods = new ArrayList<ASTNodeBuilder<? extends AbstractMethodDeclaration>>();
		createRequiredFieldInterfaces(builderData, builderMethods);
		createOptionalFieldInterface(builderData, builderMethods);
		createBuilder(builderData, interfaceTypes, builderMethods);
	}

	private void createConstructor(final IBuilderData builderData) {
		final EclipseNode typeNode = builderData.getTypeNode();
		final ASTNode source = builderData.getSource();
		final ConstructorDefBuilder builder = ConstructorDef(typeNode.getName()).makePrivate().withArgument(Arg(Type(BUILDER), "builder").makeFinal()).withImplicitSuper();
		for (final FieldDeclaration field : builderData.getAllFields()) {
			final String fieldName = new String(field.name);
			builder.withStatement(Assign(Field(This(), fieldName), Field(Name("builder"), fieldName)));
		}
		builder.injectInto(typeNode, source);
	}

	private void createInitializeBuilderMethod(final IBuilderData builderData, final ExpressionBuilder<? extends TypeReference> fieldDefType) {
		final EclipseNode typeNode = builderData.getTypeNode();
		MethodDef(fieldDefType, decapitalize(typeNode.getName())).withModifiers(STATIC | builderData.getCreateModifier()).withStatement(Return(New(Type(BUILDER)))) //
			.injectInto(typeNode, builderData.getSource());
	}

	private void createRequiredFieldInterfaces(IBuilderData builderData, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> builderMethods) {
		List<FieldDeclaration> fields = builderData.getRequiredFields();
		if (!fields.isEmpty()) {
			EclipseNode typeNode = builderData.getTypeNode();
			ASTNode source = builderData.getSource();
			List<String> names = builderData.getRequiredFieldDefTypeNames();
			boolean createFieldExtension = true;
			FieldDeclaration field = fields.get(0);
			String name = names.get(0);
			for (int i = 1, iend = fields.size(); i < iend; i++) {
				List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> interfaceMethods = new ArrayList<ASTNodeBuilder<? extends AbstractMethodDeclaration>>();
				createFluentSetter(builderData, names.get(i), field, interfaceMethods, builderMethods);
				if (createFieldExtension) {
					for (MethodDeclaration extension : builderData.getRequiredFieldExtensions()) {
						createExtension(builderData, extension, interfaceMethods, builderMethods);
					}
					createFieldExtension = false;
				}
				
				ClassDef(name).withModifiers(PUBLIC | STATIC | AccInterface).withMethods(interfaceMethods).injectInto(typeNode, source);
				field = fields.get(i);
				name = names.get(i);
			}
			List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> interfaceMethods = new ArrayList<ASTNodeBuilder<? extends AbstractMethodDeclaration>>();
			createFluentSetter(builderData, OPTIONAL_DEF, field, interfaceMethods, builderMethods);
			
			ClassDef(name).withModifiers(PUBLIC | STATIC | AccInterface).withMethods(interfaceMethods).injectInto(typeNode, source);
		}
	}

	private void createOptionalFieldInterface(IBuilderData builderData, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> builderMethods) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();
		TypeDeclaration typeDecl = (TypeDeclaration)typeNode.get();
		List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> interfaceMethods = new ArrayList<ASTNodeBuilder<? extends AbstractMethodDeclaration>>();
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

		for (MethodDeclaration extension : builderData.getOptionalFieldExtensions()) {
			createExtension(builderData, extension, interfaceMethods, builderMethods);
		}

		ClassDef(OPTIONAL_DEF).withModifiers(PUBLIC | STATIC | AccInterface).withMethods(interfaceMethods).injectInto(typeNode, source);
	}

	private void createExtension(IBuilderData builderData, MethodDeclaration extension, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> interfaceMethods, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> builderMethods) {
		String methodName = new String(extension.selector);	
		builderMethods.add(MethodDef(Type(OPTIONAL_DEF), methodName).withModifiers(PUBLIC | AccImplementing).withArguments(extension.arguments).withAnnotations(extension.annotations) //
			.withStatements(extension.statements) //
			.withStatement(Return(This())));
		interfaceMethods.add(MethodDef(Type(OPTIONAL_DEF), methodName).makePublic().withNoBody().withArguments(extension.arguments));
	}

	private void createFluentSetter(IBuilderData builderData, String typeName, FieldDeclaration field, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> interfaceMethods, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> builderMethods) {
		String fieldName = new String(field.name);
		String methodName = camelCase(builderData.getPrefix(), fieldName);
		final ArgBuilder arg0 = Arg(Type(field.type), fieldName).makeFinal();
		builderMethods.add(MethodDef(Type(typeName), methodName).withModifiers(PUBLIC | AccImplementing).withArgument(arg0) //
			.withStatement(Assign(Field(This(), fieldName), Name(fieldName))) //
			.withStatement(Return(This())));
		interfaceMethods.add(MethodDef(Type(typeName), methodName).makePublic().withNoBody().withArgument(arg0));
	}

	private void createCollectionSignaturesAndMethods(IBuilderData builderData, FieldDeclaration field, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> interfaceMethods, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> builderMethods) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();

		TypeReference elementType = Type("java.lang.Object").build(typeNode, source);
		TypeReference collectionType = Type("java.util.Collection").build(typeNode, source);
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
			final ArgBuilder arg0 = Arg(Type(elementType), "arg0").makeFinal();
			builderMethods.add(MethodDef(Type(OPTIONAL_DEF), addMethodName).withModifiers(PUBLIC | AccImplementing).withArgument(arg0) //
				.withStatement(Call(Field(This(), fieldName), "add").withArgument(Name("arg0"))) //
				.withStatement(Return(This())));
			interfaceMethods.add(MethodDef(Type(OPTIONAL_DEF), addMethodName).makePublic().withNoBody().withArgument(arg0));
		}
		{ // addAll
			String addAllMethodName = camelCase(builderData.getPrefix(), fieldName);
			final ArgBuilder arg0 = Arg(Type(collectionType), "arg0").makeFinal();
			builderMethods.add(MethodDef(Type(OPTIONAL_DEF), addAllMethodName).withModifiers(PUBLIC | AccImplementing).withArgument(arg0) //
				.withStatement(Call(Field(This(), fieldName), "addAll").withArgument(Name("arg0"))) //
				.withStatement(Return(This())));
			interfaceMethods.add(MethodDef(Type(OPTIONAL_DEF), addAllMethodName).makePublic().withNoBody().withArgument(arg0));
		}
	}

	private void createMapSignaturesAndMethods(IBuilderData builderData, FieldDeclaration field, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> interfaceMethods, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> builderMethods) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();

		TypeReference keyType = Type("java.lang.Object").build(typeNode, source);
		TypeReference valueType = Type("java.lang.Object").build(typeNode, source);
		TypeReference mapType = Type("java.util.Map").build(typeNode, source);
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
			final ArgBuilder arg0 = Arg(Type(keyType), "arg0").makeFinal();
			final ArgBuilder arg1 = Arg(Type(valueType), "arg1").makeFinal();
			builderMethods.add(MethodDef(Type(OPTIONAL_DEF), putMethodName).withModifiers(PUBLIC | AccImplementing).withArgument(arg0).withArgument(arg1) //
				.withStatement(Call(Field(This(), fieldName), "put").withArgument(Name("arg0")).withArgument(Name("arg1"))) //
				.withStatement(Return(This())));
			interfaceMethods.add(MethodDef(Type(OPTIONAL_DEF), putMethodName).makePublic().withNoBody().withArgument(arg0).withArgument(arg1));
		}
		{ // putAll
			String putAllMethodName = camelCase(builderData.getPrefix(), fieldName);
			final ArgBuilder arg0 = Arg(Type(mapType), "arg0").makeFinal();
			builderMethods.add(MethodDef(Type(OPTIONAL_DEF), putAllMethodName).withModifiers(PUBLIC | AccImplementing).withArgument(arg0) //
				.withStatement(Call(Field(This(), fieldName), "putAll").withArgument(Name("arg0"))) //
				.withStatement(Return(This())));
			interfaceMethods.add(MethodDef(Type(OPTIONAL_DEF), putAllMethodName).makePublic().withNoBody().withArgument(arg0));
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

	private void createBuildMethod(IBuilderData builderData, String typeName, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> interfaceMethods, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> builderMethods) {
		builderMethods.add(MethodDef(Type(typeName), "build").withModifiers(PUBLIC | AccImplementing) //
			.withStatement(Return(New(Type(typeName)).withArgument(This()))));
		interfaceMethods.add(MethodDef(Type(typeName), "build").makePublic().withNoBody());
	}

	private void createMethodCall(IBuilderData builderData, String method, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> interfaceMethods, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> builderMethods) {
		EclipseNode typeNode = builderData.getTypeNode();
		TypeDeclaration typeDecl = (TypeDeclaration)typeNode.get();

		ExpressionBuilder<? extends TypeReference> returnType = null;
		TypeReference[] thrown = null;
		if ("toString".equals(method)) {
			returnType = Type("java.lang.String");
		} else {
			for (AbstractMethodDeclaration def : typeDecl.methods) {
				if ((def instanceof MethodDeclaration) && method.equals(new String(def.selector)) && ((def.arguments == null) || (def.arguments.length == 0))) {
					returnType = Type(((MethodDeclaration)def).returnType);
					thrown = def.thrownExceptions;
				}
			}
			if (returnType == null) {
				typeNode.addWarning("@Builder was unable to find method '" + method + "()' within this class.");
				return;
			}
		}

		CallBuilder call = Call(Call(This(), "build"), method);
		if ("void".equals(returnType.build(typeNode, builderData.getSource()).toString())) {
			builderMethods.add(MethodDef(returnType, method).withModifiers(PUBLIC | AccImplementing).withThrownExceptions(thrown) //
				.withStatement(call));
		} else {
			builderMethods.add(MethodDef(returnType, method).withModifiers(PUBLIC | AccImplementing).withThrownExceptions(thrown) //
				.withStatement(Return(call)));
		}
		interfaceMethods.add(MethodDef(returnType, method).makePublic().withNoBody().withThrownExceptions(thrown));
	}

	private void createBuilder(IBuilderData builderData, List<ExpressionBuilder<? extends TypeReference>> interfaceTypes, List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> builderMethods) {
		EclipseNode typeNode = builderData.getTypeNode();
		ASTNode source = builderData.getSource();
		builderMethods.add(ConstructorDef(BUILDER).makePrivate().withImplicitSuper());
		ClassDef(BUILDER).withModifiers(PRIVATE | STATIC).implementing(interfaceTypes) //
			.withFields(createBuilderFields(builderData)).withMethods(builderMethods).injectInto(typeNode, source);
	}

	private List<StatementBuilder<? extends FieldDeclaration>> createBuilderFields(IBuilderData builderData) {
		List<StatementBuilder<? extends FieldDeclaration>> fields = new ArrayList<StatementBuilder<? extends FieldDeclaration>>();
		for (FieldDeclaration field : builderData.getAllFields()) {
			FieldDefBuilder builder = FieldDef(Type(field.type), new String(field.name)).makePrivate();
			if (field.initialization != null) {
				builder.withInitialization(field.initialization);
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

	private static class HandleBuilderDataCollector implements IBuilderData {
		@Getter
		private final EclipseNode typeNode;
		@Getter
		private final ASTNode source;
		@Getter
		private final String prefix;
		@Getter
		private final List<String> callMethods;
		private final AccessLevel level;
		
		@Delegate(IBuilderExtensionData.class)
		private final ExtensionCollector extensionCollector;
		@Delegate(IBuilderData.class)
		private final FieldCollector fieldCollector;

		public HandleBuilderDataCollector(EclipseNode typeNode, ASTNode source, Builder builder) {
			super();
			this.typeNode = typeNode;
			this.source = source;
			fieldCollector = new FieldCollector(builder);
			extensionCollector = new ExtensionCollector();
			prefix = builder.prefix();
			callMethods = Arrays.asList(builder.callMethods());
			level = builder.value();
		}

		public IBuilderData collect() {
			typeNode.traverse(fieldCollector);
			typeNode.traverse(extensionCollector.withRequiredFieldNames(fieldCollector.getAllRequiredFieldNames()));
			return this;
		}

		@Override
		public List<FieldDeclaration> getAllFields() {
			List<FieldDeclaration> allFields = new ArrayList<FieldDeclaration>(getRequiredFields());
			allFields.addAll(getOptionalFields());
			return allFields;
		}

		@Override
		public int getCreateModifier() {
			return toEclipseModifier(level);
		}
	}

	@Getter
	private static class FieldCollector extends EclipseASTAdapterWithTypeDepth {
		private final Set<String> allRequiredFieldNames = new HashSet<String>();
		private final List<FieldDeclaration> requiredFields = new ArrayList<FieldDeclaration>();
		private final List<FieldDeclaration> optionalFields = new ArrayList<FieldDeclaration>();
		private final List<ExpressionBuilder<? extends TypeReference>> requiredFieldDefTypes = new ArrayList<ExpressionBuilder<? extends TypeReference>>();
		private final List<String> requiredFieldDefTypeNames = new ArrayList<String>();
		private final boolean generateConvenientMethodsEnabled;
		private final Set<String> exclude;

		public FieldCollector(Builder builder) {
			super(1);
			exclude = new HashSet<String>(Arrays.asList(builder.exclude()));
			generateConvenientMethodsEnabled = builder.convenientMethods();
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
		private final List<MethodDeclaration> requiredFieldExtensions = new ArrayList<MethodDeclaration>();
		@Getter
		private final List<MethodDeclaration> optionalFieldExtensions = new ArrayList<MethodDeclaration>();
		private final Set<String> allRequiredFieldNames = new HashSet<String>();
		private final Set<String> requiredFieldNames = new HashSet<String>();
		private boolean isExtensionMethod;
		private boolean containsRequiredFields;

		public ExtensionCollector() {
			super(1);
		}

		public ExtensionCollector withRequiredFieldNames(final Set<String> fieldNames) {
			allRequiredFieldNames.clear();
			allRequiredFieldNames.addAll(fieldNames);
			return this;
		}

		@Override public void visitMethod(EclipseNode methodNode, AbstractMethodDeclaration method) {
			if (isOfInterest()) {
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
			if (isOfInterest() && (isExtensionMethod)) {
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
			if (isOfInterest() && (isExtensionMethod) && (method instanceof MethodDeclaration)) {
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
			if (isOfInterest()) {
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
				typeNode.rebuild();
			}
			super.endVisitType(typeNode, type);
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
	}

	private static interface IBuilderData extends IBuilderExtensionData {
		public EclipseNode getTypeNode();

		public ASTNode getSource();

		public int getCreateModifier();

		public String getPrefix();

		public List<String> getCallMethods();
		
		public List<FieldDeclaration> getRequiredFields();

		public List<FieldDeclaration> getOptionalFields();

		public List<FieldDeclaration> getAllFields();

		public List<ExpressionBuilder<? extends TypeReference>> getRequiredFieldDefTypes();
		
		public List<String> getRequiredFieldDefTypeNames();

		public boolean isGenerateConvenientMethodsEnabled();
	}
	
	private static interface IBuilderExtensionData {
		public List<MethodDeclaration> getRequiredFieldExtensions();

		public List<MethodDeclaration> getOptionalFieldExtensions();
	}
}
