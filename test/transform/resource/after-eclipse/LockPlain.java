import java.util.Map;
import java.util.HashMap;
class LockPlain {
  private Map<String, String> dictionary = new HashMap<String, String>();
  private final java.util.concurrent.locks.ReadWriteLock dictionaryLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
  
  LockPlain() {
    super();
  }
  
  public @lombok.WriteLock("dictionaryLock") @java.lang.SuppressWarnings("all") void put(String key, String value) {
    this.dictionaryLock.writeLock().lock();
    try 
      {
        dictionary.put(key, value);
      }
    finally
      {
        this.dictionaryLock.writeLock().unlock();
      }
  }
  
  public @lombok.ReadLock("dictionaryLock") @java.lang.SuppressWarnings("all") String get(String key) {
    this.dictionaryLock.readLock().lock();
    try 
      {
        return dictionary.get(key);
      }
    finally
      {
        this.dictionaryLock.readLock().unlock();
      }
  }
}