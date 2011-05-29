import java.io.FileInputStream;
import java.io.FileNotFoundException;

import lombok.DoPrivileged;
import lombok.DoPrivileged.SanitizeWith;

class DoPrivilegedPlain {
	private boolean b = true;

	@lombok.DoPrivileged
	int test1() {
		System.out.println("Test");
		return 0;
	}

	@lombok.DoPrivileged
	void test2() {
		if (b) {
			return;
		}
		System.out.println("Test");
	}

	String cleanFilename(String filename) {
		filename = filename.replace("\\", "/").toLowerCase();
		if (filename.startsWith("c:/windows/system32")) {
			throw new IllegalArgumentException("Trying to access forbidden file");
		}
		return filename;
	}

	@DoPrivileged
	int test3(@SanitizeWith("cleanFilename") String filename) throws FileNotFoundException {
		FileInputStream fis = new FileInputStream(filename);
		int i = fis.read();
		fis.close();
		return i;
	}
}