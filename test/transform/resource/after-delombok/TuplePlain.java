public class TuplePlain {
	public void tuple1() {
		int a = 1;
		int b = 2;
		final int $tuple0 = a;
		a = b;
		b = $tuple0;
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
				final int $tuple1 = a + b;
				a = b;
				b = 2;
				c = $tuple1;
			}
			{
				String c;
			}
		}
	}
}
