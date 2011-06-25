@lombok.Builder
class BuilderBrokenExtension {
	private final String text;
	private final int id;
	
	@lombok.Builder.Extension
	private void brokenExtension() {
		this.id = 42;
	}
}