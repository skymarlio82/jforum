
package net.jforum.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DefaultCacheEngine implements CacheEngine {

	@SuppressWarnings("rawtypes")
	private Map cache = new HashMap();

	@SuppressWarnings("unchecked")
	public void add(String key, Object value) {
		cache.put(key, value);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void add(String fqn, String key, Object value) {
		Map m = (Map)cache.get(fqn);
		if (m == null) {
			m = new HashMap();
		}
		m.put(key, value);
		cache.put(fqn, m);
	}

	@SuppressWarnings("rawtypes")
	public Object get(String fqn, String key) {
		Map m = (Map)cache.get(fqn);
		if (m == null) {
			return null;
		}
		return m.get(key);
	}

	public Object get(String fqn) {
		return cache.get(fqn);
	}

	@SuppressWarnings("rawtypes")
	public Collection getValues(String fqn) {
		Map m = (Map)cache.get(fqn);
		if (m == null) {
			return new ArrayList();
		}
		return m.values();
	}

	@SuppressWarnings("rawtypes")
	public void init() {
		cache = new HashMap();
	}

	public void stop() {

	}

	@SuppressWarnings("rawtypes")
	public void remove(String fqn, String key) {
		Map m = (Map)cache.get(fqn);
		if (m != null) {
			m.remove(key);
		}
	}

	public void remove(String fqn) {
		cache.remove(fqn);
	}
}
