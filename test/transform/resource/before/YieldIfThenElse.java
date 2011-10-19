import static lombok.Yield.yield;

import java.lang.Iterable;

class YieldIfThenElse {
	public Iterable<String> test() {
		boolean b = true;
		while (true) {
			if (b) {
				yield("foo");
			} else {
				yield("bar");
			}
			b = !b;
		}
	}
}