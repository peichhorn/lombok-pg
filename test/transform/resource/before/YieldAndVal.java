import static lombok.Yield.yield;

import java.lang.Iterable;
import java.util.List;
import lombok.Functions.Function1;
import lombok.val;

class YieldAndVal {
	
	public static <S, T> Iterable<T> needsMoreVal(final Iterable<S> values, final Function1<S, List<T>> selector) {
		for (val item : values) {
			val subItems = selector.apply(item);
			if (subItems != null) {
				for (val subItem : subItems) yield(subItem);
			}
		}
	}
}
