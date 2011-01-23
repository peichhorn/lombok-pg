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

import java.util.ArrayList;
import java.util.List;

public class Names {
	/**
	 * <pre>
	 * null        -> null
	 *             ->
	 * IOInterface -> IOInterface
	 * Irony       -> Irony
	 * IObject     -> Object
	 * 
	 * {@code [I]([A-Z][a-z].*)}
	 * <pre> 
	 */
	public static String interfaceName(final String s) {
		return (isNotEmpty(s) && (s.length() > 2) && (s.charAt(0) == 'I') && isUpperCase(s.charAt(1)) && isLowerCase(s.charAt(2))) ? s.substring(1) : s;
	}
	
	public static String removeCurlyBrackets(String s) {
		String trimmed = trim(s);
		if (isEmpty(trimmed)) return s;
		return (trimmed.startsWith("{") && trimmed.endsWith("}")) ? trimmed.substring(1, trimmed.length() - 1) : s;
	}
	
	public static boolean isEmpty(String s) {
		return trim(s).isEmpty();
	}
	
	public static boolean isNotEmpty(String s) {
		return !isEmpty(s);
	}
	
	/**
	 * <pre>
	 * null  -> 
	 *       ->
	 *   s   -> s
	 * </pre>
	 */
	public static String trim(String s) {
		if (s == null) return "";
		else return s.trim();
	}
	
	public static String singular(String s) {
		return s.endsWith("s") ? s.substring(0, s.length() - 1): s;
	}
	
	public static String camelCase(String first, String... rest) {
		List<String> nonEmptyStrings = new ArrayList<String>();
		if (isNotEmpty(first)) nonEmptyStrings.add(first);
		if (Arrays.isNotEmpty(rest)) for (String s : rest) {
			if (isNotEmpty(s)) nonEmptyStrings.add(s);
		}
		return camelCase0(nonEmptyStrings.toArray(new String[nonEmptyStrings.size()]));
	}
	
	private static String camelCase0(String[] s) {
		if (Arrays.isEmpty(s)) return "";
		StringBuilder builder = new StringBuilder();
		builder.append(s[0]);
		for (int i = 1, iend = s.length; i < iend; i++) {
			builder.append(s[i].substring(0, 1).toUpperCase()).append(s[i].substring(1));
		}
		return builder.toString();
	}
}
