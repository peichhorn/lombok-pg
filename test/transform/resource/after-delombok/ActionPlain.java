import lombok.Actions.Action1;

class ActionPlain {

	@java.lang.SuppressWarnings("all")
	public static lombok.Actions.Action1<String> startsWith(final String _prefix) {
		new lombok.Actions.Action1<String>(){

			public void apply(final String string) {
				string.startsWith(_prefix);
			}
		};
	}

	@java.lang.SuppressWarnings("all")
	public static lombok.Actions.Action1<java.lang.Float> sqrt() {
		new lombok.Actions.Action1<java.lang.Float>(){

			public void apply(final java.lang.Float f) {
				Math.sqrt(f);
			}
		};
	}

	@java.lang.SuppressWarnings("all")
	public static <T>lombok.Actions.Action2<T, Action1<T>> notNull() {
		new lombok.Actions.Action2<T, Action1<T>>(){

			public void apply(final T object, final Action1<T> notNullAction) {
				if (object != null) notNullFunction.apply(object);
			}
		};
	}

	@java.lang.SuppressWarnings("all")
	public static lombok.Actions.Action2<float[], double[]> testArrays() {
		new lombok.Actions.Action2<float[], double[]>(){

			public void apply(final float[] a, final double[] b) {
				return;
			}
		};
	}
}