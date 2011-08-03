import java.util.Map;
import java.util.HashMap;
class LockPlain {
	private Map<String, String> dictionary = new HashMap<String, String>();
	
	@java.lang.SuppressWarnings("all")
	public void put(final String key, final String value) {
		if (key == null || key.isEmpty()) {
			throw new java.lang.IllegalArgumentException("The validated object is empty");
		}
		final String sanitizedKey = checkKey(key);
		this.dictionaryLock.writeLock().lock();
		try {
			dictionary.put(sanitizedKey, value);
		} finally {
			this.dictionaryLock.writeLock().unlock();
		}
	}
	
	@java.lang.SuppressWarnings("all")
	public String get(final String key) {
		if (key == null || key.isEmpty()) {
			throw new java.lang.IllegalArgumentException("The validated object is empty");
		}
		final String sanitizedKey = checkKey(key);
		this.dictionaryLock.readLock().lock();
		try {
			return dictionary.get(sanitizedKey);
		} finally {
			this.dictionaryLock.readLock().unlock();
		}
	}
	
	private String checkKey(final String key) {
		// do something;
		return key;
	}
	
	private final java.util.concurrent.locks.ReadWriteLock dictionaryLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
}