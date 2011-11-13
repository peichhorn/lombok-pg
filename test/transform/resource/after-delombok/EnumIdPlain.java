class EnumIdPlain {
	public enum Status {
		WAITING(0),
		READY(1),
		SKIPPED(-1),
		COMPLETED(5);
		
		private static final java.util.Map<java.lang.Integer, Status> $CODE_LOOKUP = new java.util.HashMap<java.lang.Integer, Status>();
		private final int code;
		
		@java.lang.SuppressWarnings("all")
		private Status(final int code) {
			this.code = code;
		}
		
		static {
			for (Status status : Status.values()) {
				$CODE_LOOKUP.put(status.code, status);
			}
		}
		
		@java.lang.SuppressWarnings("all")
		public static Status findByCode(final int code) {
			if ($CODE_LOOKUP.containsKey(code)) {
				return $CODE_LOOKUP.get(code);
			}
			throw new java.lang.IllegalArgumentException(java.lang.String.format("Enumeration \'Status\' has no value \'%s\'", code));
		}
		
		@java.lang.SuppressWarnings("all")
		public int getCode() {
			return this.code;
		}
	}
}
