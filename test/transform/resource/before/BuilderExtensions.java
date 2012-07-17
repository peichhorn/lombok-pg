@lombok.Builder(exclude={"optionalVal3"}, convenientMethods=false)
class BuilderExtensions {
	private final String text;
	private final int id;
	private String optionalVal1;
	private java.util.List<java.lang.Long> optionalVal2;
	private long optionalVal3;
	
	@lombok.Builder.Extension
	private void idAndText(int id, String text) {
		this.id = id;
		this.text = text;
	}	

	@lombok.Builder.Extension(fields={"id", "text"})
	private void idAsStringAndText(String id, String text) {
		this.id = java.lang.Integer.valueOf(id);
		this.text = text;
	}
	
	@lombok.Builder.Extension
	private void optionalVal1(final Class<?> clazz) {
		this.optionalVal1 = clazz.getSimpleName();
	}
}