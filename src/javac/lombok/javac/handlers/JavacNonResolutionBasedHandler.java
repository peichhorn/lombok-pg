package lombok.javac.handlers;

abstract class JavacNonResolutionBasedHandler {
	public final boolean isResolutionBased() {
		return false;
	} 
}