import lombok.AccessLevel;

class ConditionPlain {
  private volatile @lombok.Getter(AccessLevel.PRIVATE) boolean paused;
  private final java.util.concurrent.locks.Lock $canResumeLock = new java.util.concurrent.locks.ReentrantLock();
  private final java.util.concurrent.locks.Condition canResume = $canResumeLock.newCondition();
  
  private @java.lang.SuppressWarnings("all") boolean isPaused() {
    return this.paused;
  }
  
  ConditionPlain() {
    super();
  }
  
  @lombok.Signal("canResume") @java.lang.SuppressWarnings("all") void unpause() {
    this.$canResumeLock.lock();
    try 
      {
        paused = false;
        this.canResume.signal();
      }
    finally
      {
        this.$canResumeLock.unlock();
      }
  }
  
  @lombok.Await(conditionName = "canResume",conditionMethod = "isPaused") @java.lang.SuppressWarnings("all") void pause() throws java.lang.InterruptedException {
    this.$canResumeLock.lock();
    try 
      {
        while (this.isPaused())          this.canResume.await();
      }
    finally
      {
        this.$canResumeLock.unlock();
      }
  }
}