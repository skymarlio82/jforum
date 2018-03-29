
package net.jforum.cache;

import java.util.Collection;

public interface CacheEngine {

	public final static String DUMMY_FQN    = "";
	public final static String NOTIFICATION = "notification";

	public void init();

	public void stop();

	public void add(String key, Object value);

	public void add(String fqn, String key, Object value);

	public Object get(String fqn, String key);

	public Object get(String fqn);

	@SuppressWarnings("rawtypes")
	public Collection getValues(String fqn);

	public void remove(String fqn, String key);

	public void remove(String fqn);
}
