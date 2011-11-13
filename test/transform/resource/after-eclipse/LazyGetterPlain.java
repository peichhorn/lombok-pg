class LazyGetterPlain {
  static class ValueType {
    ValueType() {
      super();
    }
  }

  private volatile boolean $fieldNameInitialized;
  private final java.lang.Object[] $fieldNameLock = new java.lang.Object[0];
  private @lombok.LazyGetter ValueType fieldName;

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