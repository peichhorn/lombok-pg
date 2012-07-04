import lombok.Cleanup;
import java.io.*;
class CleanupOnParameter {
  CleanupOnParameter() {
    super();
  }
  void test(@lombok.Cleanup("close") BufferedReader reader) throws Exception {
    try 
      {
        for (String next = reader.readLine();; (next != null); next = reader.readLine()) 
          System.out.println(next);
      }
    finally
      {
        if ((java.util.Collections.singletonList(reader).get(0) != null))
            {
              reader.close();
            }
      }
  }
}