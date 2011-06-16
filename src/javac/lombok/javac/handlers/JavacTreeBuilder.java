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

import static lombok.core.util.Cast.uncheckedCast;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.io.InputStream;
import java.lang.reflect.Method;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.core.AST.Kind;
import lombok.core.util.Cast;
import lombok.javac.Javac;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.parser.JavacStringParser;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

// TODO add features if required
@NoArgsConstructor(access=AccessLevel.PRIVATE)
public final class JavacTreeBuilder {
	/**
	 * Adds the given class declaration to the provided type AST Node.
	 *
	 * Also takes care of updating the JavacAST.
	 */
	public static void injectType(JavacNode typeNode, JCClassDecl type) {
		JCClassDecl typeDecl = (JCClassDecl)typeNode.get();
		addSuppressWarningsAll(type.mods, typeNode, type.pos, typeDecl);
		typeDecl.defs = typeDecl.defs.append(type);
		typeNode.add(type, Kind.TYPE);
	}
	
	private static void addSuppressWarningsAll(JCModifiers mods, JavacNode node, int pos, JCTree source) {
		TreeMaker maker = node.getTreeMaker();
		JCExpression suppressWarningsType = chainDots(maker, node, "java", "lang", "SuppressWarnings").setPos(pos);
		JCExpression allLiteral = maker.Literal("all").setPos(pos);
		JCAnnotation annotation = (JCAnnotation) maker.Annotation(suppressWarningsType, List.<JCExpression>of(allLiteral)).setPos(pos);
		mods.annotations = mods.annotations.append(Javac.recursiveSetGeneratedBy(annotation, source));
	}

	public static void injectMethodSymbol(JavacNode node, JCMethodDecl method, MethodSymbol methodSymbol) {
		while (node != null && !(node.get() instanceof JCClassDecl)) {
			node = node.up();
		}
		if (node != null) {
			JCClassDecl tree = (JCClassDecl)node.get();
			ClassSymbol c = tree.sym;
			c.members_field.enter(methodSymbol, c.members_field, methodSymbol.enclClass().members_field);
			method.sym = methodSymbol;
		}
	}

	public static ConstructorBuilder constructor(JavacNode node, String methodString, Object... args) {
		return new ConstructorBuilder(node, JavacStringParser.methodFromString(node.getContext(), String.format(methodString, args)));
	}

	public static MethodBuilder method(JavacNode node, MethodSymbol methodSymbol) {
		return new MethodBuilder(node, methodSymbol);
	}

	public static MethodBuilder method(JavacNode node, String methodString, Object... args) {
		return new MethodBuilder(node, JavacStringParser.methodFromString(node.getContext(), String.format(methodString, args)));
	}

	public static ClassBuilder clazz(JavacNode node, long flags, String typeName) {
		return new ClassBuilder(node, flags, typeName);
	}

	public static ClassBuilder interfaze(JavacNode node, long flags, String typeName) {
		return new ClassBuilder(node, flags | Flags.INTERFACE, typeName);
	}

	public static List<JCStatement> statements(JavacNode node, String statementsString, Object... args) {
		return JavacStringParser.statementsFromString(node.getContext(), String.format(statementsString, args));
	}

	public static FieldBuilder field(JavacNode node, String fieldString, Object... args) {
		return new FieldBuilder(node, JavacStringParser.fieldFromString(node.getContext(), String.format(fieldString, args)));
	}

	public static class FieldBuilder extends AbstractTreeBuilder {
		protected final JCModifiers mods;
		protected final Name fieldName;
		protected JCExpression fieldType;
		protected JCExpression init;

		public FieldBuilder(JavacNode node, JCVariableDecl field) {
			super(node);
			this.mods = maker.Modifiers(field.mods.flags);
			this.fieldName = field.name;
			this.fieldType = field.vartype;
			this.init = field.init;
		}

		public JCVariableDecl build() {
			JCVariableDecl proto = maker.VarDef(mods, fieldName, fieldType, init);
			return new TreeCopier<JCVariableDecl>(maker).copy(proto);
		}

		public void inject(JCTree source) {
			injectField(node, Javac.recursiveSetGeneratedBy(build(), source));
		}

		@Override
		public String toString() {
			return build().toString();
		}
	}

	public static class ClassBuilder extends AbstractTreeBuilder  {
		protected final JCModifiers mods;
		protected final Name typeName;
		protected JCTree extending;
		protected ListBuffer<JCTree> defs = ListBuffer.lb();
		protected ListBuffer<JCTypeParameter> typarams = ListBuffer.lb();
		protected ListBuffer<JCExpression> implementing = ListBuffer.lb();

