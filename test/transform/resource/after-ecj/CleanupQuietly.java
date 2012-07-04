import lombok.Cleanup;
import java.io.*;
class CleanupQuietly {
  CleanupQuietly() {
    super();
  }
  void test1(@lombok.Cleanup(quietly = true) BufferedReader reader) throws Exception {
    try 
      {
        for (String next = reader.readLine();; (next != null); next = reader.readLine()) 
          System.out.println(next);
      }
    finally
      {
        if ((reader instanceof java.io.Closeable))
            {
              try 
                {
                  (java.io.Closeable) reader.close();
                }
              catch (final java.io.IOException $ex)                 {
                }
            }
      }
  }
  void test2(@lombok.Cleanup(value = "close",quietly = true) BufferedReader reader) throws Exception {
    try 
      {
        for (String next = reader.readLine();; (next != null); next = reader.readLine()) 
          System.out.println(next);
      }
    finally
      {
        if ((java.util.Collections.singletonList(reader).get(0) != null))
            {
              try 
                {
                  reader.close();
                }
              catch (final java.io.IOException $ex)                 {
                }
            }
      }
  }
}