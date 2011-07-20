class ConditionPlain {
	private volatile boolean paused;
	
	@java.lang.SuppressWarnings("all")
	void unpause() {
		this.$canResumeLock.lock();
		try {
			paused = false;
			this.canResume.signal();
		} finally {
			this.$canResumeLock.unlock();
		}
	}
	
	@java.lang.SuppressWarnings("all")
	void pause() {
		this.$canResumeLock.lock();
		try {
			try {
				while (this.isPaused()) this.canResume.await();
			} catch (final java.lang.InterruptedException e) {
				throw new java.lang.RuntimeException(e);
			}
		} finally {
			this.$canResumeLock.unlock();
		}
	}
	
	@java.lang.SuppressWarnings("all")
	private boolean isPaused() {
		return this.paused;
	}
	
	private final java.util.concurrent.locks.Lock $canResumeLock = new java.util.concurrent.locks.ReentrantLock();
	private final java.util.concurrent.locks.Condition canResume = $canResumeLock.newCondition();
}