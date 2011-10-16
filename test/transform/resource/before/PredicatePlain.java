import lombok.Predicate;
import lombok.Validate.NotNull;

class PredicatePlain {

	@Predicate
	public boolean containsEmptyString(@NotNull Iterable<String> list) {
		for (String entry : list) if (entry.isEmpty()) return true;
		return false;
	}
}