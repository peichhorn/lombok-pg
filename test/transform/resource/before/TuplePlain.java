import static lombok.Tuple.tuple;

public class TuplePlain {
	public void tuple1() {
		int a = 1;
		int b = 2;
		tuple(a, b) = tuple(b, a);
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
