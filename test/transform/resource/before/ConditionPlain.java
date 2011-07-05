import lombok.AccessLevel;

class ConditionPlain {
	@lombok.Getter(AccessLevel.PRIVATE)
	private volatile boolean paused;
	
	@lombok.Signal("canResume")
	void unpause() {
		paused = false;
	}
	
	@lombok.Await(conditionName = "canResume", conditionMethod="isPaused")
	void pause() {
	}
}