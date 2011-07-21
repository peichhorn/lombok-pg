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

import static lombok.ast.AST.*;
import static lombok.ast.Wildcard.Bound.EXTENDS;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.javac.handlers.Javac.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static com.sun.tools.javac.code.Flags.*;

import java.util.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.javac.Javac;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacMethod;
import lombok.javac.handlers.ast.JavacType;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import org.mangosdk.spi.ProviderFor;

public class HandleBuilderAndExtension {
	private final static String OPTIONAL_DEF = "$OptionalDef";
	private final static String BUILDER = "$Builder";

	/**
	 * Handles the {@code lombok.Builder} annotation for javac.
	 */
	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleBuilder extends JavacAnnotationHandler<Builder> {

		@Override public void handle(AnnotationValues<Builder> annotation, JCAnnotation source, JavacNode annotationNode) {
			deleteAnnotationIfNeccessary(annotationNode, Builder.class);
			final JavacType type = JavacType.typeOf(annotationNode, source);

			if (type.isInterface() || type.isEnum() || type.isAnnotation()) {
				annotationNode.addError(canBeUsedOnClassOnly(Builder.class));
				return;
			}

			switch (methodExists(decapitalize(type.name()), type.node(), false)) {
			case EXISTS_BY_LOMBOK:
				return;
			case EXISTS_BY_USER:
				annotationNode.addWarning(String.format("Not generating 'public static %s %s()' A method with that name already exists", BUILDER, decapitalize(type.name())));
				return;
			default:
			case NOT_EXISTS:
				//continue with creating the builder
			}

			new HandleBuilderAndExtension().handleBuilder(new BuilderDataCollector(type, annotation.getInstance()).collect());
		}
	}

