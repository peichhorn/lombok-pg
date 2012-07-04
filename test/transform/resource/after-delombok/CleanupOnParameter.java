import java.io.*;

class CleanupOnParameter {
	void test(BufferedReader reader) throws Exception {
		try {
			for (String next = reader.readLine(); next != null; next = reader.readLine()) System.out.println(next);
		} finally {
			if (java.util.Collections.singletonList(reader).get(0) != null) {
				reader.close();
			}
		}
	}
}