		protected ClassBuilder(JavacNode node, long flags, String typeName) {
			this(node, flags, node.toName(typeName));
		}

		protected ClassBuilder(JavacNode node, long flags, Name typeName) {
			super(node);
			this.mods = maker.Modifiers(flags);
			this.typeName = typeName;
		}

		public ClassBuilder implementing(List<String> interfazes) {
			for (String typeName : interfazes) {
				implementing.append(expressionFromString(typeName));
			}
			return this;
		}

		public ClassBuilder withMethods(List<JCTree> methods) {
			defs.appendList(methods);
			return this;
		}

		public ClassBuilder withMethod(JCMethodDecl method) {
			defs.append(method);
			return this;
		}
		
		public ClassBuilder withField(JCVariableDecl field) {
			defs.append(field);
			return this;
		}

		public ClassBuilder withFields(List<JCTree> fields) {
			defs.appendList(fields);
			return this;
		}

		public JCClassDecl build() {
			JCClassDecl proto = maker.ClassDef(mods, typeName, typarams.toList(), extending, implementing.toList(), defs.toList());
			return new TreeCopier<JCClassDecl>(maker).copy(proto);
		}

		public void inject(JCTree source) {
			injectType(node, Javac.recursiveSetGeneratedBy(build(), source));
		}

		@Override
		public String toString() {
			return build().toString();
		}
	}

	public static class MethodBuilder extends AbstractMethodBuilder<MethodBuilder> {
		protected MethodBuilder(JavacNode node, MethodSymbol m) {
			super(node, m.flags(), m.name);
			methodSymbol = m;
			Type mtype = m.type;
			returnType = fixLeadingDot(maker, maker.Type(mtype.getReturnType()));
			typarams.appendList(maker.TypeParams(mtype.getTypeArguments()));
			for (JCTypeParameter typaram : typarams) {
				ListBuffer<JCExpression> bounds = ListBuffer.lb();
				for (JCExpression expr : typaram.bounds) {
					bounds.append(fixLeadingDot(maker, expr));
				}
				typaram.bounds = bounds.toList();
			}
			params.appendList(maker.Params(mtype.getParameterTypes(), m));
			for (JCVariableDecl param : params) {
				param.vartype = fixLeadingDot(maker, param.vartype);
			}
			for (JCExpression expr : maker.Types(mtype.getThrownTypes())) {
				thrownExceptions.append(fixLeadingDot(maker, expr));
			}
		}

		public MethodBuilder(JavacNode node, JCMethodDecl method) {
			super(node, method);
		}

		public MethodBuilder withReturnType(String returnTypeName) {
			returnType = expressionFromString(returnTypeName);
			return this;
		}


		public MethodBuilder withoutBody() {
			statements.clear();
			forceBlock = false;
			return this;
		}

		public MethodBuilder withDefaultReturnStatement() {
			if (returnType instanceof JCPrimitiveTypeTree) {
				JCPrimitiveTypeTree primitiveType = (JCPrimitiveTypeTree) returnType;
				if (primitiveType.typetag != TypeTags.VOID) {
					return withReturnStatement(maker.Literal(primitiveType.typetag, 0)); // return const0;
				} else {
					forceBlock = true;
				}
			} else {
				return withReturnStatement(maker.Literal(TypeTags.BOT, 0)); // return null;
			}
			return this;
		}


		public MethodBuilder withReturnStatement(String returnValue) {
			return withReturnStatement(expressionFromString(returnValue));
		}

		public MethodBuilder withReturnStatement(JCExpression returnValue) {
			return withStatement(maker.Return(returnValue));
		}
	}

	public static class ConstructorBuilder extends AbstractMethodBuilder<ConstructorBuilder> {
		public ConstructorBuilder(JavacNode node, JCMethodDecl method) {
			super(node, method);
		}
	}

	private static abstract class AbstractMethodBuilder<SELF_TYPE extends AbstractMethodBuilder<SELF_TYPE>> extends AbstractTreeBuilder {
		protected final JCModifiers mods;
		protected ListBuffer<JCAnnotation> annotations = ListBuffer.lb();
		protected ListBuffer<JCTypeParameter> typarams = ListBuffer.lb();
		protected JCExpression returnType;
		protected final Name methodName;
		protected ListBuffer<JCVariableDecl> params = ListBuffer.lb();
		protected ListBuffer<JCExpression> thrownExceptions = ListBuffer.lb();
		protected ListBuffer<JCStatement> statements = ListBuffer.lb();
		protected boolean forceBlock = false;
		protected MethodSymbol methodSymbol;

		protected AbstractMethodBuilder(JavacNode node, long flags, Name methodName) {
			super(node);
			this.mods = maker.Modifiers(flags);
			this.methodName = methodName;
		}

