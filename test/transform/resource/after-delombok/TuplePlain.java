public class TuplePlain {
	public void tuple1() {
		int a = 1;
		int b = 2;
		int c = 3;
		int[] d = new int[]{4, 5, 6};
		final int $tuple0 = a;
		a = b;
		b = $tuple0;
		final int $tuple1 = d;
		c = $tuple1[0];
		b = $tuple1[1];
		a = $tuple1[2];
		a = b;
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
				final int $tuple2 = a + b;
				a = b;
				b = 2;
				c = $tuple2;
			}
			{
				String c;
			}
		}
	}
}
