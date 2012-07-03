@lombok.RequiredArgsConstructor(callSuper = true)
class RequiredArgsConstructor1 extends Exception {
	final int x;
	String name;
}
@lombok.AllArgsConstructor(callSuper = true, staticName="create")
class AllArgsConstructor1 extends Exception {
	final int x;
	String name;
}
@lombok.NoArgsConstructor(callSuper = true)
class NoArgsConstructor1 extends Exception {
	int x;
	String name;
}