class DoPrivilegedPlain {
  private boolean b = true;

  DoPrivilegedPlain() {
    super();
  }

  @lombok.DoPrivileged @java.lang.SuppressWarnings("all") int test1() {
    return java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<java.lang.Integer>() {
  public java.lang.Integer run() {
    System.out.println("Test");
    return 0;
  }
});
  }

  @lombok.DoPrivileged @java.lang.SuppressWarnings("all") void test2() {
    java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<java.lang.Void>() {
  public java.lang.Void run() {
    if (b)
        {
          return null;
        }
    System.out.println("Test");
    return null;
  }
});
  }
}