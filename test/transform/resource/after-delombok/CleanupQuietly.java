import java.io.*;
class CleanupQuietly {

	void test1(BufferedReader reader) throws Exception {
		try {
			for (String next = reader.readLine(); next != null; next = reader.readLine()) System.out.println(next);
		} finally {
			if (reader instanceof java.io.Closeable) {
				try {
					((java.io.Closeable)reader).close();
				} catch (final java.io.IOException $ex) {
				}
			}
		}
	}
	
	void test2(BufferedReader reader) throws Exception {
		try {
			for (String next = reader.readLine(); next != null; next = reader.readLine()) System.out.println(next);
		} finally {
			if (java.util.Collections.singletonList(reader).get(0) != null) {
				try {
					reader.close();
				} catch (final java.io.IOException $ex) {
				}
			}
		}
	}
}