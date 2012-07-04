import lombok.Cleanup;
import java.io.*;

class CleanupOnParameter {
	void test(@lombok.Cleanup("close") BufferedReader reader) throws Exception {
		for (String next = reader.readLine(); next != null; next = reader.readLine()) System.out.println(next);
	}
}