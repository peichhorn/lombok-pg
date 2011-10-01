/*
 * Copyright © 2011 Philipp Eichhorn
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
 * Explicitly turns on validation for all method
 * parameter annotated with {@code @Validate.With("methodname")},
 * {@code @Validate.NotNull()} or {@code @Validate.NotEmpty()}.
 * <p>
 * <b>Note:</b> All lombok-pg method-level annotations automatically
 * trigger a parameter validation.
 */
@Target({METHOD, CONSTRUCTOR}) @Retention(SOURCE)
public @interface Validate {
	/**
	 * Specify a custom validation method.
	 * <p>
	 * This annotation also triggers a null-check before the custom validation.
	 * <p>
	 * <b>Note:</b> This works with all types, but the parameter type
	 * has to match the method signature.
	 */
	@Target(PARAMETER) @Retention(SOURCE)
	public static @interface With {
		/**
		 * Name of method that should be used to validate the parameter.
		 */
		String value();
	}
	
	/**
	 * Triggers a null-check for the annotated parameter.
	 */
	@Target(PARAMETER) @Retention(SOURCE)
	public static @interface NotNull {
	}

	/**
	 * Triggers an empty check for the annotated parameter by invoking the
	 * {@code isEmpty} method of the parameter type. So this works with
	 * {@link String Strings} and {@link java.util.Collection Collections}
	 * out of the box.
	 * <p>
	 * This annotation also triggers a null-check before the empty check.
	 */
	@Target(PARAMETER) @Retention(SOURCE)
	public static @interface NotEmpty {
	}
}
