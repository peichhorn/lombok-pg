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
import java.text.Normalizer;

/**
 * Explicitly turns on sanitation for all method parameter annotated with {@code @Sanitize.With("methodname")} or
 * {@code @Sanitize.Normalize}.
 * <p>
 * <b>Note:</b> All lombok-pg method-level annotations automatically trigger a parameter sanitation.
 */
@Target({ METHOD, CONSTRUCTOR })
@Retention(SOURCE)
public @interface Sanitize {

	/**
	 * Specify a custom sanitation method.
	 * <p>
	 * <b>Note:</b> This works with all types, but the parameter type has to match the method signature.
	 */
	@Target(PARAMETER)
	@Retention(SOURCE)
	public static @interface With {
		String value();
	}

	/**
	 * {@link String} parameter gets normalized using {@link Normalizer#normalize(CharSequence, Normalizer.Form)} with
	 * default form being {@link java.text.Normalizer.Form#NFKC NFKC}
	 * <p>
	 * <b>Note:</b> This works only on {@link String Strings}.
	 */
	@Target(PARAMETER)
	@Retention(SOURCE)
	public static @interface Normalize {
		Normalizer.Form value() default Normalizer.Form.NFKC;
	}
}
