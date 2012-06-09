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
 * Lock Conditions
 * 
 * <pre>
 * void methodAnnotatedWithAwait() throws java.lang.InterruptedException {
 *   this.&lt;LOCK_NAME&gt;.lock();
 *   try {
 *     while (this.&lt;CONDITION_METHOD&gt;()) {
 *       this.&lt;CONDITION_NAME&gt;.await();
 *     }
 * 
 *     // method body
 * 
 *   } finally {
 *     this.&lt;LOCK_NAME&gt;.unlock();
 *   }
 * }
 * 
 * <pre>
 */
@Target(METHOD)
@Retention(SOURCE)
public @interface Await {
	/**
	 * Name of the condition.
	 * <p>
	 * If no condition with the specified name exists a new {@link java.util.concurrent.locks.Condition Condition} will
	 * be created, using this name.
	 */
	String conditionName();

	/**
	 * Specifies the place to put the await-block, default is {@link lombok.Position BEFORE}.
	 * <p>
	 * Supported positions:
	 * <ul>
	 * <li>{@link lombok.Position BEFORE} - before the method body</li>
	 * <li>{@link lombok.Position AFTER} - after the method body</li>
	 * </ul>
	 */
	Position pos() default lombok.Position.BEFORE;

	/**
	 * Name of the lock, default is {@code $<CONDITION_NAME>Lock}.
	 * <p>
	 * If no lock with the specified name exists a new {@link java.util.concurrent.locks.Lock Lock} will be created,
	 * using this name.
	 */
	String lockName() default "";

	/**
	 * Method to verify if the condition is met.
	 * <p>
	 * The method must return a {@code boolean} and may not require any parameters.
	 */
	String conditionMethod();
}
