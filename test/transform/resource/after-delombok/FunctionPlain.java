import lombok.Functions.Function1;

class FunctionPlain {
	
	public void test() {
		Float foo = 1.618F;
		notNull(foo, sqrt());
	}
	
	@java.lang.SuppressWarnings("all")
	public static lombok.Functions.Function1<String, java.lang.Boolean> startsWith(final String _prefix) {
		return new lombok.Functions.Function1<String, java.lang.Boolean>(){
			public java.lang.Boolean apply(final String string) {
				if (string == null) {
					throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "string", 1));
				}
				if (_prefix == null) {
					throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "_prefix", 2));
				}
				return string.startsWith(_prefix);
			}
		};
	}
	
	@java.lang.SuppressWarnings("all")
	public static lombok.Functions.Function1<java.lang.Float, java.lang.Float> sqrt() {
		return new lombok.Functions.Function1<java.lang.Float, java.lang.Float>(){
			public java.lang.Float apply(final java.lang.Float f) {
				return (float)Math.sqrt(f);
			}
		};
	}
	
	@java.lang.SuppressWarnings("all")
	public static <T>lombok.Functions.Function2<T, Function1<T, Void>, java.lang.Void> notNull() {
		return new lombok.Functions.Function2<T, Function1<T, Void>, java.lang.Void>(){
			public java.lang.Void apply(final T object, final Function1<T, Void> notNullFunction) {
				if (object != null) notNullFunction.apply(object);
				return null;
			}
		};
	}
	
	@java.lang.SuppressWarnings("all")
	public static lombok.Functions.Function2<float[], double[], int[]> testArrays() {
		return new lombok.Functions.Function2<float[], double[], int[]>(){
			
			public int[] apply(final float[] a, final double[] b) {
				return null;
			}
		};
	}
}