		public AbstractMethodBuilder(JavacNode node, JCMethodDecl method) {
			this(node, method.mods.flags, method.name);
			returnType = method.restype;
			typarams.appendList(method.typarams);
			params.appendList(method.params);
			annotations.appendList(method.mods.annotations);
			thrownExceptions.appendList(method.thrown);
			if (method.body != null) {
				statements.appendList(method.body.stats);
			}
		}

		protected final SELF_TYPE self() {
			return Cast.<SELF_TYPE>uncheckedCast(this);
		}

		public SELF_TYPE withMods(long flags) {
			mods.flags = flags;
			annotations.clear();
			return self();
		}

		public SELF_TYPE withThrownExceptions(List<JCExpression> thrownExceptions) {
			this.thrownExceptions.appendList(thrownExceptions);
			return self();
		}

		public SELF_TYPE withStatement(JCStatement statement) {
			this.statements.append(statement);
			return self();
		}

		public SELF_TYPE withStatements(List<JCStatement> statements) {
			this.statements.appendList(statements);
			return self();
		}

		public JCMethodDecl build() {
			JCBlock body = null;
			if (!statements.isEmpty() || forceBlock) {
				body = maker.Block(0, statements.toList());
			}
			mods.annotations = annotations.toList();
			JCMethodDecl proto = maker.MethodDef(mods, methodName, returnType, typarams.toList(), params.toList(), thrownExceptions.toList(), body, null);
			return new TreeCopier<JCMethodDecl>(maker).copy(proto);
		}

		public void inject(JCTree source) {
			JCMethodDecl method = Javac.recursiveSetGeneratedBy(build(), source);
			injectMethod(node, method);
			if (methodSymbol != null) {
				injectMethodSymbol(node, method, methodSymbol);
			}
		}

		@Override
		public String toString() {
			return build().toString();
		}
	}

	private static abstract class AbstractTreeBuilder {
		protected final JavacNode node;
		protected final TreeMaker maker;

		protected AbstractTreeBuilder(JavacNode node) {
			this.node = node;
			this.maker = node.getTreeMaker();
		}

		protected final JCExpression expressionFromString(final String s) {
			if ("void".equals(s)) {
				return maker.TypeIdent(TypeTags.VOID);
			} else if ("int".equals(s)) {
				return maker.TypeIdent(TypeTags.INT);
			} else if ("long".equals(s)) {
				return maker.TypeIdent(TypeTags.LONG);
			} else if ("short".equals(s)) {
				return maker.TypeIdent(TypeTags.SHORT);
			} else if ("boolean".equals(s)) {
				return maker.TypeIdent(TypeTags.BOOLEAN);
			} else if ("byte".equals(s)) {
				return maker.TypeIdent(TypeTags.BYTE);
			} else if ("char".equals(s)) {
				return maker.TypeIdent(TypeTags.CHAR);
			} else if ("float".equals(s)) {
				return maker.TypeIdent(TypeTags.FLOAT);
			} else if ("double".equals(s)) {
				return maker.TypeIdent(TypeTags.DOUBLE);
			} else {
				return JavacHandlerUtil.chainDotsString(maker, node, s);
			}
		}

		protected JCExpression fixLeadingDot(TreeMaker maker, JCExpression expr) {
			if (expr instanceof JCFieldAccess) {
				JCFieldAccess fieldAccess = (JCFieldAccess) expr;
				JCExpression selExpr = fieldAccess.selected;
				if (selExpr instanceof JCIdent) {
					if ("".equals(selExpr.toString())) {
						return maker.Ident(fieldAccess.name);
					}
				} else if (selExpr instanceof JCFieldAccess) {
					fieldAccess.selected = fixLeadingDot(maker, selExpr);
				}
			}
			return expr;
		}
	}

	static {
		try {
			reloadClass("com.sun.tools.javac.parser.JavacStringParser", Parser.class.getClassLoader());
		} catch (Exception ignore) {
			// if this fails, all of my javac transformations will fail, so be it..
		}
	}

	static <T> Class<T> reloadClass(String claz, ClassLoader outClassLoader) throws Exception {
		try {
			return uncheckedCast(outClassLoader.loadClass(claz));
		} catch (ClassNotFoundException e) {}
		String path = claz.replace('.', '/') + ".class";
		ClassLoader incl = JavacAnnotationHandler.class.getClassLoader();
		InputStream is = incl.getResourceAsStream(path);
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		Method m = ClassLoader.class.getDeclaredMethod("defineClass", new Class[] {
				String.class, byte[].class, int.class, int.class });
		m.setAccessible(true);
		return uncheckedCast(m.invoke(outClassLoader, claz, bytes, 0, bytes.length));
	}
}