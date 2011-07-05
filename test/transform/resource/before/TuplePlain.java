import static lombok.Tuple.tuple;

public class TuplePlain {
	public void tuple1() {
		int a, b, c = tuple(1, 2, 3);
		int[] d = new int[]{4, 5, 6};
		tuple(a, b) = tuple(b, a);
		tuple(c, b, a) = tuple(d);
		tuple(a) = tuple(b);
	}
	
	private static class Shadowing {
		private float c;
		
		public void tuple2() {
			{
				String c;
			}
			{
				int a = 0;
				int b = 1;
				int c;
				tuple(a, b, c) = tuple(b, 2, a + b);
			}
			{
				String c;
			}
		}
	}
}
