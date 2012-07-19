import lombok.Builder;

@Builder(allowReset = true)
class BuilderAllowRest {
	private final String finalField;
	private int initializedPrimitiveField = 42;
	private Boolean initializedField = Boolean.FALSE;
	private double primitiveField;
	private Float field;
}