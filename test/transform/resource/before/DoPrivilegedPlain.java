import java.io.FileInputStream;
import java.io.FileNotFoundException;

import lombok.DoPrivileged;
import lombok.Sanitize;
import lombok.SneakyThrows;

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
	
	@SneakyThrows
	@DoPrivileged
	int test3(@lombok.Validate.NotEmpty @Sanitize.With("cleanFilename") String filename) throws FileNotFoundException {
		FileInputStream fis = new FileInputStream(filename);
		int i = fis.read();
		fis.close();
		return i;
	}

	@SneakyThrows
	@DoPrivileged
	@Sanitize
	int test4(@Sanitize.With("cleanFilename") String filename) throws FileNotFoundException {
		FileInputStream fis = new FileInputStream(filename);
		int i = fis.read();
		fis.close();
		return i;
	}
}