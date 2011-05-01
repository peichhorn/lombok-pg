class DoPrivilegedPlain {
	private boolean b = true;
	
	@lombok.DoPrivileged
	int test1() {
		System.out.println("Test");
		return 0;
	}

	@lombok.DoPrivileged
	void test2() {
		if (b) {
			return;
		}
		System.out.println("Test");
	}
}