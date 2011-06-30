import java.io.FileInputStream;
import java.io.FileNotFoundException;

import lombok.DoPrivileged.SanitizeWith;

class DoPrivilegedPlain {
	private boolean b = true;

	@java.lang.SuppressWarnings("all")
	int test1() {
		try {
			return java.security.AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<java.lang.Integer>(){
				public java.lang.Integer run() {
					System.out.println("Test");
					return 0;
				}
			});
		} catch (final java.security.PrivilegedActionException $ex) {
			final java.lang.Throwable $cause = $ex.getCause();
			throw new java.lang.RuntimeException($cause);
		}
	}

	@java.lang.SuppressWarnings("all")
	void test2() {
		try {
			java.security.AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<java.lang.Void>(){
				public java.lang.Void run() {
					if (b) {
						return null;
					}
					System.out.println("Test");
					return null;
				}
			});
		} catch (final java.security.PrivilegedActionException $ex) {
			final java.lang.Throwable $cause = $ex.getCause();
			throw new java.lang.RuntimeException($cause);
		}
	}

	String cleanFilename(String filename) {
		filename = filename.replace("\\", "/").toLowerCase();
		if (filename.startsWith("c:/windows/system32")) {
			throw new IllegalArgumentException("Trying to access forbidden file");
		}
		return filename;
	}

	@java.lang.SuppressWarnings("all")
	int test3(final String $filename) throws FileNotFoundException {
		String filename = cleanFilename($filename);
		try {
			return java.security.AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<java.lang.Integer>(){
				public java.lang.Integer run() throws FileNotFoundException {
					FileInputStream fis = new FileInputStream(filename);
					int i = fis.read();
					fis.close();
					return i;
				}
			});
		} catch (final java.security.PrivilegedActionException $ex) {
			final java.lang.Throwable $cause = $ex.getCause();
			if ($cause instanceof FileNotFoundException) throw (FileNotFoundException)$cause;
			throw new java.lang.RuntimeException($cause);
		}
	}
}