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
package lombok.core.util;

import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;

public class Names {
	/**
	 * <pre>
	 * null        -> null
	 * IOInterface -> IOInterface
	 * Irony       -> Irony
	 * IObject     -> Object
	 * 
	 * {@code [I]([A-Z][a-z].*)}
	 * <pre> 
	 */
	public static String interfaceName(final String s) {
		return (!s.isEmpty() && (s.length() > 2) && (s.charAt(0) == 'I') && isUpperCase(s.charAt(1)) && isLowerCase(s.charAt(2))) ? s.substring(1) : s;
	}
	
	/**
	 * <pre>
	 * null -> null
	 * tree -> Tree
	 * o    -> O
	 * Ion  -> Ion
	 * A    -> A
	 * <pre>
	 */
	public static String capitalize(final String s) {
		return isEmpty(s) ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
	}
	
	public static String removeCurlyBrackets(String s) {
		int startIndex = s.indexOf("{");
		if (startIndex < 0) return s;
		int endIndex = s.lastIndexOf("}");
		if (endIndex <= 0) return s;
		s = s.substring(startIndex + 1, endIndex - 1);
		return s;
	}
	
	public static boolean isEmpty(String s) {
		return (s == null) || s.isEmpty();
	}
	
	public static String trim(String s) {
		if (isEmpty(s)) return "";
		else return s.trim();
	}
	
	public static String toCamelCase(boolean singular, String... strings) {
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
}
