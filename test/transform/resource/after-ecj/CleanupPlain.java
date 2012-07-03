import lombok.Cleanup;
import java.io.*;
class CleanupPlain {
  CleanupPlain() {
    super();
  }
  void test() throws Exception {
    @lombok.Cleanup("close") InputStream in = new FileInputStream("in");
    try 
      {
        @Cleanup OutputStream out = new FileOutputStream("out");
        try 
          {
            if (in.markSupported())
                {
                  out.flush();
                }
          }
        finally
          {
            if ((out instanceof java.io.Closeable))
                {
                  (java.io.Closeable) out.close();
                }
          }
      }
    finally
      {
        if ((java.util.Collections.singletonList(in).get(0) != null))
            {
              in.close();
            }
      }
  }
}