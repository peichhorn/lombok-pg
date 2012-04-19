import lombok.Actions.Action1;
import lombok.Action;
import lombok.Validate.NotNull;

class ActionPlain {

	@Action
	public static void testAction0() {
		System.out.println("Action0");
	}

	@Action
	public static void startsWith(String string, String _prefix) {
		string.startsWith(_prefix);
	}

	@Action
	public static void sqrt(float f) {
		Math.sqrt(f);
	}

	@Action
	public static <T> void notNull(T object, @NotNull Action1<T> notNullAction) {
		if (object != null) notNullAction.apply(object);
	}

	@Action
	public static void testArrays(float[] a, double[] b) {
		return;
	}
}