class LazyGetterPlain {
	private volatile boolean $fieldNameInitialized;
	private final java.lang.Object[] $fieldNameLock = new java.lang.Object[0];

	static class ValueType {
	}

	private ValueType fieldName;

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
