package lombok.core.util;

import static java.util.Arrays.asList;

import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Lists {

	public static <T> List<T> list(T... a) {
		return asList(a);
	}
}
