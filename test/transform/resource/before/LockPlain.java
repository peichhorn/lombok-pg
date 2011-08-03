import java.util.Map;
import java.util.HashMap;
class LockPlain {
	private Map<String, String> dictionary = new HashMap<String, String>();
	
	@lombok.WriteLock("dictionaryLock")
	public void put(final @lombok.Validate.NotEmpty @lombok.Sanitize.With("checkKey") String key, final String value) {
		dictionary.put(key, value);
	}
	
	@lombok.ReadLock("dictionaryLock")
	public String get(final @lombok.Validate.NotEmpty @lombok.Sanitize.With("checkKey") String key) {
		return dictionary.get(key);
	}
	
	private String checkKey(final String key) {
		// do something;
		return key;
	}
}