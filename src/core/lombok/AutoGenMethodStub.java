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
package lombok;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.*;

/**
 * With this annotation you can avoid cluttering your code with empty methods that an interface forces you to implement.
 * Just implement the ones you need and lombok will create empty stubs for the rest.
 * <p>
 * Before:
 * 
 * <pre>
 * &#064;AutoGenMethodStub
 * class Foo extends Bar implements Buzz {
 * 
 * 	public void a() { // overrides Bar.a()
 * 		super.a();
 * 	}
 * 
 * 	public void b() { // overrides Buzz.b()
 * 	}
 * }
 * </pre>
 * 
 * After:
 * 
 * <pre>
 * class Foo extends Bar implements Buzz {
 * 
 * 	public void a() { // overrides Bar.a()
 * 		super.a();
 * 	}
 * 
 * 	public void b() { // overrides Buzz.b()
 * 	}
 * 
 * 	public int c() { // overrides Buzz.c()
 * 		return 0;
 * 	}
 * 
 * 	public String d() { // overrides Buzz.d()
 * 		return null;
 * 	}
 * }
 * </pre>
 * <p>
 * If you prefer an {@link java.lang.UnsupportedOperationException UnsupportedOperationException} being thrown rather
 * than the default value being returned, use: {@code @AutoGenMethodStub(throwException = true)}.
 * <p>
 * <b>Note:</b> Remember that this annotation is a curve ball, decent interface design comes first. But in some cases it
 * removes massive amounts of boilerplate.
 */
@Target(TYPE)
@Retention(SOURCE)
public @interface AutoGenMethodStub {
	/**
	 * If you prefer an {@link java.lang.UnsupportedOperationException UnsupportedOperationException} being thrown
	 * rather than the default value being returned, use {@code @AutoGenMethodStub(throwException = true)}.
	 */
	boolean throwException() default false;
}
