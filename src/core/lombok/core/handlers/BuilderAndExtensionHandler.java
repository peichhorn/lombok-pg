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
package lombok.core.handlers;

import static lombok.ast.AST.*;
import static lombok.ast.Wildcard.Bound.EXTENDS;
import static lombok.core.util.Names.*;

import java.util.ArrayList;
import java.util.List;

import lombok.*;
import lombok.ast.*;

public abstract class BuilderAndExtensionHandler<TYPE_TYPE extends IType<METHOD_TYPE, ?, ?, ?, ?>, METHOD_TYPE extends IMethod<TYPE_TYPE, ?, ?, ?>, FIELD_TYPE> {
	public static final String OPTIONAL_DEF = "$OptionalDef";
	public static final String BUILDER = "$Builder";

	public void handleBuilder(final IBuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData) {
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

	public void handleExtension(final IBuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final METHOD_TYPE method, final IParameterSanitizer<METHOD_TYPE> sanitizer) {
		TYPE_TYPE type = builderData.getType();
		IExtensionCollector extensionCollector = builderData.getExtensionCollector().withRequiredFieldNames(builderData.getAllRequiredFieldNames());
		collectExtensions(method, extensionCollector);
		if (extensionCollector.isExtension()) {
			TYPE_TYPE builderType = type.<TYPE_TYPE>memberType(BUILDER);
			TYPE_TYPE interfaceType;
			if (extensionCollector.isRequiredFieldsExtension()) {
				interfaceType = type.<TYPE_TYPE>memberType(builderData.getRequiredFieldDefTypeNames().get(0));
			} else {
				interfaceType = type.<TYPE_TYPE>memberType(OPTIONAL_DEF);
			}
			String methodName = method.name();
			builderType.injectMethod(MethodDecl(Type(OPTIONAL_DEF), methodName).makePublic().implementing().withArguments(method.arguments(true)) //
					.withStatements(method.statements()) //
					.withStatement(Return(This())));
			interfaceType.injectMethod(MethodDecl(Type(OPTIONAL_DEF), method.name()).makePublic().withNoBody().withArguments(method.arguments(true)));
			type.removeMethod(method);
		}
	}

	private void createConstructor(final IBuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData) {
		TYPE_TYPE type = builderData.getType();
		ConstructorDecl constructorDecl = ConstructorDecl(type.name()).makePrivate().withArgument(Arg(Type(BUILDER), "builder").makeFinal()).withImplicitSuper();
		for (final FIELD_TYPE field : builderData.getAllFields()) {
			final String fieldName = name(field);
			constructorDecl.withStatement(Assign(Field(This(), fieldName), Field(Name("builder"), fieldName)));
		}
		type.injectConstructor(constructorDecl);
	}

	private void createInitializeBuilderMethod(final IBuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final TypeRef fieldDefType) {
		final TYPE_TYPE type = builderData.getType();
		type.injectMethod(MethodDecl(fieldDefType, decapitalize(type.name())).makeStatic().withAccessLevel(builderData.getLevel()).withStatement(Return(New(Type(BUILDER)))));
	}

	private void createRequiredFieldInterfaces(final IBuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final List<AbstractMethodDecl<?>> builderMethods) {
		List<FIELD_TYPE> fields = builderData.getRequiredFields();
		if (!fields.isEmpty()) {
			TYPE_TYPE type = builderData.getType();
			List<String> names = builderData.getRequiredFieldDefTypeNames();
			FIELD_TYPE field = fields.get(0);
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

	private void createOptionalFieldInterface(final IBuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final List<AbstractMethodDecl<?>> builderMethods) {
		TYPE_TYPE type = builderData.getType();
		List<AbstractMethodDecl<?>> interfaceMethods = new ArrayList<AbstractMethodDecl<?>>();
		for (FIELD_TYPE field : builderData.getOptionalFields()) {
			if (isInitializedMapOrCollection(field)) {
				if (builderData.isGenerateConvenientMethodsEnabled()) {
					if (isCollection(field)) {
						createCollectionMethods(builderData, field, interfaceMethods, builderMethods);
					}  else if (isMap(field)) {
						createMapMethods(builderData, field, interfaceMethods, builderMethods);
					}
				}
			} else {
				createFluentSetter(builderData, OPTIONAL_DEF, field, interfaceMethods, builderMethods);
			}
		}

		createBuildMethod(builderData, type.name(), interfaceMethods, builderMethods);

		for (String callMethod : builderData.getCallMethods()) {
			createMethodCall(builderData, callMethod, interfaceMethods, builderMethods);
		}

		type.injectType(InterfaceDecl(OPTIONAL_DEF).makePublic().makeStatic().withMethods(interfaceMethods));
	}

	private void createFluentSetter(final IBuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final String typeName, final FIELD_TYPE field,
			final List<AbstractMethodDecl<?>> interfaceMethods, final List<AbstractMethodDecl<?>> builderMethods) {
		String fieldName = name(field);
		String methodName = camelCase(builderData.getPrefix(), fieldName);
		final Argument arg0 = Arg(Type(type(field)), fieldName).makeFinal();
		builderMethods.add(MethodDecl(Type(typeName), methodName).makePublic().implementing().withArgument(arg0) //
			.withStatement(Assign(Field(This(), fieldName), Name(fieldName))) //
			.withStatement(Return(This())));
		interfaceMethods.add(MethodDecl(Type(typeName), methodName).makePublic().withNoBody().withArgument(arg0));
	}

	private void createCollectionMethods(final IBuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final FIELD_TYPE field,
			final List<AbstractMethodDecl<?>> interfaceMethods, final List<AbstractMethodDecl<?>> builderMethods) {
		TypeRef elementType = Type("java.lang.Object");
		TypeRef collectionType = Type("java.util.Collection");
		Object[] typeArguments = getTypeArguments(type(field));
		if ((typeArguments != null) && (typeArguments.length == 1)) {
			elementType = Type(typeArguments[0]);
			collectionType.withTypeArgument(Wildcard(EXTENDS, elementType));
		}

		String fieldName = name(field);

		{ // add
			String addMethodName = singular(camelCase(builderData.getPrefix(), fieldName));
			final Argument arg0 = Arg(elementType, "arg0").makeFinal();
			builderMethods.add(MethodDecl(Type(OPTIONAL_DEF), addMethodName).makePublic().implementing().withArgument(arg0) //
				.withStatement(Call(Field(This(), fieldName), "add").withArgument(Name("arg0"))) //
				.withStatement(Return(This())));
			interfaceMethods.add(MethodDecl(Type(OPTIONAL_DEF), addMethodName).makePublic().withNoBody().withArgument(arg0));
		}
		{ // addAll
			String addAllMethodName = camelCase(builderData.getPrefix(), fieldName);
			final Argument arg0 = Arg(collectionType, "arg0").makeFinal();
			builderMethods.add(MethodDecl(Type(OPTIONAL_DEF), addAllMethodName).makePublic().implementing().withArgument(arg0) //
				.withStatement(Call(Field(This(), fieldName), "addAll").withArgument(Name("arg0"))) //
				.withStatement(Return(This())));
			interfaceMethods.add(MethodDecl(Type(OPTIONAL_DEF), addAllMethodName).makePublic().withNoBody().withArgument(arg0));
		}
	}

	private void createMapMethods(final IBuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final FIELD_TYPE field, final List<AbstractMethodDecl<?>> interfaceMethods,
			final List<AbstractMethodDecl<?>> builderMethods) {
		TypeRef keyType = Type("java.lang.Object");
		TypeRef valueType = Type("java.lang.Object");
		TypeRef mapType = Type("java.util.Map");
		Object[] typeArguments = getTypeArguments(type(field));
		if ((typeArguments != null) && (typeArguments.length == 2)) {
			keyType = Type(typeArguments[0]);
			valueType = Type(typeArguments[1]);
			mapType.withTypeArgument(Wildcard(EXTENDS, keyType)) //
				.withTypeArgument(Wildcard(EXTENDS, valueType));
		}

		String fieldName = name(field);

		{ // put
			final String putMethodName = singular(camelCase(builderData.getPrefix(), fieldName));
			final Argument arg0 = Arg(keyType, "arg0").makeFinal();
			final Argument arg1 = Arg(valueType, "arg1").makeFinal();
			builderMethods.add(MethodDecl(Type(OPTIONAL_DEF), putMethodName).makePublic().implementing().withArgument(arg0).withArgument(arg1) //
				.withStatement(Call(Field(This(), fieldName), "put").withArgument(Name("arg0")).withArgument(Name("arg1"))) //
				.withStatement(Return(This())));
			interfaceMethods.add(MethodDecl(Type(OPTIONAL_DEF), putMethodName).makePublic().withNoBody().withArgument(arg0).withArgument(arg1));
		}
		{ // putAll
			String putAllMethodName = camelCase(builderData.getPrefix(), fieldName);
			final Argument arg0 = Arg(mapType, "arg0").makeFinal();
			builderMethods.add(MethodDecl(Type(OPTIONAL_DEF), putAllMethodName).makePublic().implementing().withArgument(arg0) //
				.withStatement(Call(Field(This(), fieldName), "putAll").withArgument(Name("arg0"))) //
				.withStatement(Return(This())));
			interfaceMethods.add(MethodDecl(Type(OPTIONAL_DEF), putAllMethodName).makePublic().withNoBody().withArgument(arg0));
		}
	}

	private void createBuildMethod(final IBuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final String typeName, final List<AbstractMethodDecl<?>> interfaceMethods,
			final List<AbstractMethodDecl<?>> builderMethods) {
		builderMethods.add(MethodDecl(Type(typeName), "build").makePublic().implementing() //
			.withStatement(Return(New(Type(typeName)).withArgument(This()))));
		interfaceMethods.add(MethodDecl(Type(typeName), "build").makePublic().withNoBody());
	}

	private void createMethodCall(final IBuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final String methodName, final List<AbstractMethodDecl<?>> interfaceMethods,
			final List<AbstractMethodDecl<?>> builderMethods) {
		TYPE_TYPE type = builderData.getType();

		TypeRef returnType = Type("void");
		boolean returnsVoid = true;
		List<TypeRef> thrownExceptions = new ArrayList<TypeRef>();
		if ("toString".equals(methodName)) {
			returnType = Type("java.lang.String");
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

	private void createBuilder(final IBuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData, final List<TypeRef> interfaceTypes,
			final List<AbstractMethodDecl<?>> builderMethods) {
		TYPE_TYPE type = builderData.getType();
		builderMethods.add(ConstructorDecl(BUILDER).makePrivate().withImplicitSuper());
		type.injectType(ClassDecl(BUILDER).makePrivate().makeStatic().implementing(interfaceTypes) //
			.withFields(createBuilderFields(builderData)).withMethods(builderMethods));
	}

	private List<FieldDecl> createBuilderFields(final IBuilderData<TYPE_TYPE, METHOD_TYPE, FIELD_TYPE> builderData) {
		List<FieldDecl> fields = new ArrayList<FieldDecl>();
		for (FIELD_TYPE field : builderData.getAllFields()) {
			FieldDecl builder = FieldDecl(Type(type(field)), name(field)).makePrivate();
			if (getFieldInitialization(field) != null) {
				builder.withInitialization(Expr(getFieldInitialization(field)));
				setFieldInitialization(field, null);
			}
			fields.add(builder);
		}
		return fields;
	}
	
	public boolean isInitializedMapOrCollection(final FIELD_TYPE field) {
		return (isMap(field) || isCollection(field)) && (getFieldInitialization(field) != null);
	}

	private boolean isCollection(final FIELD_TYPE field) {
		String type = typeStringOf(field);
		return (type.endsWith("Collection") || type.endsWith("List") || type.endsWith("Set"));
	}

	private boolean isMap(final FIELD_TYPE field) {
		String type = typeStringOf(field);
		return type.endsWith("Map");
	}
	
	protected abstract void collectExtensions(METHOD_TYPE method, IExtensionCollector collector);

	protected abstract Object[] getTypeArguments(Object type);

	protected abstract String name(Object object);

	protected abstract Object type(FIELD_TYPE field);
	
	protected abstract String typeStringOf(FIELD_TYPE field);

	protected abstract Object getFieldInitialization(FIELD_TYPE field);

	protected abstract void setFieldInitialization(FIELD_TYPE field, Object init);

	public static interface IExtensionCollector {
		public IExtensionCollector withRequiredFieldNames(final List<String> fieldNames);

		public boolean isRequiredFieldsExtension();

		public boolean isExtension();
	}

	public static interface IBuilderData<TYPE_TYPE extends IType<METHOD_TYPE, ?, ?, ?, ?>, METHOD_TYPE extends IMethod<TYPE_TYPE, ?, ?, ?>, FIELD_TYPE> {

		public TYPE_TYPE getType();

		public AccessLevel getLevel();

		public String getPrefix();

		public IExtensionCollector getExtensionCollector();

		public List<String> getCallMethods();

		public List<FIELD_TYPE> getAllFields();

		public List<FIELD_TYPE> getRequiredFields();

		public List<FIELD_TYPE> getOptionalFields();

		public List<TypeRef> getRequiredFieldDefTypes();

		public List<String> getAllRequiredFieldNames();

		public List<String> getRequiredFieldDefTypeNames();

		public boolean isGenerateConvenientMethodsEnabled();
	}
}
