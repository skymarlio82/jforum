
package net.jforum.repository;

import java.util.Iterator;
import java.util.List;

import net.jforum.cache.CacheEngine;
import net.jforum.cache.Cacheable;
import net.jforum.dao.DataAccessDriver;
import net.jforum.entities.Smilie;
import net.jforum.exceptions.SmiliesLoadException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

public class SmiliesRepository implements Cacheable {

	private static CacheEngine cache = null;

	private final static String FQN     = "smilies";
	private final static String ENTRIES = "entries";

	private static boolean contexted = false;

	public void setCacheEngine(CacheEngine engine) {
		cache = engine;
	}

	public static void loadSmilies() {
		System.out.println("--> [SmiliesRepository.loadSmilies] ......");
		try {
			cache.add(FQN, ENTRIES, DataAccessDriver.getInstance().newSmilieDAO().selectAll());
			contexted = false;
		} catch (Exception e) {
			throw new SmiliesLoadException("Error while loading smilies: " + e);
		}
	}

	@SuppressWarnings("rawtypes")
	public static List getSmilies() {
		List list = (List)cache.get(FQN, ENTRIES);
		if (!contexted) {
			String forumLink = SystemGlobals.getValue(ConfigKeys.FORUM_LINK);
			for (Iterator iter = list.iterator(); iter.hasNext(); ) {
				Smilie s = (Smilie)iter.next();
				s.setUrl(s.getUrl().replaceAll("#CONTEXT#", forumLink).replaceAll("\\\\", ""));
			}
			cache.add(FQN, ENTRIES, list);
			contexted = true;
		}
		return list;
	}
}
