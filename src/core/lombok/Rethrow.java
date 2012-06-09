/*
 * Copyright © 2011-2012 Philipp Eichhorn
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
package lombok;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.*;

/**
 * Simplifies rethrowing {@link Exception Exceptions} by wrapping them.
 * <p>
 * With lombok:
 * 
 * <pre>
 * &#64;Rethrow(IOException.class)
 * void testMethod() {
 *   // do something
 * }
 * 
 * void testMethod() throw IOException as RuntimeException {
 *   // do something
 * }
 * </pre>
 * 
 * Vanilla Java:
 * 
 * <pre>
 * void testMethod() {
 * 	try {
 * 		// do something
 * 	} catch (IOException e1) {
 * 		throw new RuntimeException(e1);
 * 	}
 * }
 * </pre>
 */
@Target(METHOD)
@Retention(SOURCE)
public @interface Rethrow {
	/**
	 * Specifies the exception types, that should be caught and rethrown. Default is {@code Exception.class}.
	 */
	Class<? extends Exception>[] value() default {};

	/**
	 * Specifies the exception type, that wraps the caught exceptions. Default is {@code RuntimeException.class}.
	 */
	Class<? extends Exception> as() default RuntimeException.class;

	/**
	 * Specifies the message used for the new exception, default is no message.
	 */
	String message() default "";
}
