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

import java.lang.annotation.*;

/**
 * Put on any type to make lombok create a builder for it.
 * <p>
 * Before:
 * <pre>
 * &#64;lombok.Builder
 * class Foo {
 *   private final String a;
 *   private final int b;
 *   private String optionalC = "default";
 *   private java.util.List&lt;java.lang.Long&gt; optionalD;
 * }
 * </pre>
 * After:
 * <pre>
 * class Foo {
 *   private final String a;
 *   private final int b;
 *   private String optionalC;
 *   private java.util.List&lt;java.lang.Long&gt; optionalD;
 *
 *   private Foo(final $Builder builder) {
 *     super();
 *     this.a = builder.a;
 *     this.b = builder.b;
 *     this.optionalVal1 = builder.optionalVal1;
 *     this.optionalVal2 = builder.optionalVal2;
 *   }
 *
 *   public static interface $ADef {
 *     public $BDef a(final String a);
 *   }
 *
 *   public static interface $BDef {
 *     public $OptionalDef b(final int b);
 *   }
 *
 *   public static interface $OptionalDef {
 *     public $OptionalDef optionalC(final String optionalC);
 *
 *     public $OptionalDef optionalD(final java.util.List&lt;java.lang.Long&gt; optionalD);
 *
 *     public Foo build();
 *   }
 *
 *   private static class $Builder implements $ADef, $BDef, $OptionalDef {
 *     private String a;
 *     private int b;
 *     private String optionalC = "default";
 *     private java.util.List&lt;java.lang.Long&gt; optionalD;
 *
 *     public $BDef a(final String a) {
 *       this.a = a;
 *       return this;
 *     }
 *
 *     public $OptionalDef b(final int b) {
 *       this.b = b;
 *       return this;
 *     }
 *
 *     public $OptionalDef optionalC(final String optionalC) {
 *       this.optionalC = optionalC;
 *       return this;
 *     }
 *
 *     public $OptionalDef optionalD(final java.util.List&lt;java.lang.Long&gt; optionalD) {
 *       this.optionalD = optionalD;
 *       return this;
 *     }
 *
 *     public Foo build() {
 *       return new Foo(this);
 *     }
 *   }
 *
 *   public static $ADef create() {
 *     return new $Builder();
 *   }
 * }
 * </pre>
 * <p>
 * <b>Note:</b> For each field that is a initialized collection( or map), the methods add/addAll( or put/putAll) will be generated instead of the fluent-set method.
 * This behavior can be disabled via {@link #convenientMethods() convenientMethods = false}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Builder {
	/**
	 * If you want the create-method to be non-public, you can specify an alternate access level here.
	 */
	lombok.AccessLevel value() default lombok.AccessLevel.PUBLIC;

	/**
	 * If specified all builder methods will be prefixed with this string.
	 * <p>
	 * A common example would be {@code @Builder(prefix="with")} which will
	 * generate builder methods like {@code .withValue(value)}.
	 */
	String prefix() default "";

	/**
	 * Any fields listed here will not appear in the builder.
	 */
	String[] exclude() default {};

	/**
	 * If you don't want collection-specific methods (add, addAll, put, putAll) you can disable them here.
	 */
	boolean convenientMethods() default true;

	/**
	 * For each method listed here a method will appear in the builder.
	 * <p>
	 * A common example would be <code>@Builder(callMethods={"execute", "toString"})</code> which would allow something like:
	 * <pre>
	 * Java.java().jar("test.jar").Xbootclasspatha("libs/asm.jar").execute()}
	 * Java.java().jar("test.jar").Xbootclasspatha("libs/asm.jar").toString()}
	 * </pre>
	 */
	String[] callMethods() default {};

	/**
	 * Use this on methods in a {@link Builder @Builder}-annotated class to specify
	 * extensions for the generated builder.
	 * <p>
	 * <b>Note:</b> For this to work, the methods annotated by {@link Builder.Extension @Builder.Extension},
	 * need to be private and must return void. And if you want to set a required value in you own extension,
	 * you need to set all other required values too.
	 */
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.SOURCE)
	public static @interface Extension {
	}
}
