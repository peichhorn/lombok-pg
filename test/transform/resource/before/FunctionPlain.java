import lombok.Functions.Function1;
import lombok.Function;
import lombok.Validate.NotNull;

class FunctionPlain {

	@Function
	public static boolean startsWith(@NotNull String string, @NotNull String _prefix) {
		return string.startsWith(_prefix);
	}

	@Function
	public static float sqrt(float f) {
		return (float) Math.sqrt(f);
	}

	@Function
	public static <T> void notNull(T object, Function1<T, T> notNullFunction) {
		if (object != null) notNullFunction.apply(object);
	}

	@Function
	public static int[] testArrays(float[] a, double[] b) {
		return null;
	}
}