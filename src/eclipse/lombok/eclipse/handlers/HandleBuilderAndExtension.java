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

import static lombok.ast.AST.Type;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.eclipse.handlers.Eclipse.*;
import static org.eclipse.jdt.core.dom.Modifier.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;

import java.util.*;

import lombok.*;
import lombok.ast.TypeRef;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.handlers.BuilderAndExtensionHandler;
import lombok.core.handlers.BuilderAndExtensionHandler.IBuilderData;
import lombok.core.handlers.BuilderAndExtensionHandler.IExtensionCollector;
import lombok.eclipse.DeferUntilPostDiet;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseASTVisitor;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.EclipseHandlerUtil.MemberExistsResult;
import lombok.eclipse.handlers.ast.EclipseMethod;
import lombok.eclipse.handlers.ast.EclipseType;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.mangosdk.spi.ProviderFor;

public class HandleBuilderAndExtension {

	/**
	 * Handles the {@code lombok.Builder} annotation for eclipse.
	 */
	@ProviderFor(EclipseAnnotationHandler.class)
	public static class HandleBuilder extends EclipseAnnotationHandler<Builder> {

		@Override public void handle(AnnotationValues<Builder> annotation, Annotation source, EclipseNode annotationNode) {
			final EclipseType type = EclipseType.typeOf(annotationNode, source);

			if (type.isInterface() || type.isEnum() || type.isAnnotation()) {
				annotationNode.addError(canBeUsedOnClassOnly(Builder.class));
				return;
			}

			switch (methodExists(decapitalize(type.name()), type.node(), false)) {
			case EXISTS_BY_LOMBOK:
				return;
			case EXISTS_BY_USER:
				annotationNode.addWarning(String.format("Not generating 'public static %s %s()' A method with that name already exists", BuilderAndExtensionHandler.BUILDER, decapitalize(type.name())));
				return;
			default:
			case NOT_EXISTS:
				//continue with creating the builder
			}

			new EclispeBuilderAndExtensionHandler().handleBuilder(new BuilderDataCollector(type, annotation.getInstance()).collect());
		}
	}

	/**
	 * Handles the {@code lombok.Builder.Extension} annotation for eclipse.
	 */
	@ProviderFor(EclipseAnnotationHandler.class)
	@DeferUntilPostDiet
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

			new EclispeBuilderAndExtensionHandler().handleExtension(new BuilderDataCollector(EclipseType.typeOf(typeNode, source), builderAnnotation.getInstance()).collect(), method);
		}
	}
	
	private static class EclispeBuilderAndExtensionHandler extends BuilderAndExtensionHandler<EclipseType, EclipseMethod, FieldDeclaration> {

		@Override
		protected void collectExtensions(EclipseMethod method, IExtensionCollector collector) {
			method.node().traverse((EclipseASTVisitor) collector);
		}

		@Override
		protected Object[] getTypeArguments(Object type) {
			if (type instanceof ParameterizedQualifiedTypeReference) {
				ParameterizedQualifiedTypeReference typeRef = (ParameterizedQualifiedTypeReference)type;
				if (typeRef.typeArguments != null) {
					return typeRef.typeArguments[typeRef.typeArguments.length - 1];
				}
			}
			if (type instanceof ParameterizedSingleTypeReference) {
				ParameterizedSingleTypeReference typeRef = (ParameterizedSingleTypeReference)type;
				return typeRef.typeArguments;
			}
			return null;
		}

		@Override
		protected String name(Object object) {
			if (object instanceof AbstractMethodDeclaration) {
				return string(((AbstractMethodDeclaration)object).selector);
			} else if (object instanceof AbstractVariableDeclaration) {
				return string(((AbstractVariableDeclaration)object).name);
			}
			return null;
		}

		@Override
		protected Object type(FieldDeclaration field) {
			return field.type;
		}

		@Override
		protected String typeStringOf(FieldDeclaration field) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (char[] elem : field.type.getTypeName()) {
				if (first) first = false;
				else sb.append('.');
				sb.append(elem);
			}
			return sb.toString();
		}

		@Override
		protected Object getFieldInitialization(FieldDeclaration field) {
			return field.initialization;
		}

		@Override
		protected void setFieldInitialization(FieldDeclaration field, Object init) {
			field.initialization = (Expression) init;
		}
	}

	@Getter
	private static class BuilderDataCollector extends EclipseASTAdapterWithTypeDepth implements IBuilderData<EclipseType, EclipseMethod, FieldDeclaration> {
		private final List<FieldDeclaration> requiredFields = new ArrayList<FieldDeclaration>();
		private final List<FieldDeclaration> optionalFields = new ArrayList<FieldDeclaration>();
		private final List<TypeRef> requiredFieldDefTypes = new ArrayList<TypeRef>();
		private final List<String> allRequiredFieldNames = new ArrayList<String>();
		private final List<String> requiredFieldDefTypeNames = new ArrayList<String>();
		private final EclipseType type;
		private final String prefix;
		private final List<String> callMethods;
		private final boolean generateConvenientMethodsEnabled;
		private final AccessLevel level;
		private final Set<String> excludes;

		@Override public IExtensionCollector getExtensionCollector() {
			return new ExtensionCollector();
		}

		public BuilderDataCollector(EclipseType type, Builder builder) {
			super(1);
			this.type = type;
			excludes = new HashSet<String>(Arrays.asList(builder.exclude()));
			generateConvenientMethodsEnabled = builder.convenientMethods();
			prefix = builder.prefix();
			callMethods = Arrays.asList(builder.callMethods());
			level = builder.value();
		}

		public IBuilderData<EclipseType, EclipseMethod, FieldDeclaration> collect() {
			type.node().traverse(this);
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
				if (excludes.contains(fieldName)) return;
				if ((field.initialization == null) && ((field.modifiers & FINAL) != 0)) {
					requiredFields.add(field);
					allRequiredFieldNames.add(fieldName);
					String typeName = camelCase("$", fieldName, "def");
					requiredFieldDefTypeNames.add(typeName);
					requiredFieldDefTypes.add(Type(typeName));
				}
				boolean append = new EclispeBuilderAndExtensionHandler().isInitializedMapOrCollection(field) && generateConvenientMethodsEnabled;
				append |= (field.modifiers & FINAL) == 0;
				if (append) optionalFields.add(field);
			}
		}
	}

	private static class ExtensionCollector extends EclipseASTAdapterWithTypeDepth implements IExtensionCollector {
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

		@Override public ExtensionCollector withRequiredFieldNames(final List<String> fieldNames) {
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
}
