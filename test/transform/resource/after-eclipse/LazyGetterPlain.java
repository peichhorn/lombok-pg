class LazyGetterPlain {
  static class ValueType {
    ValueType() {
      super();
    }
  }

  private @lombok.LazyGetter ValueType fieldName;
  private volatile boolean $fieldNameInitialized;
  private final java.lang.Object[] $fieldNameLock = new java.lang.Object[0];

  public @java.lang.SuppressWarnings("all") ValueType getFieldName() {
    if ((! this.$fieldNameInitialized))
        {
          synchronized (this.$fieldNameLock)
            {
              if ((! this.$fieldNameInitialized))
                  {
                    this.fieldName = new ValueType();
                    this.$fieldNameInitialized = true;
                  }
            }
        }
    return this.fieldName;
  }

  LazyGetterPlain() {
    super();
  }
}