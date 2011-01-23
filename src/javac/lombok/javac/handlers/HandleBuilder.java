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

import static lombok.core.util.ErrorMessages.canBeUsedOnClassOnly;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static lombok.javac.handlers.JavacTreeBuilder.*;
import static com.sun.tools.javac.code.Flags.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.BuilderExtension;
import lombok.core.AnnotationValues;
import lombok.javac.JavacASTAdapter;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

/**
 * Handles the {@code lombok.Builder} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleBuilder extends JavacNonResolutionBasedHandler implements JavacAnnotationHandler<Builder> {
	private final static String CONSTRUCTOR = "private ctor(final $Builder builder) {%s}";
	private final static String CONSTRUCTOR_ASSIGN = "this.%s = builder.%s;";
	private final static String OPTIONAL_DEF = "$OptionalDef";
	private final static String BUILDER = "$Builder";
	private final static String CREATE_METHOD = "%s static %s create() { return new $Builder(); }";	
	private final static String BUILDER_METHOD_CALL_ARG1 = "public %s %s(final %s arg0) { this.%s.%s(arg0); return this; }";
	private final static String BUILDER_METHOD_ASSIGN_ARG1 = "public %s %s(final %s arg0) { this.%s=arg0; return this; }";
	private final static String BUILDER_METHOD_CALL_ARG2 = "public %s %s(final %s arg0, final %s arg1) { this.%s.%s(arg0, arg1); return this; }";
	private final static String BUILDER_BUILD_METHOD = "public %s build() { return new %s(this); }";
	private final static String BUILDER_TO_STRING_METHOD = "public java.lang.String toString() { return build().toString(); }";
	private final static String BUILDER_METHOD_CALL_AFTER_BUILD = "public %s %s() { %s build().%s(); }";
	
	@Override public boolean handle(AnnotationValues<Builder> annotation, JCAnnotation ast, JavacNode annotationNode) {
		markAnnotationAsProcessed(annotationNode, Builder.class);
		JavacNode typeNode = annotationNode.up();
		
		JCClassDecl typeDecl = null;
		if (typeNode.get() instanceof JCClassDecl) typeDecl = (JCClassDecl)typeNode.get();
		long flags = typeDecl == null ? 0 : typeDecl.mods.flags;
		boolean notAClass = (flags & (INTERFACE | ENUM | ANNOTATION)) != 0;
		
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
		
		handleBuilder(new HandleBuilderDataCollector(typeNode, annotation.getInstance()).collect());
		return true;
	}
	
	private static void handleBuilder(IBuilderData builderData) {
		List<String> requiredFieldDefTypeNames = builderData.getRequiredFieldDefTypeNames();
		List<String> typeNames = new ListBuffer<String>().appendList(requiredFieldDefTypeNames).append(OPTIONAL_DEF).toList();
		String fieldDefTypeName = builderData.getRequiredFields().isEmpty() ? OPTIONAL_DEF : requiredFieldDefTypeNames.head;
		
		JavacNode typeNode = builderData.getTypeNode();
		StringBuilder assignments = new StringBuilder();
		for (JCVariableDecl field  : builderData.getAllFields()) {
			assignments.append(String.format(CONSTRUCTOR_ASSIGN, field.name, field.name));
		}
		constructor(typeNode, CONSTRUCTOR, assignments).inject();
		method(typeNode, CREATE_METHOD, Flags.toString(builderData.getCreateModifier()), fieldDefTypeName).inject();
		
		ListBuffer<JCTree> builderMethods = ListBuffer.lb();
		createRequiredFieldInterfaces(builderData, builderMethods);
		createOptionalFieldInterface(builderData, builderMethods);

		clazz(typeNode, STATIC | PRIVATE, BUILDER).implementing(typeNames)
			.withFields(createBuilderFields(builderData)).withMethods(builderMethods.toList()).inject();
	}
	
	private static List<JCTree> createBuilderFields(IBuilderData builderData) {
		TreeMaker maker = builderData.getTypeNode().getTreeMaker();
		ListBuffer<JCTree> fields = new ListBuffer<JCTree>();
		for (JCVariableDecl field : builderData.getAllFields()) {
			fields.append(maker.VarDef(maker.Modifiers(PRIVATE), field.name, field.vartype, field.init));
			field.init = null;
		}
		return fields.toList();
	}
	
	private static void createRequiredFieldInterfaces(IBuilderData builderData, ListBuffer<JCTree> builderMethods) {
		List<JCVariableDecl> fields = builderData.getRequiredFields();
		if (!fields.isEmpty()) {
			JavacNode typeNode = builderData.getTypeNode();
			List<String> names = builderData.getRequiredFieldDefTypeNames();
			boolean createFieldExtension = true;
			while (names.tail.head != null) {
				ListBuffer<JCTree> interfaceMethods = ListBuffer.lb();
				createFluentSetter(builderData, names.tail.head, fields.head, interfaceMethods, builderMethods);
				if (createFieldExtension) {
					for (JCMethodDecl extension : builderData.getRequiredFieldExtensions()) {
						extension.mods = typeNode.getTreeMaker().Modifiers(PUBLIC);
						extension.restype = chainDotsString(typeNode.getTreeMaker(), typeNode, OPTIONAL_DEF);
						addMethodTo(method(typeNode,extension.toString()).withReturnStatement("this"), interfaceMethods, builderMethods);
					}
					createFieldExtension = false;
				}
				
				interfaze(typeNode, PUBLIC | STATIC, names.head).withMethods(interfaceMethods.toList()).inject();
				names = names.tail;
				fields = fields.tail;
			}
			ListBuffer<JCTree> interfaceMethods = ListBuffer.lb();
			createFluentSetter(builderData, OPTIONAL_DEF, fields.head, interfaceMethods, builderMethods);
			
			interfaze(typeNode, PUBLIC | STATIC, names.head).withMethods(interfaceMethods.toList()).inject();
		}
	}
	
	private static void createOptionalFieldInterface(IBuilderData builderData, ListBuffer<JCTree> builderMethods) {
		ListBuffer<JCTree> interfaceMethods = ListBuffer.lb();
		for (JCVariableDecl field : builderData.getOptionalFields()) {
			if (isInitializedMapOrCollection(field)) {
				if (builderData.generateConvenientMethods()) {
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
		
		JavacNode typeNode = builderData.getTypeNode();
		String typeName = typeNode.getName();
		addMethodTo(method(typeNode, BUILDER_BUILD_METHOD, typeName, typeName), interfaceMethods, builderMethods);
		
		for (String callMethod : builderData.getCallMethods()) {
			createMethodCall(builderData, callMethod, interfaceMethods, builderMethods);
		}
		
		for (JCMethodDecl extension : builderData.getOptionalFieldExtensions()) {
			addMethodTo(method(typeNode, extension.toString()).withMods(PUBLIC).withReturnType(OPTIONAL_DEF).withReturnStatement("this"), 
				interfaceMethods, builderMethods);
		}
		
		interfaze(typeNode, PUBLIC | STATIC, OPTIONAL_DEF).withMethods(interfaceMethods.toList()).inject();
	}

	private static void createFluentSetter(IBuilderData builderData, String nextTypeName, JCVariableDecl field, ListBuffer<JCTree> interfaceMethods,
			ListBuffer<JCTree> builderMethods) {
		String fieldName = field.name.toString();
		String methodName = toCamelCase(false, builderData.getPrefix(), fieldName);
		addMethodTo(method(builderData.getTypeNode(), BUILDER_METHOD_ASSIGN_ARG1, nextTypeName, methodName, field.vartype, fieldName), interfaceMethods, builderMethods);
	}
	
	private static void createCollectionMethods(IBuilderData builderData, JCVariableDecl field, ListBuffer<JCTree> interfaceMethods, ListBuffer<JCTree> builderMethods) {
		JavacNode typeNode = builderData.getTypeNode();
		TreeMaker maker = typeNode.getTreeMaker();
		Object elementType = "java.lang.Object";
		JCExpression collectionType = chainDotsString(maker, typeNode, "java.util.Collection");
		if (field.vartype instanceof JCTypeApply) {
			JCTypeApply typeRef = (JCTypeApply)field.vartype;
			if (typeRef.arguments.size() == 1) {
				ListBuffer<JCExpression> args = ListBuffer.lb();
				elementType = addWildCard(maker, typeRef.arguments.head, args);
				collectionType = maker.TypeApply(collectionType, args.toList());
			}
		}
		
		String addMethodName = toCamelCase(true, builderData.getPrefix(), field.name.toString());
		String addAllMethodName = toCamelCase(false, builderData.getPrefix(), field.name.toString());
		addMethodTo(method(typeNode, BUILDER_METHOD_CALL_ARG1, OPTIONAL_DEF, addMethodName, elementType, field.name, "add"), interfaceMethods, builderMethods);
		addMethodTo(method(typeNode, BUILDER_METHOD_CALL_ARG1, OPTIONAL_DEF, addAllMethodName, collectionType, field.name, "addAll"), interfaceMethods, builderMethods);
	}
	
	private static void createMapMethods(IBuilderData builderData, JCVariableDecl field, ListBuffer<JCTree> interfaceMethods, ListBuffer<JCTree> builderMethods) {
		JavacNode typeNode = builderData.getTypeNode();
		TreeMaker maker = typeNode.getTreeMaker();
		Object keyType = "java.lang.Object";
		Object valueType = "java.lang.Object";
		JCExpression mapType = chainDotsString(maker, typeNode, "java.util.Map");
		if (field.vartype instanceof JCTypeApply) {
			JCTypeApply typeRef = (JCTypeApply)field.vartype;
			if (typeRef.arguments.size() == 2) {
				ListBuffer<JCExpression> args = ListBuffer.lb();
				keyType = addWildCard(maker, typeRef.arguments.head, args);
				valueType = addWildCard(maker, typeRef.arguments.tail.head, args);
				mapType = maker.TypeApply(mapType, args.toList());
			}
		}
		
		String putMethodName = toCamelCase(true, builderData.getPrefix(), field.name.toString());
		String putAllMethodName = toCamelCase(false, builderData.getPrefix(), field.name.toString());
		addMethodTo(method(typeNode, BUILDER_METHOD_CALL_ARG2, OPTIONAL_DEF, putMethodName, keyType, valueType, field.name, "put"), interfaceMethods, builderMethods);
		addMethodTo(method(typeNode, BUILDER_METHOD_CALL_ARG1, OPTIONAL_DEF, putAllMethodName, mapType, field.name, "putAll"), interfaceMethods, builderMethods);
	}
	
	private static void createMethodCall(IBuilderData builderData, String method, ListBuffer<JCTree> interfaceMethods, ListBuffer<JCTree> builderMethods) {
		JavacNode typeNode = builderData.getTypeNode();
		JCClassDecl typeDecl = (JCClassDecl)typeNode.get();
		if ("toString".equals(method)) {
			addMethodTo(method(typeNode, BUILDER_TO_STRING_METHOD), interfaceMethods, builderMethods);
		} else {
			for (JCTree def : typeDecl.defs) {
				if (def instanceof JCMethodDecl) {
					JCMethodDecl m = (JCMethodDecl)def;
					if (method.equals(m.name.toString()) && m.params.isEmpty()) {
						String s = "void".equals(m.restype.toString()) ? "" : "return";
						addMethodTo(method(typeNode, BUILDER_METHOD_CALL_AFTER_BUILD, m.restype, method, s, method).withThrownExceptions(m.thrown),
								interfaceMethods, builderMethods);
						return;
					}
				}
			}
			typeNode.addWarning("@Builder was unable to find method '" + method + "()' within this class.");
		}
	}
	
	private static void addMethodTo(MethodBuilder builder, ListBuffer<JCTree> interfaceMethods, ListBuffer<JCTree> builderMethods) {
		builderMethods.append(builder.build());
		interfaceMethods.append(builder.withMods(InterfaceMethodFlags).withoutBody().build());
	}
	
	private static Object addWildCard(TreeMaker maker, JCExpression type, ListBuffer<JCExpression> args) {
		if (type instanceof JCWildcard) {
			args.append(type);
			return ((JCWildcard)type).inner;
		} else {
			args.append(maker.Wildcard(maker.TypeBoundKind(BoundKind.EXTENDS), type));
			return type;
		}
	}
	
	private static boolean isInitializedMapOrCollection(JCVariableDecl field) {
		return (field.init != null) && (isMap(field) || isCollection(field));
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
	
	private static String toCamelCase(boolean singular, String... strings) {
		StringBuilder builder = new StringBuilder();
		boolean mustCapitalize = false;
		for (String s : strings) {
			if (s.isEmpty()) continue;
			if (mustCapitalize) {
				builder.append(s.substring(0, 1).toUpperCase()).append(s.substring(1));
			} else {
				builder.append(s);
				mustCapitalize = true;
			}
		}
		if (singular && (builder.charAt(builder.length() - 1) == 's')) {
			builder.setLength(builder.length() - 1);
		}
		return builder.toString();
	}
	
	private static class HandleBuilderDataCollector extends JavacASTAdapter implements IBuilderData {
		private final JavacNode typeNode;
		private final Set<String> exclude;
		private final String prefix;
		private final boolean convenientMethods;
		private final List<String> callMethods;
		private final AccessLevel level;
		private final ListBuffer<JCVariableDecl> requiredFields = ListBuffer.lb();
		private final ListBuffer<JCVariableDecl> optionalFields = ListBuffer.lb();
		private final ListBuffer<String> requiredFieldDefTypeNames = ListBuffer.lb();
		private final Set<String> requiredFieldNames = new HashSet<String>();
		private final Set<String> allRequiredFieldNames = new HashSet<String>();
		private final ListBuffer<JCMethodDecl> requiredFieldExtensions = ListBuffer.lb();
		private final ListBuffer<JCMethodDecl> optionalFieldExtensions = ListBuffer.lb();
		private boolean isExtensionMethod;
		private boolean containsRequiredFields;
		private int typeDepth;
		private int phase;
		
		public HandleBuilderDataCollector(JavacNode typeNode, Builder builder) {
			super();
			this.typeNode = typeNode;
			exclude = new HashSet<String>(Arrays.asList(builder.exclude()));
			prefix = builder.prefix();
			convenientMethods = builder.convenientMethods();
			callMethods = List.from(builder.callMethods());
			level = builder.value();
		}
		
		public IBuilderData collect() {
			phase = 1; typeNode.traverse(this);
			phase = 2; typeNode.traverse(this);
			return this;
		}
		
		public JavacNode getTypeNode() {
			return typeNode;
		}
		
		public List<JCVariableDecl> getRequiredFields() {
			return requiredFields.toList();
		}
		
		public List<JCVariableDecl> getOptionalFields() {
			return optionalFields.toList();
		}
		
		public List<JCVariableDecl> getAllFields() {
			return ListBuffer.<JCVariableDecl>lb().appendList(requiredFields).appendList(optionalFields).toList();
		}
		
		public List<String> getRequiredFieldDefTypeNames() {
			return requiredFieldDefTypeNames.toList();
		}
		
		public List<JCMethodDecl> getRequiredFieldExtensions() {
			return requiredFieldExtensions.toList();
		}
		
		public List<JCMethodDecl> getOptionalFieldExtensions() {
			return optionalFieldExtensions.toList();
		}
		
		public long getCreateModifier() {
			return toJavacModifier(level);
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
		
		@Override public void visitType(JavacNode typeNode, JCClassDecl type) {
			typeDepth++;
		}

		@Override public void visitMethod(JavacNode methodNode, JCMethodDecl method) {
			if ((phase == 2) && (typeDepth == 1)) {
				isExtensionMethod = false;
				containsRequiredFields = false;
				requiredFieldNames.clear();
				requiredFieldNames.addAll(allRequiredFieldNames);
				boolean isAnImport = methodNode.getImportStatements().contains(BuilderExtension.class.getName());
				for (JCAnnotation annotation : method.mods.annotations) {
					if (annotation.annotationType.toString().equals(BuilderExtension.class.getName())
							|| (isAnImport && annotation.annotationType.toString().equals(BuilderExtension.class.getSimpleName()))) {
						isExtensionMethod = true;
						return;
					}
				}
			}
		}
		
		@Override public void visitStatement(JavacNode statementNode, JCTree statement) {
			if ((phase == 2) && (typeDepth == 1) && (isExtensionMethod)) {
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
			if ((phase == 2) && (typeDepth == 1) && (isExtensionMethod)) {
				if (((method.mods.flags & PRIVATE) != 0) && "void".equals(method.restype.toString())) {
					if (containsRequiredFields) {
						if (requiredFieldNames.isEmpty()) {
							requiredFieldExtensions.append(method);
						} else {
							methodNode.addWarning("@BuilderExtension: The method '" + methodNode.getName() + "' does not contain all required fields and was skipped.", method);
						}
					} else optionalFieldExtensions.append(method);
				} else methodNode.addWarning("@BuilderExtension:  The method '" + methodNode.getName() + "' is not a valid extension and was skipped.", method);
			}
		}
		
		@Override public void endVisitType(JavacNode typeNode, JCClassDecl type) {
			if ((phase == 2) && (typeDepth == 1)) {
				ListBuffer<JCTree> defs = ListBuffer.lb();
				for (JCTree def : type.defs) {
					if (def instanceof JCMethodDecl) {
						if (!requiredFieldExtensions.contains(def) && !(optionalFieldExtensions.contains(def))) {
							defs.append(def);
						}
					} else defs.append(def);
				}
				type.defs = defs.toList();
			}
			typeDepth--;
		}
		
		@Override public void visitField(JavacNode fieldNode, JCVariableDecl field) {
			if ((phase == 1) && (typeDepth == 1)) {
				if ((field.mods.flags & STATIC) != 0) return;
				String fieldName = field.name.toString();
				if (exclude.contains(fieldName)) return;
				if ((field.init == null) && ((field.mods.flags & FINAL) != 0)) {
					requiredFields.append(field);
					allRequiredFieldNames.add(fieldName);
					requiredFieldDefTypeNames.append(toCamelCase(false, "$", fieldName, "def"));
				}
				boolean append = isInitializedMapOrCollection(field) && convenientMethods;
				append |= (field.mods.flags & FINAL) == 0;
				if (append) optionalFields.append(field);
			}
		}
	}
	
	private static interface IBuilderData {
		public JavacNode getTypeNode();
		
		public List<JCVariableDecl> getRequiredFields();
		
		public List<JCVariableDecl> getOptionalFields();
		
		public List<JCVariableDecl> getAllFields();
		
		public List<String> getRequiredFieldDefTypeNames();
		
		public List<JCMethodDecl> getRequiredFieldExtensions();
		
		public List<JCMethodDecl> getOptionalFieldExtensions();
		
		public long getCreateModifier();
		
		public String getPrefix();
		
		public boolean generateConvenientMethods();
		
		public List<String> getCallMethods();
	}
}
