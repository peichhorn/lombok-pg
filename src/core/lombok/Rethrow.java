package lombok;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Before:
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
 * After:
 * <pre>
 * void testMethod() {
 *   try {
 *     // do something
 *   } catch (IOException e1) {
 *     throw new RuntimeException(e1);
 *   } 
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Rethrow {
	Class<? extends Exception>[] value() default {};
	
	Class<? extends Exception> as() default RuntimeException.class;
	
	String message() default "";
}
