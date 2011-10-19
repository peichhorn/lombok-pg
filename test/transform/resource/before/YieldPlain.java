import static lombok.Yield.yield;

import java.util.Iterator;
import java.lang.Iterable;

import lombok.Yield;

class YieldPlain {
	public Iterator<String> simple() {
		Yield.yield("A String");
	}

	public Iterator<Long> fib_while() {
		long a = 0;
		long b = 1;
		while (true) {
			yield(a);
			long c = a + b;
			if (c < 0) break;
			a = b;
			b = c;
		}
	}

	public Iterator<Long> fib_while_2() {
		long a = 0;
		long b = 1;
		while (b >= 0) {
			yield(a);
			long c = a + b;
			a = b;
			b = c;
		}
	}

	public Iterable<Long> fib_for() {
		long a = 0;
		long b = 1;
		for (;b >= 0;) {
			yield(a);
			long c = a + b;
			a = b;
			b = c;
		}
	}

	public Iterable<String> complex_foreach(final Iterable<Object> objects) {
		for (Object object : objects) {
			if (object instanceof Class<?>) {
				Class<?> c = (Class<?>) object;
				yield("A String");
				yield(c.getName());
				break;
			}
			yield(object.toString());
		}
		yield("Another String");
	}

	public Iterator<String> complex(final Iterator<Object> objects) {
		yield("Another String");
		while (objects.hasNext()) {
			Object object = objects.next();
			if (object instanceof Class<?>) {
				Class<?> c = (Class<?>) object;
				yield("A String");
				yield(c.getName());
				break;
			}
			yield(object.toString());
		}
		yield("Another String");
	}
}
