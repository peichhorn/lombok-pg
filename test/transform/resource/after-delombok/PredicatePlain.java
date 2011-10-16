class PredicatePlain {

	@java.lang.SuppressWarnings("all")
	public lombok.Predicates.Predicate1<Iterable<String>> containsEmptyString() {
		return new lombok.Predicates.Predicate1<Iterable<String>>(){
			public boolean evaluate(final Iterable<String> list) {
				if (list == null) {
					throw new java.lang.NullPointerException(java.lang.String.format("The validated object \'%s\' (argument #%s) is null", "list", 1));
				}
				for (String entry : list) if (entry.isEmpty()) return true;
				return false;
			}
		};
	}
}