	/**
	 * Handles the {@code lombok.Builder.Extension} annotation for javac.
	 */
	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleBuilderExtension extends JavacAnnotationHandler<Builder.Extension> {

		@Override public void handle(AnnotationValues<Builder.Extension> annotation, JCAnnotation source, JavacNode annotationNode) {
			final Class<? extends java.lang.annotation.Annotation> annotationType = Builder.Extension.class;
			deleteAnnotationIfNeccessary(annotationNode, annotationType);

			final JavacMethod method = JavacMethod.methodOf(annotationNode, source);

			if (method == null) {
				annotationNode.addError(canBeUsedOnMethodOnly(annotationType));
				return;
			}
			if (method.isAbstract() || method.isEmpty()) {
				annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
				return;
			}

			final JavacNode typeNode = typeNodeOf(annotationNode);
			JavacNode builderNode = null;

			for (JavacNode child : typeNode.down()) {
				if (child.getKind() != Kind.ANNOTATION) continue;
				if (Javac.annotationTypeMatches(Builder.class, child)) {
					builderNode = child;
				}
			}

			if (builderNode == null) {
				annotationNode.addError("@Builder.Extension is only allowed in types annotated with @Builder");
				return;
			}
			AnnotationValues<Builder> builderAnnotation = Javac.createAnnotation(Builder.class, builderNode);
			if (methodExists(decapitalize(typeNode.getName()), typeNode, false) == MemberExistsResult.NOT_EXISTS) {
				new HandleBuilder().handle(builderAnnotation, (JCAnnotation)builderNode.get(), builderNode);
			}

			new HandleBuilderAndExtension().handleExtension(new BuilderDataCollector(JavacType.typeOf(typeNode, source), builderAnnotation.getInstance()).collect(), method);
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

	public void handleExtension(final IBuilderData builderData, final JavacMethod method) {
		JavacType type = builderData.getType();
		ExtensionCollector extensionCollector = new ExtensionCollector().withRequiredFieldNames(builderData.getAllRequiredFieldNames());
		method.node().traverse(extensionCollector);
		if (extensionCollector.isExtension()) {
			JavacType builderType = type.memberType(BUILDER);
			JavacType interfaceType;
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
		JavacType type = builderData.getType();
		ConstructorDecl constructorDecl = ConstructorDecl(type.name()).makePrivate().withArgument(Arg(Type(BUILDER), "builder").makeFinal()).withImplicitSuper();
		for (final JCVariableDecl field : builderData.getAllFields()) {
			final String fieldName = field.name.toString();
			constructorDecl.withStatement(Assign(Field(This(), fieldName), Field(Name("builder"), fieldName)));
		}
		type.injectConstructor(constructorDecl);
	}

	private void createInitializeBuilderMethod(final IBuilderData builderData, final TypeRef fieldDefType) {
		final JavacType type = builderData.getType();
		type.injectMethod(MethodDecl(fieldDefType, decapitalize(type.name())).makeStatic().withAccessLevel(builderData.getLevel()).withStatement(Return(New(Type(BUILDER)))));
	}

	private void createRequiredFieldInterfaces(IBuilderData builderData, List<AbstractMethodDecl<?>> builderMethods) {
		List<JCVariableDecl> fields = builderData.getRequiredFields();
		if (!fields.isEmpty()) {
			JavacType type = builderData.getType();
			List<String> names = builderData.getRequiredFieldDefTypeNames();
			JCVariableDecl field = fields.get(0);
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
		JavacType type = builderData.getType();
		List<AbstractMethodDecl<?>> interfaceMethods = new ArrayList<AbstractMethodDecl<?>>();
		for (JCVariableDecl field : builderData.getOptionalFields()) {
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

	private void createFluentSetter(IBuilderData builderData, String typeName, JCVariableDecl field, List<AbstractMethodDecl<?>> interfaceMethods, List<AbstractMethodDecl<?>> builderMethods) {
		String fieldName = field.name.toString();
		String methodName = camelCase(builderData.getPrefix(), fieldName);
		final Argument arg0 = Arg(Type(field.vartype), fieldName).makeFinal();
		builderMethods.add(MethodDecl(Type(typeName), methodName).makePublic().implementing().withArgument(arg0) //
			.withStatement(Assign(Field(This(), fieldName), Name(fieldName))) //
			.withStatement(Return(This())));
		interfaceMethods.add(MethodDecl(Type(typeName), methodName).makePublic().withNoBody().withArgument(arg0));
	}

	private void createCollectionMethods(IBuilderData builderData, JCVariableDecl field, List<AbstractMethodDecl<?>> interfaceMethods, List<AbstractMethodDecl<?>> builderMethods) {
		TypeRef elementType = Type("java.lang.Object");
		TypeRef collectionType = Type("java.util.Collection");
		Object[] typeArguments = getTypeArguments(field.vartype);
		if ((typeArguments != null) && (typeArguments.length == 1)) {
			elementType = Type(typeArguments[0]);
			collectionType.withTypeArgument(Wildcard(EXTENDS, elementType));
		}

		String fieldName = field.name.toString();

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

	private void createMapMethods(IBuilderData builderData, JCVariableDecl field, List<AbstractMethodDecl<?>> interfaceMethods, List<AbstractMethodDecl<?>> builderMethods) {
		TypeRef keyType = Type("java.lang.Object");
		TypeRef valueType = Type("java.lang.Object");
		TypeRef mapType = Type("java.util.Map");
		Object[] typeArguments = getTypeArguments(field.vartype);
		if ((typeArguments != null) && (typeArguments.length == 2)) {
			keyType = Type(typeArguments[0]);
			valueType = Type(typeArguments[1]);
			mapType.withTypeArgument(Wildcard(EXTENDS, keyType)) //
				.withTypeArgument(Wildcard(EXTENDS, valueType));
		}

		String fieldName = field.name.toString();

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

	private JCExpression[] getTypeArguments(JCExpression type) {
		if (type instanceof JCTypeApply) {
			return ((JCTypeApply) type).arguments.toArray(new JCExpression[0]);
		}
		return null;
	}

	private void createBuildMethod(IBuilderData builderData, String typeName, List<AbstractMethodDecl<?>> interfaceMethods, List<AbstractMethodDecl<?>> builderMethods) {
		builderMethods.add(MethodDecl(Type(typeName), "build").makePublic().implementing() //
			.withStatement(Return(New(Type(typeName)).withArgument(This()))));
		interfaceMethods.add(MethodDecl(Type(typeName), "build").makePublic().withNoBody());
	}

	private void createMethodCall(IBuilderData builderData, String methodName, List<AbstractMethodDecl<?>> interfaceMethods, List<AbstractMethodDecl<?>> builderMethods) {
		JavacType type = builderData.getType();

		TypeRef returnType = Type("void");
		List<TypeRef> thrownExceptions = new ArrayList<TypeRef>();
		if ("toString".equals(methodName)) {
			returnType = Type("java.lang.String");
		} else {
			for (JavacMethod method : type.methods()) {
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
		JavacType type = builderData.getType();
		builderMethods.add(ConstructorDecl(BUILDER).makePrivate().withImplicitSuper());
		type.injectType(ClassDecl(BUILDER).makePrivate().makeStatic().implementing(interfaceTypes) //
			.withFields(createBuilderFields(builderData)).withMethods(builderMethods));
	}

	private List<FieldDecl> createBuilderFields(IBuilderData builderData) {
		List<FieldDecl> fields = new ArrayList<FieldDecl>();
		for (JCVariableDecl field : builderData.getAllFields()) {
			FieldDecl builder = FieldDecl(Type(field.vartype), field.name.toString()).makePrivate();
			if (field.init != null) {
				builder.withInitialization(Expr(field.init));
				field.init = null;
			}
			fields.add(builder);
		}
		return fields;
	}

	private static boolean isInitializedMapOrCollection(JCVariableDecl field) {
		return (isMap(field) || isCollection(field)) && (field.init != null);
	}

	private static boolean isCollection(JCVariableDecl field) {
		String type = getTypeStringOf(field);
		return type.startsWith("java.util") && (type.endsWith("Collection") || type.endsWith("List") || type.endsWith("Set"));
	}

	private static boolean isMap(JCVariableDecl field) {
		String type = getTypeStringOf(field);
		return type.startsWith("java.util") && type.endsWith("Map");
	}

	private static String getTypeStringOf(JCVariableDecl field) {
		if (field.vartype instanceof JCTypeApply) {
			return ((JCTypeApply)field.vartype).clazz.type.toString();
		} else {
			return field.vartype.type.toString();
		}
	}

	@Getter
	private static class BuilderDataCollector extends JavacASTAdapterWithTypeDepth implements IBuilderData {
		private final List<JCVariableDecl> requiredFields = new ArrayList<JCVariableDecl>();
		private final List<JCVariableDecl> optionalFields = new ArrayList<JCVariableDecl>();
		private final List<TypeRef> requiredFieldDefTypes = new ArrayList<TypeRef>();
		private final List<String> allRequiredFieldNames = new ArrayList<String>();
		private final List<String> requiredFieldDefTypeNames = new ArrayList<String>();;
		private final JavacType type;
		private final String prefix;
		private final List<String> callMethods;
		private final boolean generateConvenientMethodsEnabled;
		private final AccessLevel level;
		private final Set<String> excludes;

		public BuilderDataCollector(JavacType type, Builder builder) {
			super(1);
			this.type = type;
			excludes = new HashSet<String>(Arrays.asList(builder.exclude()));
			generateConvenientMethodsEnabled = builder.convenientMethods();
			prefix = builder.prefix();
			callMethods = Arrays.asList(builder.callMethods());
			level = builder.value();
		}

		public IBuilderData collect() {
			type.node().traverse(this);
			return this;
		}

		@Override
		public List<JCVariableDecl> getAllFields() {
			List<JCVariableDecl> allFields = new ArrayList<JCVariableDecl>(getRequiredFields());
			allFields.addAll(getOptionalFields());
			return allFields;
		}

		@Override public void visitField(JavacNode fieldNode, JCVariableDecl field) {
			if (isOfInterest()) {
				if ((field.mods.flags & STATIC) != 0) return;
				String fieldName = field.name.toString();
				if (excludes.contains(fieldName)) return;
				if ((field.init == null) && ((field.mods.flags & FINAL) != 0)) {
					requiredFields.add(field);
					allRequiredFieldNames.add(fieldName);
					String typeName = camelCase("$", fieldName, "def");
					requiredFieldDefTypeNames.add(typeName);
					requiredFieldDefTypes.add(Type(typeName));
				}
				boolean append = isInitializedMapOrCollection(field) && generateConvenientMethodsEnabled;
				append |= (field.mods.flags & FINAL) == 0;
				if (append) optionalFields.add(field);
			}
		}
	}

	private static class ExtensionCollector extends JavacASTAdapterWithTypeDepth {
		private final Set<String> allRequiredFieldNames = new HashSet<String>();
		private final Set<String> requiredFieldNames = new HashSet<String>();
		@Getter
		private boolean isRequiredFieldsExtension;
		@Getter
		private boolean isExtension;
		private boolean containsRequiredFields;

		public ExtensionCollector() {
			super(1);
		}

		public ExtensionCollector withRequiredFieldNames(final List<String> fieldNames) {
			allRequiredFieldNames.clear();
			allRequiredFieldNames.addAll(fieldNames);
			return this;
		}

		@Override public void visitMethod(JavacNode methodNode, JCMethodDecl method) {
			if (isOfInterest() && !"<init>".equals(method.name.toString())) {
				containsRequiredFields = false;
				isRequiredFieldsExtension = false;
				isExtension = false;
				requiredFieldNames.clear();
				requiredFieldNames.addAll(allRequiredFieldNames);
			}
		}

		@Override public void visitStatement(JavacNode statementNode, JCTree statement) {
			if (isOfInterest()) {
				if (statement instanceof JCAssign) {
					JCAssign assign = (JCAssign) statement;
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

		@Override public void endVisitMethod(JavacNode methodNode, JCMethodDecl method) {
			if (isOfInterest() && !"<init>".equals(method.name.toString())) {
				if (((method.mods.flags & PRIVATE) != 0) && "void".equals(method.restype.toString())) {
					if (containsRequiredFields) {
						if (requiredFieldNames.isEmpty()) {
							isRequiredFieldsExtension = true;
							isExtension = true;
						} else {
							methodNode.addWarning("@Builder.Extension: The method '" + methodNode.getName() + "' does not contain all required fields and was skipped.");
						}
					} else {
						isExtension = true;
					}
				} else {
					methodNode.addWarning("@Builder.Extension: The method '" + methodNode.getName() + "' is not a valid extension and was skipped.");
				}
			}
		}
	}

	private static interface IBuilderData {
		public JavacType getType();

		public AccessLevel getLevel();

		public String getPrefix();

		public List<String> getCallMethods();

		public List<JCVariableDecl> getAllFields();

		public List<JCVariableDecl> getRequiredFields();

		public List<JCVariableDecl> getOptionalFields();

		public List<TypeRef> getRequiredFieldDefTypes();

		public List<String> getAllRequiredFieldNames();

		public List<String> getRequiredFieldDefTypeNames();

		public boolean isGenerateConvenientMethodsEnabled();
	}
}
