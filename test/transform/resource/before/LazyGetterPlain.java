class LazyGetterPlain {
	static class ValueType {
	}
	
	@lombok.LazyGetter
	private final ValueType fieldName = new ValueType();
}
