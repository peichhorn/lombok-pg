import java.util.Map;
import java.util.HashMap;
class LockPlain {
	private Map<String, String> dictionary = new HashMap<String, String>();
	
	@lombok.WriteLock("dictionaryLock")
	public void put(String key, String value) {
		dictionary.put(key, value);
	}
	
	@lombok.ReadLock("dictionaryLock")
	public String get(String key) {
		return dictionary.get(key);
	}
}