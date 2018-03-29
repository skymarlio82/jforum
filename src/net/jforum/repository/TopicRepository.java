
package net.jforum.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.jforum.cache.CacheEngine;
import net.jforum.cache.Cacheable;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.TopicDAO;
import net.jforum.entities.Topic;
import net.jforum.entities.TopicTypeComparator;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

public class TopicRepository implements Cacheable {

	private static int maxItems = SystemGlobals.getIntValue(ConfigKeys.TOPICS_PER_PAGE);

	private final static String FQN                 = "topics";
	private final static String RECENT              = "recent";
	private final static String HOTTEST             = "hottest";
	private final static String FQN_FORUM           = FQN + "/byforum";
	private final static String RELATION            = "relation";
	private final static String FQN_LOADED          = FQN + "/loaded";
	@SuppressWarnings("rawtypes")
	private final static Comparator TYPE_COMPARATOR = new TopicTypeComparator();

	private static CacheEngine cache = null;

	public void setCacheEngine(CacheEngine engine) {
		cache = engine;
	}

	public static boolean isLoaded(int forumId) {
		return "1".equals(cache.get(FQN_LOADED, Integer.toString(forumId)));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized static void pushTopic(Topic topic) {
		if (SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			int limit = SystemGlobals.getIntValue(ConfigKeys.RECENT_TOPICS);
			LinkedList l = (LinkedList)cache.get(FQN, RECENT);
			if (l == null || l.size() == 0) {
				l = new LinkedList(loadMostRecentTopics());
			}
			l.remove(topic);
			l.addFirst(topic);
			while (l.size() > limit) {
				l.removeLast();
			}
			cache.add(FQN, RECENT, l);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List getRecentTopics() {
		List l = (List)cache.get(FQN, RECENT);
		if (l == null || l.size() == 0 || !SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			l = loadMostRecentTopics();
		}
		return new ArrayList(l);
	}	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List getHottestTopics() {
		List l = (List) cache.get(FQN, HOTTEST);
		if (l == null || l.size() == 0 || !SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			l = loadHottestTopics();
		}
		return new ArrayList(l);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized static List loadMostRecentTopics() {
		TopicDAO tm = DataAccessDriver.getInstance().newTopicDAO();
		int limit = SystemGlobals.getIntValue(ConfigKeys.RECENT_TOPICS);
		List l = tm.selectRecentTopics(limit);
		cache.add(FQN, RECENT, new LinkedList(l));
		return l;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized static List loadHottestTopics() {
	    TopicDAO tm = DataAccessDriver.getInstance().newTopicDAO();
	    int limit = SystemGlobals.getIntValue(ConfigKeys.HOTTEST_TOPICS);
	    List l = tm.selectHottestTopics(limit);
	    cache.add(FQN, HOTTEST, new LinkedList(l));
	    return l;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void addAll(int forumId, List topics) {
		if (SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			synchronized (FQN_FORUM) {
				cache.add(FQN_FORUM, Integer.toString(forumId), new LinkedList(topics));
				Map m = (Map)cache.get(FQN, RELATION);
				if (m == null) {
					m = new HashMap();
				}
				Integer fId = new Integer(forumId);
				for (Iterator iter = topics.iterator(); iter.hasNext(); ) {
					Topic t = (Topic)iter.next();
					m.put(new Integer(t.getId()), fId);
				}
				cache.add(FQN, RELATION, m);
				cache.add(FQN_LOADED, Integer.toString(forumId), "1");
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static void clearCache(int forumId) {
		synchronized (FQN_FORUM) {
			cache.add(FQN_FORUM, Integer.toString(forumId), new LinkedList());
			cache.remove(FQN, RELATION);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void addTopic(Topic topic) {
		if (!SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			return;
		}
		synchronized (FQN_FORUM) {
			String forumId = Integer.toString(topic.getForumId());
			LinkedList list = (LinkedList)cache.get(FQN_FORUM, forumId);
			if (list == null) {
				list = new LinkedList();
				list.add(topic);
			} else {
				boolean contains = list.contains(topic);
				// If the cache is full, remove the eldest element
				if (!contains && list.size() + 1 > maxItems) {
					list.removeLast();
				} else if (contains) {
					list.remove(topic);
				}
				list.add(topic);
				Collections.sort(list, TYPE_COMPARATOR);
			}
			cache.add(FQN_FORUM, forumId, list);
			Map m = (Map) cache.get(FQN, RELATION);
			if (m == null) {
				m = new HashMap();
			}
			m.put(new Integer(topic.getId()), new Integer(forumId));
			cache.add(FQN, RELATION, m);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void updateTopic(Topic topic) {
		if (SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			synchronized (FQN_FORUM) {
				String forumId = Integer.toString(topic.getForumId());
				List l = (List)cache.get(FQN_FORUM, forumId);
				if (l != null) {
					int index = l.indexOf(topic);
					if (index > -1) {
						l.set(index, topic);
						cache.add(FQN_FORUM, forumId, l);
					}
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static Topic getTopic(Topic t) {
		if (!SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			return null;
		}
		if (t.getForumId() == 0) {
			Map m = (Map) cache.get(FQN, RELATION);
			if (m != null) {
				Integer forumId = (Integer) m.get(new Integer(t.getId()));
				if (forumId != null) {
					t.setForumId(forumId.intValue());
				}
			}
			if (t.getForumId() == 0) {
				return null;
			}
		}
		List l = (List) cache.get(FQN_FORUM, Integer.toString(t.getForumId()));
		int index = -1;
		if (l != null) {
			index = l.indexOf(t);
		}
		return (index == -1) ? null : (Topic) (l.get(index));
	}

	@SuppressWarnings("rawtypes")
	public static boolean isTopicCached(Topic topic) {
		if (!SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			return false;
		}
		String forumId = Integer.toString(topic.getForumId());
		List list = (List) cache.get(FQN_FORUM, forumId);
		return list == null ? false : list.contains(topic);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List getTopics(int forumid) {
		if (SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			synchronized (FQN_FORUM) {
				List returnList = (List)cache.get(FQN_FORUM, Integer.toString(forumid));
				if (returnList == null) {
					return new ArrayList();
				}
				return new ArrayList(returnList);
			}
		}
		return new ArrayList();
	}
}
