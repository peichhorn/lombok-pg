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

import static lombok.ast.AST.Type;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static com.sun.tools.javac.code.Flags.*;

import java.util.*;

import lombok.*;
import lombok.ast.TypeRef;
import lombok.core.AnnotationValues;
import lombok.core.handlers.BuilderAndExtensionHandler;
import lombok.core.handlers.BuilderAndExtensionHandler.IBuilderData;
import lombok.core.handlers.BuilderAndExtensionHandler.IExtensionCollector;
import lombok.javac.Javac;
import lombok.javac.JavacASTVisitor;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacField;
import lombok.javac.handlers.ast.JavacMethod;
import lombok.javac.handlers.ast.JavacType;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import org.mangosdk.spi.ProviderFor;

public class HandleBuilderAndExtension {

	/**
	 * Handles the {@code lombok.Builder} annotation for javac.
	 */
	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleBuilder extends JavacAnnotationHandler<Builder> {

		@Override public void handle(final AnnotationValues<Builder> annotation, final JCAnnotation source, final JavacNode annotationNode) {
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
				final String message = "Not generating 'public static %s %s()' A method with that name already exists";
				annotationNode.addWarning(String.format(message, BuilderAndExtensionHandler.BUILDER, decapitalize(type.name())));
				return;
			default:
			case NOT_EXISTS:
				//continue with creating the builder
			}

			new JavacBuilderAndExtensionHandler().handleBuilder(new BuilderDataCollector(type, annotation.getInstance()).collect());
		}
	}

	/**
	 * Handles the {@code lombok.Builder.Extension} annotation for javac.
	 */
	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleBuilderExtension extends JavacAnnotationHandler<Builder.Extension> {

		@Override public void handle(final AnnotationValues<Builder.Extension> annotation, final JCAnnotation source, final JavacNode annotationNode) {
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

			JavacType type = JavacType.typeOf(annotationNode, source);
			JavacNode builderNode = type.getAnnotation(Builder.class);

			if (builderNode == null) {
				annotationNode.addError("@Builder.Extension is only allowed in types annotated with @Builder");
				return;
			}
			AnnotationValues<Builder> builderAnnotation = Javac.createAnnotation(Builder.class, builderNode);

			if (!type.hasMethod(decapitalize(type.name()))) {
				new HandleBuilder().handle(builderAnnotation, (JCAnnotation)builderNode.get(), builderNode);
			}

			final BuilderDataCollector collector = new BuilderDataCollector(type, builderAnnotation.getInstance());
			new JavacBuilderAndExtensionHandler().handleExtension(collector.collect(), method, new JavacParameterValidator(), new JavacParameterSanitizer());
		}
	}

	private static class JavacBuilderAndExtensionHandler extends BuilderAndExtensionHandler<JavacType, JavacMethod, JavacField> {

		@Override protected void collectExtensions(final JavacMethod method, final IExtensionCollector collector) {
			method.node().traverse((JavacASTVisitor) collector);
		}
	}

	@Getter
	private static class BuilderDataCollector implements IBuilderData<JavacType, JavacMethod, JavacField> {
		private final List<JavacField> requiredFields = new ArrayList<JavacField>();
		private final List<JavacField> optionalFields = new ArrayList<JavacField>();
		private final List<TypeRef> requiredFieldDefTypes = new ArrayList<TypeRef>();
		private final List<String> allRequiredFieldNames = new ArrayList<String>();
		private final List<String> requiredFieldDefTypeNames = new ArrayList<String>();;
		private final JavacType type;
		private final String prefix;
		private final List<String> callMethods;
		private final boolean generateConvenientMethodsEnabled;
		private final AccessLevel level;
		private final Set<String> excludes;

		public BuilderDataCollector(final JavacType type, final Builder builder) {
			this.type = type;
			excludes = new HashSet<String>(Arrays.asList(builder.exclude()));
			generateConvenientMethodsEnabled = builder.convenientMethods();
			prefix = builder.prefix();
			callMethods = Arrays.asList(builder.callMethods());
			level = builder.value();
		}

		@Override public IExtensionCollector getExtensionCollector() {
			return new ExtensionCollector();
		}

		public IBuilderData<JavacType, JavacMethod, JavacField> collect() {
			for (JavacField field : type.fields()) {
				if (field.isStatic()) continue;
				String fieldName = field.name();
				if (excludes.contains(fieldName)) continue;
				if ((!field.isInitialized()) && field.isFinal()) {
					requiredFields.add(field);
					allRequiredFieldNames.add(fieldName);
					String typeName = camelCase("$", fieldName, "def");
					requiredFieldDefTypeNames.add(typeName);
					requiredFieldDefTypes.add(Type(typeName));
				}
				boolean append = new JavacBuilderAndExtensionHandler().isInitializedMapOrCollection(field) && generateConvenientMethodsEnabled;
				append |= !field.isFinal();
				if (append) optionalFields.add(field);
			}
			return this;
		}

		@Override public List<JavacField> getAllFields() {
			List<JavacField> allFields = new ArrayList<JavacField>(getRequiredFields());
			allFields.addAll(getOptionalFields());
			return allFields;
		}
	}

	private static class ExtensionCollector extends JavacASTAdapterWithTypeDepth implements IExtensionCollector {
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

		@Override public void visitMethod(final JavacNode methodNode, final JCMethodDecl method) {
			if (isOfInterest() && !"<init>".equals(method.name.toString())) {
				containsRequiredFields = false;
				isRequiredFieldsExtension = false;
				isExtension = false;
				requiredFieldNames.clear();
				requiredFieldNames.addAll(allRequiredFieldNames);
			}
		}

		@Override public void visitStatement(final JavacNode statementNode, final JCTree statement) {
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

		@Override public void endVisitMethod(final JavacNode methodNode, final JCMethodDecl method) {
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
}
