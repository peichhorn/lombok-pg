package lombok;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class Functions {

	public static interface Function0<R> {
		public R apply();
	}

	public static interface Function1<T1, R> {
		public R apply(T1 t1);
	}

	public static interface Function2<T1, T2, R> {
		public R apply(T1 t1, T2 t2);
	}

	public static interface Function3<T1, T2, T3, R> {
		public R apply(T1 t1, T2 t2, T3 t3);
	}

	public static interface Function4<T1, T2, T3, T4, R> {
		public R apply(T1 t1, T2 t2, T3 t3, T4 t4);
	}

	public static interface Function5<T1, T2, T3, T4, T5, R> {
		public R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
	}

	public static interface Function6<T1, T2, T3, T4, T5, T6, R> {
		public R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);
	}

	public static interface Function7<T1, T2, T3, T4, T5, T6, T7, R> {
		public R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7);
	}

	public static interface Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> {
		public R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8);
	}
}
