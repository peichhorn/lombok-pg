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
package lombok;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put on any type to make lombok create default implementations for all methods you forgot to implement.
 * <p>
 * Before:
 * <pre>
 * &#64;AutoGenMethodStub
 * class Foo extends Bar implements Buzz {
 *   Foo() {
 *     super();
 *   }
 *   
 *   public void a() { // overrides Bar.a()
 *     super.a();
 *   }
 *   
 *   public void b() { // overrides Buzz.b()
 *   }
 * }
 * </pre>
 * After:
 * <pre>
 * class Foo extends Bar implements Buzz {
 *   Foo() {
 *     super();
 *   }
 *   
 *   public void a() { // overrides Bar.a()
 *     super.a();
 *   }
 *   
 *   public void b() { // overrides Buzz.b()
 *   }
 *   
 *   public int c() { // overrides Buzz.c()
 *     return 0;
 *   }
 *   
 *   public String d() { // overrides Buzz.d()
 *     return null;
 *   }
 * }
 * </pre>
 * <p>
 * Remember that this annotation a curve ball, decent interface design comes first.
 * But in a few cases it may remove massive amounts of boilerplate.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface AutoGenMethodStub {
}
