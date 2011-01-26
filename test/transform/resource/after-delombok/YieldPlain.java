import static lombok.Yield.yield;

import java.util.Iterator;
import java.util.Iterable;

import javax.swing.JScrollPane;
class YieldPlain {
	public Iterator<Integer> fib_while() {
		int a = 0;
		int b = 1;
		while (true) {
			yield(a);
			int c = a + b;
			if (c < 0) break;
			a = b;
			b = c;
		}
	}
	
	public Iterator<Integer> fib_while_2() {
		int a = 0;
		int b = 1;
		while (b >= 0) {
			yield(a);
			int c = a + b;
			a = b;
			b = c;
		}
	}

	public Iterable<Integer> fib_for() {
		int a = 0;
		int b = 1;
		for (;b >= 0;) {
			yield(a);
			int c = a + b;
			a = b;
			b = c;
		}
	}
	
	public Iterable<String> complex_foreach(Iterable<Object> objects) {
		for (Object object : objects) {
			if (object instanceof Class<?>) {
				Class<?> c = (Class<?>) object;
				yield("A String");
				yield(c.getName());
				break;
			}
			yield(object.toString);
		}
		yield("Another String");
	}
	
	public Iterator<String> complex(Iterator<Object> objects) {
		while (objects.hasNext()) {
			Object object = objects.next();
			if (object instanceof Class<?>) {
				Class<?> c = (Class<?>) object;
				yield("A String");
				yield(c.getName());
				break;
			}
			yield(object.toString);
		}
		yield("Another String");
	}
}
