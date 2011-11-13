import lombok.AccessLevel;

class ConditionPlain {
  private final java.util.concurrent.locks.Lock $canResumeLock = new java.util.concurrent.locks.ReentrantLock();
  private final java.util.concurrent.locks.Condition canResume = $canResumeLock.newCondition();
  private volatile @lombok.Getter(AccessLevel.PRIVATE) boolean paused;
  
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
  
  @lombok.Await(conditionName = "canResume",conditionMethod = "isPaused") @java.lang.SuppressWarnings("all") void pause() {
    this.$canResumeLock.lock();
    try 
      {
        try 
          {
            while (this.isPaused())              this.canResume.await();
          }
        catch (final java.lang.InterruptedException e)           {
            throw new java.lang.RuntimeException(e);
          }
      }
    finally
      {
        this.$canResumeLock.unlock();
      }
  }
}