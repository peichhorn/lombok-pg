import lombok.Cleanup;
import java.io.*;
class CleanupQuietly {
	void test1(@lombok.Cleanup(quietly = true) BufferedReader reader) throws Exception {
		for (String next = reader.readLine(); next != null; next = reader.readLine()) System.out.println(next);
	}

	void test2(@lombok.Cleanup(value = "close", quietly = true) BufferedReader reader) throws Exception {
		for (String next = reader.readLine(); next != null; next = reader.readLine()) System.out.println(next);
	}
}