import static lombok.Yield.yield;

import java.lang.Iterable;

class YieldTryBlock {
	public Iterable<String> test() {
		boolean b = true;
		while (true) {
			try {
				if (b) {
					throw new RuntimeException();
				}
				yield("bar");
			} catch(RuntimeException e) {
				yield("foo");
			} finally {
				b = !b;
			}
		}
	}
}
