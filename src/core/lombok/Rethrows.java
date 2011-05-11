package lombok;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Before:
 * <pre>
 * &#64;Rethrows({
 *   &#64;Rethrow(IOException.class),
 *   &#64;Rethrow(value=NullPointerException.class,as=IllegalArgumentException.class)
 * })
 * void testMethod(Object arg) {
 *   // do something
 * }
 * 
 * void testMethod() throw IOException as RuntimeException, NullPointerException as IllegalArgumentException {
 *   // do something
 * }
 * </pre>
 * After:
 * <pre>
 * void testMethod(Object arg) {
 *   try {
 *     // do something
 *   } catch (IOException e1) {
 *     throw new RuntimeException(e1);
 *   } catch (NullPointerException e2) {
 *     throw new IllegalArgumentException(e2);
 *   } 
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Rethrows {
	Rethrow[] value();
}
