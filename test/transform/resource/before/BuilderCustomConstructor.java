import lombok.Builder;

@Builder
class BuilderCustomConstructor {
	private final String name;
	private final String surname;

	private BuilderCustomConstructor(final Builder builder) {
		super();
		this.name= builder.name.trim();
		this.surname = builder.surname.trim();
	}
}