public class TuplePlain {
  private static class Shadowing {
    private float c;
    private Shadowing() {
      super();
    }
    
    public void tuple2() {
      {
        String c;
      }
      {
        int a = 0;
        int b = 1;
        int c;
        final int $tuple0 = (a + b);
        a = b;
        b = 2;
        c = $tuple0;
      }
      {
        String c;
      }
    }
  }
  public TuplePlain() {
    super();
  }
  public void tuple1() {
    int a = 1;
    int b = 2;
    int c = 3;
    int[] d = new int[]{4, 5, 6};
    final int $tuple1 = a;
    a = b;
    b = $tuple1;
    final int[] $tuple2 = d;
    c = $tuple2[0];
    b = $tuple2[1];
    a = $tuple2[2];
    a = b;
  }
}
