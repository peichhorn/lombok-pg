import java.util.Map;
import java.util.HashMap;
class LockPlain {
  private Map<String, String> dictionary = new HashMap<String, String>();
  private final java.util.concurrent.locks.ReadWriteLock dictionaryLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
  
  LockPlain() {
    super();
  }
  
  public @lombok.WriteLock("dictionaryLock") @java.lang.SuppressWarnings("all") void put(final @lombok.Validate.NotEmpty @lombok.Sanitize.With("checkKey") String key, final String value) {
    if (((key == null) || key.isEmpty()))
        {
          throw new java.lang.IllegalArgumentException("The validated object is empty");
        }
    final String sanitizedKey = checkKey(key);
    this.dictionaryLock.writeLock().lock();
    try 
      {
        dictionary.put(sanitizedKey, value);
      }
    finally
      {
        this.dictionaryLock.writeLock().unlock();
      }
  }
  
  public @lombok.ReadLock("dictionaryLock") @java.lang.SuppressWarnings("all") String get(final @lombok.Validate.NotEmpty @lombok.Sanitize.With("checkKey") String key) {
    if (((key == null) || key.isEmpty()))
        {
          throw new java.lang.IllegalArgumentException("The validated object is empty");
        }
    final String sanitizedKey = checkKey(key);
    this.dictionaryLock.readLock().lock();
    try 
      {
        return dictionary.get(sanitizedKey);
      }
    finally
      {
        this.dictionaryLock.readLock().unlock();
      }
  }
  
  private String checkKey(final String key) {
    return key;
  }
}