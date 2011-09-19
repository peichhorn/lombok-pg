package lombok;

public @interface Function {
	Class<?> template() default Functions.class;
}
