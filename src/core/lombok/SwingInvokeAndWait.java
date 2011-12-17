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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.*;

/**
 * Wraps a bit of Swing pain.
 * <p>
 * With lombok:
 * 
 * <pre>
 * class SwingApp {
 * 	private final JFrame frame = new JFrame();
 * 
 * 	&#064;SwingInvokeAndWait
 * 	void start() throws Exception {
 * 		frame.setTitle(&quot;SwingApp&quot;);
 * 		frame.setVisible(true);
 * 	}
 * }
 * </pre>
 * 
 * Vanilla Java:
 * 
 * <pre>
 * class SwingApp {
 * 	private final JFrame frame = new JFrame();
 * 
 * 	void start() throws Exception {
 * 		final java.lang.Runnable $startRunnable = new java.lang.Runnable() {
 * 			public void run() {
 * 				frame.setTitle(&quot;SwingApp&quot;);
 * 				frame.setVisible(true);
 * 			}
 * 		};
 * 		if (java.awt.EventQueue.isDispatchThread()) {
 * 			$startRunnable();
 * 		} else {
 * 			try {
 * 				java.awt.EventQueue.invokeAndWait($startRunnable);
 * 			} catch (java.lang.InterruptedException $ex1) {
 * 			} catch (java.lang.reflect.InvocationTargetException $ex2) {
 * 				final java.lang.Throwable $cause = $ex2.getCause();
 * 				if ($cause instanceof FileNotFoundException) throw (FileNotFoundException) $cause;
 * 				if ($cause != null) throw new java.lang.RuntimeException($cause);
 * 			}
 * 		}
 * 	}
 * }
 * </pre>
 */
@Target(METHOD)
@Retention(SOURCE)
public @interface SwingInvokeAndWait {
}
