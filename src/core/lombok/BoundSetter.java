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
import static lombok.AccessLevel.PUBLIC;

import java.lang.annotation.*;

import lombok.AccessLevel;

/**
 * Creates a "bound" setter for an annotated field.
 * <p>
 * Before:
 * 
 * <pre>
 * public class Mountain {
 * 
 * 	&#064;GenerateBoundSetter
 * 	private String name;
 * }
 * </pre>
 * 
 * After:
 * 
 * <pre>
 * public class Mountain {
 * 	public static final String PROP_NAME = &quot;name&quot;;
 * 
 * 	private String name;
 * 
 * 	public void setName(String value) {
 * 		String oldValue = name;
 * 		firstName = value;
 * 		getPropertySupport().firePropertyChange(PROP_NAME, oldValue, name);
 * 	}
 * }
 * </pre>
 */
@Target({ FIELD, TYPE })
@Retention(SOURCE)
public @interface BoundSetter {

	/**
	 * If you want your setter to be non-public, you can specify an alternate access level here.
	 */
	AccessLevel value() default PUBLIC;
}
