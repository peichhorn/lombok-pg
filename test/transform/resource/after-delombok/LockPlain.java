import java.util.Map;
import java.util.HashMap;
class LockPlain {
	private Map<String, String> dictionary = new HashMap<String, String>();
	
	@java.lang.SuppressWarnings("all")
	public void put(String key, String value) {
		this.dictionaryLock.writeLock().lock();
		try {
			dictionary.put(key, value);
		} finally {
			this.dictionaryLock.writeLock().unlock();
		}
	}
	
	@java.lang.SuppressWarnings("all")
	public String get(String key) {
		this.dictionaryLock.readLock().lock();
		try {
			return dictionary.get(key);
		} finally {
			this.dictionaryLock.readLock().unlock();
		}
	}
	
	private final java.util.concurrent.locks.ReadWriteLock dictionaryLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
}