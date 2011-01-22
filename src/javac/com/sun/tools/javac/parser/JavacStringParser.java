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
package com.sun.tools.javac.parser;

import static com.sun.tools.javac.parser.Token.*;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Position;

public class JavacStringParser extends EndPosParser {
	
	public static JCVariableDecl fieldFromString(Context context, String field) {
		char[] input = field.toCharArray(); 
		Lexer lexer = Scanner.Factory.instance(context).newScanner(input, input.length);
		JavacStringParser parser = new JavacStringParser(Parser.Factory.instance(context), lexer);
		return parser.parseField();
	}
	
	public static JCMethodDecl methodFromString(Context context, String method) {
		char[] input = method.toCharArray(); 
		Lexer lexer = Scanner.Factory.instance(context).newScanner(input, input.length);
		JavacStringParser parser = new JavacStringParser(Parser.Factory.instance(context), lexer);
		return parser.parseMethod();
	}
	
	public static List<JCStatement> statementsFromString(Context context, String statements) {
		char[] input = statements.toCharArray(); 
		Lexer lexer = Scanner.Factory.instance(context).newScanner(input, input.length);
		JavacStringParser parser = new JavacStringParser(Parser.Factory.instance(context), lexer);
		return parser.parseStatements();
	}
	
	private Lexer S;
	
	protected JavacStringParser(Factory fac, Lexer S) {
		super(fac, S, false);
		this.S = S;
	}
	
	JCVariableDecl parseField() {
		if (S.token() != SEMI) {
			String dc = S.docComment();
			int pos = S.pos();
			JCModifiers mods = modifiersOpt();
			pos = S.pos();
			Name name = S.name();
			pos = S.pos();
			JCExpression type = type();
			pos = S.pos();
			name = ident();
			return variableDeclaratorRest(pos, mods, type, name, false, dc);
		}
		throw new IllegalArgumentException("No can't do");
	}
	
	JCMethodDecl parseMethod() {
		if (S.token() != SEMI) {
			String dc = S.docComment();
			int pos = S.pos();
			JCModifiers mods = modifiersOpt();
			if (!(S.token() == CLASS || S.token() == INTERFACE || allowEnums && S.token() == ENUM) 
				|| !(S.token() == LBRACE && (mods.flags & Flags.StandardFlags & ~Flags.STATIC) == 0 && mods.annotations.isEmpty())) {
				pos = S.pos();
				List<JCTypeParameter> typarams = typeParametersOpt();
				if (typarams.length() > 0 && mods.pos == Position.NOPOS) {
					mods.pos = pos;
				}
				Name name = S.name();
				pos = S.pos();
				JCExpression type;
				boolean isVoid = S.token() == VOID;
				if (isVoid) {
					type = to(F.at(pos).TypeIdent(TypeTags.VOID));
					S.nextToken();
				} else {
					type = type();
				}
				
				if (S.token() == LPAREN  && getTag(type) == JCTree.IDENT) {
					return (JCMethodDecl)methodDeclaratorRest(pos, mods, null, name.table.init, typarams, false, true, dc);
				} else {
					pos = S.pos();
					name = ident();
					if (S.token() == LPAREN) {
						return (JCMethodDecl)methodDeclaratorRest(pos, mods, type, name, typarams, false, isVoid, dc);
					}
				}
			}
		}
		throw new IllegalArgumentException("No can't do");
	}
	List<JCStatement> parseStatements() {
		return blockStatements();
	}
	
	static int getTag(JCTree t) {
		try {
			return JCTree.class.getField("tag").getInt(t); // SunJDK
		} catch (Exception e) {
			return t.getTag(); // OpenJDK
		}
	}
}
