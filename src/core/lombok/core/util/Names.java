/*
 * Copyright © 2010-2012 Philipp Eichhorn
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

import static java.lang.Character.*;

import java.util.*;

import lombok.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
	 * 
	 * <pre>
	 */
	public static String interfaceName(final String s) {
		if (Is.empty(s) || (s.length() <= 2)) return s;
		return ((s.charAt(0) == 'I') && isUpperCase(s.charAt(1)) && isLowerCase(s.charAt(2))) ? s.substring(1) : s;
	}

	public static String decapitalize(final String s) {
		if (s == null) return "";
		return s.substring(0, 1).toLowerCase() + s.substring(1);
	}

	public static String capitalize(final String s) {
		if (s == null) return "";
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	/**
	 * <pre>
	 * null  ->
	 *       ->
	 *   s   -> s
	 * </pre>
	 */
	public static String trim(final String s) {
		if (s == null) return "";
		else return s.trim();
	}

	public static String singular(final String s) {
		return s.endsWith("s") ? s.substring(0, s.length() - 1) : s;
	}

	public static String camelCaseToConstant(final String fieldName) {
		if (Is.empty(fieldName)) return "";
		char[] chars = fieldName.toCharArray();
		StringBuilder b = new StringBuilder();
		b.append(toUpperCase(chars[0]));
		for (int i = 1, iend = chars.length; i < iend; i++) {
			char c = chars[i];
			if (isUpperCase(c)) {
				b.append('_');
			} else {
				c = toUpperCase(c);
			}
			b.append(c);
		}
		return b.toString();
	}

	public static String camelCase(final String first, final String... rest) {
		List<String> nonEmptyStrings = new ArrayList<String>();
		if (Is.notEmpty(first)) nonEmptyStrings.add(first);
		for (String s : Each.elementIn(rest)) {
			if (Is.notEmpty(s)) nonEmptyStrings.add(s);
		}
		return camelCase0(nonEmptyStrings.toArray(new String[nonEmptyStrings.size()]));
	}

	private static String camelCase0(final String[] s) {
		if (Is.empty(s)) return "";
		StringBuilder builder = new StringBuilder();
		builder.append(s[0]);
		for (int i = 1, iend = s.length; i < iend; i++) {
			builder.append(capitalize(s[i]));
		}
		return builder.toString();
	}

	/**
	 * Given the name of a field, return the 'base name' of that field. For example, {@code fFoobar} becomes
	 * {@code foobar} if {@code f} is in the prefix list. For prefixes that end in a letter character, the next
	 * character must be a non-lowercase character (i.e. {@code hashCode} is not {@code ashCode} even if {@code h} is in
	 * the prefix list, but {@code hAshcode} would become {@code ashCode}). The first prefix that matches is used. If
	 * the prefix list is empty, or the empty string is in the prefix list and no prefix before it matches, the
	 * fieldName will be returned verbatim.
	 * 
	 * If no prefix matches and the empty string is not in the prefix list and the prefix list is not empty,
	 * {@code null} is returned.
	 * 
	 * @param fieldName
	 *            The full name of a field.
	 * @param prefixes
	 *            A list of prefixes, usually provided by the {@code Accessors} settings annotation, listing field
	 *            prefixes.
	 * @return The base name of the field.
	 */
	public static String removePrefix(String fieldName, String[] prefixes) {
		if (Is.empty(prefixes)) return fieldName;
		for (final String prefix : prefixes) {
			if (prefix.isEmpty()) return fieldName;
			if (!fieldName.startsWith(prefix)) continue;
			char followupChar = fieldName.charAt(prefix.length());
			if (isLetter(prefix.charAt(prefix.length() - 1)) && isLowerCase(followupChar)) continue;
			return toLowerCase(followupChar) + fieldName.substring(prefix.length() + 1, fieldName.length());
		}
		return null;
	}
}
