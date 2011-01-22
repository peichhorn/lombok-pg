class SetterFluent {
	int nonfluent;
	int fluent;
	int nonfluent_accessLevel;
	int fluent_accessLevel;
	
	@java.lang.SuppressWarnings("all")
	public void setNonfluent(final int nonfluent) {
		this.nonfluent = nonfluent;
	}
	
	@java.lang.SuppressWarnings("all")
	public SetterFluent fluent(final int fluent) {
		this.fluent = fluent;
		return this;
	}
	
	@java.lang.SuppressWarnings("all")
	private void setNonfluent_accessLevel(final int nonfluent_accessLevel) {
		this.nonfluent_accessLevel = nonfluent_accessLevel;
	}
	
	@java.lang.SuppressWarnings("all")
	private SetterFluent fluent_accessLevel(final int fluent_accessLevel) {
		this.fluent_accessLevel = fluent_accessLevel;
		return this;
	}
}