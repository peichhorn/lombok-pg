class LazyGetterPlain {

	static class ValueType {

	}

	private ValueType fieldName;
	private volatile boolean $fieldNameInitialized;
	private final java.lang.Object[] $fieldNameLock = new java.lang.Object[0];

	@java.lang.SuppressWarnings("all")
	public ValueType getFieldName() {
		if (!this.$fieldNameInitialized) {
			synchronized (this.$fieldNameLock) {
				if (!this.$fieldNameInitialized) {
					this.fieldName = new ValueType();
					this.$fieldNameInitialized = true;
				}
			}
		}
		return this.fieldName;
	}
}
