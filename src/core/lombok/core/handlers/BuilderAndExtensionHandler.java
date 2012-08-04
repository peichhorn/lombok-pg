/*
 * Copyright © 2011-2012 Philipp Eichhorn
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
package lombok.core.handlers;

import static lombok.ast.AST.*;
import static lombok.ast.IMethod.ArgumentStyle.INCLUDE_ANNOTATIONS;
import static lombok.ast.Wildcard.Bound.EXTENDS;
import static lombok.core.TransformationsUtil.NON_NULL_PATTERN;
import static lombok.core.util.Names.*;

import java.util.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.util.Is;
import lombok.core.util.Names;

public class BuilderAndExtensionHandler<TYPE_TYPE extends IType<METHOD_TYPE, FIELD_TYPE, ?, ?, ?, ?>, METHOD_TYPE extends IMethod<TYPE_TYPE, ?, ?, ?>, FIELD_TYPE extends IField<?, ?, ?, ?>> {
	public static final String OPTIONAL_DEF = "OptionalDef";
	public static final String BUILDER = "$Builder";

	public void handleBuilder(final TYPE_TYPE type, final Builder builder) {
		final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData = new BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE>(type, builder).collect();
		final List<TypeRef> interfaceTypes = new ArrayList<TypeRef>(builderData.getRequiredFieldDefTypes());
		interfaceTypes.add(Type(OPTIONAL_DEF));
		for (TypeRef interfaceType : interfaceTypes) interfaceType.withTypeArguments(type.typeArguments());
		final List<AbstractMethodDecl<?>> builderMethods = new ArrayList<AbstractMethodDecl<?>>();

		createConstructor(builderData);
		createInitializeBuilderMethod(builderData);
		createRequiredFieldInterfaces(builderData, builderMethods);
		createOptionalFieldInterface(builderData, builderMethods);
		createBuilder(builderData, interfaceTypes, builderMethods);
	}

	public void handleExtension(final TYPE_TYPE type, final METHOD_TYPE method, final IParameterValidator<METHOD_TYPE> validation,
			final IParameterSanitizer<METHOD_TYPE> sanitizer, final Builder builder, final Builder.Extension extension) {
		TYPE_TYPE builderType = type.<TYPE_TYPE> memberType(BUILDER);
		final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData = new BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE>(type, builder).collect();

		final ExtensionType extensionType = getExtensionType(method, builderData, extension.fields());
		if (extensionType == ExtensionType.NONE) return;

		TYPE_TYPE interfaceType;
		if (extensionType  == ExtensionType.REQUIRED) {
			interfaceType = type.<TYPE_TYPE> memberType(builderData.getRequiredFieldDefTypeNames().get(0));
		} else {
			interfaceType = type.<TYPE_TYPE> memberType(OPTIONAL_DEF);
		}
		builderType.editor().injectMethod(MethodDecl(Type(OPTIONAL_DEF).withTypeArguments(type.typeArguments()), method.name()).posHint(method.get()).makePublic().implementing().withArguments(method.arguments(INCLUDE_ANNOTATIONS)) //
				.withStatements(validation.validateParameterOf(method)) //
				.withStatements(sanitizer.sanitizeParameterOf(method)) //
				.withStatements(method.statements()) //
				.withStatement(Return(This())));
		interfaceType.editor().injectMethod(MethodDecl(Type(OPTIONAL_DEF).withTypeArguments(type.typeArguments()), method.name()).makePublic().withNoBody().withArguments(method.arguments(INCLUDE_ANNOTATIONS)));
		type.editor().removeMethod(method);
	}

	private ExtensionType getExtensionType(final METHOD_TYPE method, final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final String[] fields) {
		if (method.isConstructor() || (method.accessLevel() != AccessLevel.PRIVATE) || !method.returns("void")) {
			method.node().addWarning("@Builder.Extension: The method '" + method.name() + "' is not a valid extension and was ignored.");
			return ExtensionType.NONE;
		}

		final String[] extensionFieldNames = Is.notEmpty(fields) ? fields : extensionFieldNames(method, builderData);
		List<String> allFieldNames = builderData.getAllFieldNames();
		for (String potentialFieldName : extensionFieldNames) {
			if (!allFieldNames.contains(Names.decapitalize(potentialFieldName))) {
				method.node().addWarning("@Builder.Extension: The method '" + method.name() + "' is not a valid extension and was ignored.");
				return ExtensionType.NONE;
			}
		}

		List<String> requiredFieldNames = builderData.getRequiredFieldNames();
		Set<String> uninitializedRequiredFieldNames = new HashSet<String>();
		for (FIELD_TYPE field : builderData.getAllFields()) {
			if (requiredFieldNames.contains(field.name()) && !field.isInitialized()) {
				uninitializedRequiredFieldNames.add(field.name());
			}
		}

		boolean containsRequiredFields = false;
		for (String potentialFieldName : extensionFieldNames) {
			containsRequiredFields |= uninitializedRequiredFieldNames.remove(Names.decapitalize(potentialFieldName));
		}

		if (containsRequiredFields) {
			if (uninitializedRequiredFieldNames.isEmpty()) {
				return ExtensionType.REQUIRED;
			} else {
				method.node().addWarning("@Builder.Extension: The method '" + method.name() + "' is not a valid extension and was ignored.");
				return ExtensionType.NONE;
			}
		}
		return ExtensionType.OPTIONAL;
	}

	private String[] extensionFieldNames(final METHOD_TYPE method, final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData) {
		final String prefix = builderData.getMethodPrefix();
		String methodName = method.name();
		if (methodName.startsWith(prefix)) {
			methodName = methodName.substring(prefix.length());
		}
		return methodName.split("And");
	}

	private void createConstructor(final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData) {
		TYPE_TYPE type = builderData.getType();

		if (hasCustomConstructor(type)) return;

		ConstructorDecl constructorDecl = ConstructorDecl(type.name()).makePrivate().withArgument(Arg(Type(BUILDER).withTypeArguments(type.typeArguments()), "builder").makeFinal()).withImplicitSuper();
		for (final FIELD_TYPE field : builderData.getAllFields()) {
			if (field.isFinal() && field.isInitialized()) {
				if (isCollection(field)) {
					constructorDecl.withStatement(Call(Field(field.name()), "addAll").withArgument(Field(Name("builder"), field.filteredName())));
				} else if (isMap(field)) {
					constructorDecl.withStatement(Call(Field(field.name()), "putAll").withArgument(Field(Name("builder"), field.filteredName())));
				}
			} else {
				constructorDecl.withStatement(Assign(Field(field.name()), Field(Name("builder"), field.filteredName())));
			}
		}
		type.editor().injectConstructor(constructorDecl);
	}

	private boolean hasCustomConstructor(final IType<METHOD_TYPE, ?, ?, ?, ?, ?> type) {
		for (final METHOD_TYPE method : type.methods()) {
			if (!method.isConstructor()) continue;
			final List<Argument> arguments = method.arguments();
			if (arguments.size() != 1) continue;
			final Argument argument = arguments.get(0);
			final String argumentTypeName = argument.getType().toString();
			if (argumentTypeName.endsWith("Builder")) {
				method.editor().replaceArguments(Arg(Type(BUILDER).withTypeArguments(type.typeArguments()), argument.getName()).makeFinal());
				return true;
			}
		}
		return false;
	}

	private void createInitializeBuilderMethod(final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData) {
		final TYPE_TYPE type = builderData.getType();
		final TypeRef fieldDefType = builderData.getRequiredFields().isEmpty() ? Type(OPTIONAL_DEF) : builderData.getRequiredFieldDefTypes().get(0);
		type.editor().injectMethod(MethodDecl(fieldDefType, decapitalize(type.name())).makeStatic().withAccessLevel(builderData.getLevel()).withTypeParameters(type.typeParameters()) //
				.withStatement(Return(New(Type(BUILDER).withTypeArguments(type.typeArguments())))));
	}

	private void createRequiredFieldInterfaces(final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final List<AbstractMethodDecl<?>> builderMethods) {
		List<FIELD_TYPE> fields = builderData.getRequiredFields();
		if (!fields.isEmpty()) {
			TYPE_TYPE type = builderData.getType();
			List<String> names = builderData.getRequiredFieldDefTypeNames();
			FIELD_TYPE field = fields.get(0);
			String name = names.get(0);
			for (int i = 1, iend = fields.size(); i < iend; i++) {
				List<AbstractMethodDecl<?>> interfaceMethods = new ArrayList<AbstractMethodDecl<?>>();
				createFluentSetter(builderData, names.get(i), field, interfaceMethods, builderMethods);

				if (builderData.isResetAllowed()) {
					createResetMethod(builderData, interfaceMethods, new ArrayList<AbstractMethodDecl<?>>());
				}

				type.editor().injectType(InterfaceDecl(name).makePublic().makeStatic().withTypeParameters(type.typeParameters()).withMethods(interfaceMethods));
				field = fields.get(i);
				name = names.get(i);
			}
			List<AbstractMethodDecl<?>> interfaceMethods = new ArrayList<AbstractMethodDecl<?>>();
			createFluentSetter(builderData, OPTIONAL_DEF, field, interfaceMethods, builderMethods);

			if (builderData.isResetAllowed()) {
				createResetMethod(builderData, interfaceMethods, new ArrayList<AbstractMethodDecl<?>>());
			}

			type.editor().injectType(InterfaceDecl(name).makePublic().makeStatic().withTypeParameters(type.typeParameters()).withMethods(interfaceMethods));
		}
	}

	private void createOptionalFieldInterface(final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final List<AbstractMethodDecl<?>> builderMethods) {
		TYPE_TYPE type = builderData.getType();
		List<AbstractMethodDecl<?>> interfaceMethods = new ArrayList<AbstractMethodDecl<?>>();
		for (FIELD_TYPE field : builderData.getOptionalFields()) {
			if (isInitializedMapOrCollection(field)) {
				if (builderData.isGenerateConvenientMethodsEnabled()) {
					if (isCollection(field)) {
						createCollectionMethods(builderData, field, interfaceMethods, builderMethods);
					} else if (isMap(field)) {
						createMapMethods(builderData, field, interfaceMethods, builderMethods);
					}
				}
			} else {
				createFluentSetter(builderData, OPTIONAL_DEF, field, interfaceMethods, builderMethods);
			}
		}

		createBuildMethod(builderData, type.name(), interfaceMethods, builderMethods);

		if (builderData.isResetAllowed()) {
			createResetMethod(builderData, interfaceMethods, builderMethods);
		}

		for (String callMethod : builderData.getCallMethods()) {
			createMethodCall(builderData, callMethod, interfaceMethods, builderMethods);
		}

		type.editor().injectType(InterfaceDecl(OPTIONAL_DEF).makePublic().makeStatic().withTypeParameters(type.typeParameters()).withMethods(interfaceMethods));
	}

	private void createFluentSetter(final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final String typeName, final FIELD_TYPE field,
			final List<AbstractMethodDecl<?>> interfaceMethods, final List<AbstractMethodDecl<?>> builderMethods) {
		TYPE_TYPE type = builderData.getType();
		String methodName = camelCase(builderData.getMethodPrefix(), field.filteredName());
		final Argument arg0 = Arg(field.type(), field.filteredName()).makeFinal();
		builderMethods.add(MethodDecl(Type(typeName).withTypeArguments(type.typeArguments()), methodName).makePublic().implementing().withArgument(arg0) //
				.withStatement(Assign(Field(field.filteredName()), Name(field.filteredName()))) //
				.withStatement(Return(This())));
		interfaceMethods.add(MethodDecl(Type(typeName).withTypeArguments(type.typeArguments()), methodName).makePublic().withNoBody().withArgument(arg0));
	}

	private void createCollectionMethods(final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final FIELD_TYPE field,
			final List<AbstractMethodDecl<?>> interfaceMethods, final List<AbstractMethodDecl<?>> builderMethods) {
		TYPE_TYPE type = builderData.getType();
		TypeRef elementType = Type(Object.class);
		TypeRef collectionType = Type(Collection.class);
		List<TypeRef> typeArguments = field.typeArguments();
		if (typeArguments.size() == 1) {
			elementType = typeArguments.get(0);
			collectionType.withTypeArgument(Wildcard(EXTENDS, elementType));
		}

		{ // add
			String addMethodName = singular(camelCase(builderData.getMethodPrefix(), field.filteredName()));
			final Argument arg0 = Arg(elementType, "arg0").makeFinal();
			builderMethods.add(MethodDecl(Type(OPTIONAL_DEF).withTypeArguments(type.typeArguments()), addMethodName).makePublic().implementing().withArgument(arg0) //
					.withStatement(Call(Field(field.filteredName()), "add").withArgument(Name("arg0"))) //
					.withStatement(Return(This())));
			interfaceMethods.add(MethodDecl(Type(OPTIONAL_DEF).withTypeArguments(type.typeArguments()), addMethodName).makePublic().withNoBody().withArgument(arg0));
		}
		{ // addAll
			String addAllMethodName = camelCase(builderData.getMethodPrefix(), field.filteredName());
			final Argument arg0 = Arg(collectionType, "arg0").makeFinal();
			builderMethods.add(MethodDecl(Type(OPTIONAL_DEF).withTypeArguments(type.typeArguments()), addAllMethodName).makePublic().implementing().withArgument(arg0) //
					.withStatement(Call(Field(field.filteredName()), "addAll").withArgument(Name("arg0"))) //
					.withStatement(Return(This())));
			interfaceMethods.add(MethodDecl(Type(OPTIONAL_DEF).withTypeArguments(type.typeArguments()), addAllMethodName).makePublic().withNoBody().withArgument(arg0));
		}
	}

	private void createMapMethods(final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final FIELD_TYPE field, final List<AbstractMethodDecl<?>> interfaceMethods,
			final List<AbstractMethodDecl<?>> builderMethods) {
		TYPE_TYPE type = builderData.getType();
		TypeRef keyType = Type(Object.class);
		TypeRef valueType = Type(Object.class);
		TypeRef mapType = Type(Map.class);
		List<TypeRef> typeArguments = field.typeArguments();
		if (typeArguments.size() == 2) {
			keyType = typeArguments.get(0);
			valueType = typeArguments.get(1);
			mapType.withTypeArgument(Wildcard(EXTENDS, keyType)) //
					.withTypeArgument(Wildcard(EXTENDS, valueType));
		}

		{ // put
			final String putMethodName = singular(camelCase(builderData.getMethodPrefix(), field.filteredName()));
			final Argument arg0 = Arg(keyType, "arg0").makeFinal();
			final Argument arg1 = Arg(valueType, "arg1").makeFinal();
			builderMethods.add(MethodDecl(Type(OPTIONAL_DEF).withTypeArguments(type.typeArguments()), putMethodName).makePublic().implementing().withArgument(arg0).withArgument(arg1) //
					.withStatement(Call(Field(field.filteredName()), "put").withArgument(Name("arg0")).withArgument(Name("arg1")))
					.withStatement(Return(This())));
			interfaceMethods.add(MethodDecl(Type(OPTIONAL_DEF).withTypeArguments(type.typeArguments()), putMethodName).makePublic().withNoBody().withArgument(arg0).withArgument(arg1));
		}
		{ // putAll
			String putAllMethodName = camelCase(builderData.getMethodPrefix(), field.filteredName());
			final Argument arg0 = Arg(mapType, "arg0").makeFinal();
			builderMethods.add(MethodDecl(Type(OPTIONAL_DEF).withTypeArguments(type.typeArguments()), putAllMethodName).makePublic().implementing().withArgument(arg0) //
					.withStatement(Call(Field(field.filteredName()), "putAll").withArgument(Name("arg0"))) //
					.withStatement(Return(This())));
			interfaceMethods.add(MethodDecl(Type(OPTIONAL_DEF).withTypeArguments(type.typeArguments()), putAllMethodName).makePublic().withNoBody().withArgument(arg0));
		}
	}

	private void createBuildMethod(final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final String typeName, final List<AbstractMethodDecl<?>> interfaceMethods,
			final List<AbstractMethodDecl<?>> builderMethods) {
		TYPE_TYPE type = builderData.getType();
		builderMethods.add(MethodDecl(Type(typeName).withTypeArguments(type.typeArguments()), "build").makePublic().implementing() //
				.withStatement(Return(New(Type(typeName).withTypeArguments(type.typeArguments())).withArgument(This()))));
		interfaceMethods.add(MethodDecl(Type(typeName).withTypeArguments(type.typeArguments()), "build").makePublic().withNoBody());
	}

	private void createResetMethod(final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final List<AbstractMethodDecl<?>> interfaceMethods,
			final List<AbstractMethodDecl<?>> builderMethods) {
		final TypeRef fieldDefType = builderData.getRequiredFields().isEmpty() ? Type(OPTIONAL_DEF) : builderData.getRequiredFieldDefTypes().get(0);
		MethodDecl methodDecl = MethodDecl(fieldDefType, "reset").makePublic().implementing();
		for (final FIELD_TYPE field : builderData.getAllFields()) {
			if (field.isInitialized()) {
				String fieldDefaultMethodName = "$" + field.filteredName() + "Default";
				methodDecl.withStatement(Assign(Field(field.filteredName()), Call(fieldDefaultMethodName)));
			} else {
				methodDecl.withStatement(Assign(Field(field.filteredName()), DefaultValue(field.type())));
			}
		}
		builderMethods.add(methodDecl.withStatement(Return(This())));
		interfaceMethods.add(MethodDecl(fieldDefType, "reset").makePublic().withNoBody());
	}

	private void createMethodCall(final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final String methodName, final List<AbstractMethodDecl<?>> interfaceMethods,
			final List<AbstractMethodDecl<?>> builderMethods) {
		TYPE_TYPE type = builderData.getType();

		TypeRef returnType = Type("void");
		boolean returnsVoid = true;
		List<TypeRef> thrownExceptions = new ArrayList<TypeRef>();
		if ("toString".equals(methodName)) {
			returnType = Type(String.class);
			returnsVoid = false;
		} else {
			for (METHOD_TYPE method : type.methods()) {
				if (methodName.equals(method.name()) && !method.hasArguments()) {
					returnType = method.returns();
					returnsVoid = method.returns("void");
					thrownExceptions.addAll(method.thrownExceptions());
					break;
				}
			}
		}

		Call call = Call(Call("build"), methodName);
		if (returnsVoid) {
			builderMethods.add(MethodDecl(returnType, methodName).makePublic().implementing().withThrownExceptions(thrownExceptions) //
					.withStatement(call));
		} else {
			builderMethods.add(MethodDecl(returnType, methodName).makePublic().implementing().withThrownExceptions(thrownExceptions) //
					.withStatement(Return(call)));
		}
		interfaceMethods.add(MethodDecl(returnType, methodName).makePublic().withNoBody().withThrownExceptions(thrownExceptions));
	}

	private void createBuilder(final BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final List<TypeRef> interfaceTypes,
			final List<AbstractMethodDecl<?>> builderMethods) {
		TYPE_TYPE type = builderData.getType();
		List<FieldDecl> builderFields = new ArrayList<FieldDecl>();
		List<AbstractMethodDecl<?>> builderFieldDefaultMethods = new ArrayList<AbstractMethodDecl<?>>();
		for (FIELD_TYPE field : builderData.getAllFields()) {
			FieldDecl builderField = FieldDecl(field.type(), field.filteredName()).makePrivate();
			if (field.isInitialized()) {
				String fieldDefaultMethodName = "$" + field.filteredName() + "Default";
				builderFieldDefaultMethods.add(MethodDecl(field.type(), fieldDefaultMethodName).makeStatic().withTypeParameters(type.typeParameters()) //
						.withStatement(Return(field.initialization())));
				builderField.withInitialization(Call(fieldDefaultMethodName));
				field.editor().replaceInitialization(Call(Name(BUILDER), fieldDefaultMethodName));
			}
			builderFields.add(builderField);
		}
		type.editor().injectType(ClassDecl(BUILDER).withTypeParameters(type.typeParameters()).makePrivate().makeStatic().implementing(interfaceTypes) //
				.withFields(builderFields) //
				.withMethods(builderFieldDefaultMethods) //
				.withMethods(builderMethods) //
				.withMethod(ConstructorDecl(BUILDER).makePrivate().withImplicitSuper()));
	}

	private static <FIELD_TYPE extends IField<?, ?, ?, ?>> boolean isInitializedMapOrCollection(final FIELD_TYPE field) {
		return (isMap(field) || isCollection(field)) && field.isInitialized();
	}

	private static <FIELD_TYPE extends IField<?, ?, ?, ?>> boolean isCollection(final FIELD_TYPE field) {
		return (field.isOfType("Collection") || field.isOfType("List") || field.isOfType("Set"));
	}

	private static <FIELD_TYPE extends IField<?, ?, ?, ?>> boolean isMap(final FIELD_TYPE field) {
		return field.isOfType("Map");
	}

	@Getter
	private static class BuilderData<TYPE_TYPE extends IType<METHOD_TYPE, FIELD_TYPE, ?, ?, ?, ?>, METHOD_TYPE extends IMethod<TYPE_TYPE, ?, ?, ?>, FIELD_TYPE extends IField<?, ?, ?, ?>> {
		private final List<FIELD_TYPE> requiredFields = new ArrayList<FIELD_TYPE>();
		private final List<FIELD_TYPE> optionalFields = new ArrayList<FIELD_TYPE>();
		private final List<TypeRef> requiredFieldDefTypes = new ArrayList<TypeRef>();
		private final List<String> requiredFieldNames = new ArrayList<String>();
		private final List<String> optionalFieldNames = new ArrayList<String>();
		private final List<String> requiredFieldDefTypeNames = new ArrayList<String>();;
		private final TYPE_TYPE type;
		private final String methodPrefix;
		private final List<String> callMethods;
		private final boolean generateConvenientMethodsEnabled;
		private final boolean resetAllowed;
		private final AccessLevel level;
		private final Set<String> excludes;

		private BuilderData(final TYPE_TYPE type, final Builder builder) {
			this.type = type;
			excludes = new HashSet<String>(Arrays.asList(builder.exclude()));
			generateConvenientMethodsEnabled = builder.convenientMethods();
			methodPrefix = builder.prefix();
			callMethods = Arrays.asList(builder.callMethods());
			level = builder.value();
			resetAllowed = builder.allowReset();
		}

		public BuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> collect() {
			for (FIELD_TYPE field : type.fields()) {
				if (field.isStatic()) continue;
				String fieldName = field.name();
				if (excludes.contains(fieldName)) continue;
				if ((!field.isInitialized()) && (field.isFinal() || !field.annotations(NON_NULL_PATTERN).isEmpty())) {
					requiredFields.add(field);
					requiredFieldNames.add(fieldName);
					String typeName = capitalize(camelCase(field.filteredName(), "def"));
					requiredFieldDefTypeNames.add(typeName);
					requiredFieldDefTypes.add(Type(typeName));
				} else if ((generateConvenientMethodsEnabled && isInitializedMapOrCollection(field)) || !field.isFinal()) {
					optionalFields.add(field);
					optionalFieldNames.add(fieldName);
				}
			}
			return this;
		}

		public List<FIELD_TYPE> getAllFields() {
			List<FIELD_TYPE> allFields = new ArrayList<FIELD_TYPE>(getRequiredFields());
			allFields.addAll(getOptionalFields());
			return allFields;
		}

		public List<String> getAllFieldNames() {
			List<String> allFieldNames = new ArrayList<String>(getRequiredFieldNames());
			allFieldNames.addAll(getOptionalFieldNames());
			return allFieldNames;
		}
	}

	private enum ExtensionType {
		NONE,
		REQUIRED,
		OPTIONAL;
	}
}
