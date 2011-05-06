/*
 * Copyright Â© 2011 Philipp Eichhorn
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
package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.ast.Arrays.buildArray;
import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.handlers.Eclipse.injectType;
import static lombok.eclipse.handlers.Eclipse.typeNodeOf;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

@RequiredArgsConstructor
public class ClassDefBuilder implements ASTNodeBuilder<TypeDeclaration> {
	private final List<ExpressionBuilder<? extends Annotation>> annotations = new ArrayList<ExpressionBuilder<? extends Annotation>>();
	private final List<StatementBuilder<? extends TypeParameter>> typeParameters = new ArrayList<StatementBuilder<? extends TypeParameter>>();
	private final List<StatementBuilder<? extends FieldDeclaration>> fields = new ArrayList<StatementBuilder<? extends FieldDeclaration>>();
	private final List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> methods = new ArrayList<ASTNodeBuilder<? extends AbstractMethodDeclaration>>();
	private final List<ASTNodeBuilder<? extends TypeDeclaration>> memberTypes = new ArrayList<ASTNodeBuilder<? extends TypeDeclaration>>();
	private final List<ExpressionBuilder<? extends TypeReference>> superInterfaces = new ArrayList<ExpressionBuilder<? extends TypeReference>>();
	private final String name;
	private ExpressionBuilder<? extends TypeReference> superclass;
	private int modifiers;
	private int bits;

	public ClassDefBuilder implementing(final ExpressionBuilder<? extends TypeReference> type) {
		this.superInterfaces.add(type);
		return this;
	}

	public ClassDefBuilder implementing(final List<ExpressionBuilder<? extends TypeReference>> types) {
		this.superInterfaces.addAll(types);
		return this;
	}

	public ClassDefBuilder makeLocal() {
		return withBits(ASTNode.IsLocalType);
	}

	public ClassDefBuilder makeAnonymous() {
		return withBits(ASTNode.IsAnonymousType);
	}

	public ClassDefBuilder withBits(final int bits) {
		this.bits |= bits;
		return this;
	}

	public ClassDefBuilder withModifiers(final int modifiers) {
		this.modifiers = modifiers;
		return this;
	}

	public ClassDefBuilder withMethod(final ASTNodeBuilder<? extends AbstractMethodDeclaration> method) {
		this.methods.add(method);
		return this;
	}

	public ClassDefBuilder withMethods(final List<ASTNodeBuilder<? extends AbstractMethodDeclaration>> methods) {
		this.methods.addAll(methods);
		return this;
	}

	public ClassDefBuilder withField(final StatementBuilder<? extends FieldDeclaration> field) {
		this.fields.add(field);
		return this;
	}

	public ClassDefBuilder withFields(final List<StatementBuilder<? extends FieldDeclaration>> fields) {
		this.fields.addAll(fields);
		return this;
	}

	public ClassDefBuilder withFields(final FieldDeclaration... fields) {
		for (FieldDeclaration field : fields) {
			this.fields.add(new StatementWrapper<FieldDeclaration>(field));
		}
		return this;
	}

	public ClassDefBuilder withType(final ASTNodeBuilder<? extends TypeDeclaration> type) {
		this.memberTypes.add(type);
		return this;
	}

	public ClassDefBuilder withTypes(final List<ASTNodeBuilder<? extends TypeDeclaration>> types) {
		this.memberTypes.addAll(types);
		return this;
	}

	@Override
	public TypeDeclaration build(final EclipseNode node, final ASTNode source) {
		final TypeDeclaration proto = new TypeDeclaration(((CompilationUnitDeclaration) node.top().get()).compilationResult);
		setGeneratedByAndCopyPos(proto, source);
		proto.modifiers = modifiers;
		proto.bits |= bits | ECLIPSE_DO_NOT_TOUCH_FLAG;
		if ((name == null) || name.isEmpty()) {
			proto.name = CharOperation.NO_CHAR;
		} else {
			proto.name = name.toCharArray();
		}
		if ((bits & (ASTNode.IsAnonymousType)) != 0) {
			proto.sourceEnd = 0;
			proto.bodyEnd = source.sourceEnd + 2;
		}
		proto.annotations = buildArray(annotations, new Annotation[0], node, source);
		proto.typeParameters = buildArray(typeParameters, new TypeParameter[0], node, source);
		proto.fields = buildArray(fields, new FieldDeclaration[0], node, source);
		proto.methods = buildArray(methods, new AbstractMethodDeclaration[0], node, source);
		proto.memberTypes = buildArray(memberTypes, new TypeDeclaration[0], node, source);
		proto.superInterfaces = buildArray(superInterfaces, new TypeReference[0], node, source);
		if (superclass != null) {
			proto.superclass = superclass.build(node, source);
		}
		return proto;
	}

	public final void injectInto(final EclipseNode node, final ASTNode source) {
		injectType(typeNodeOf(node), build(node, source));
	}
}
