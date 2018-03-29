
package net.jforum.repository;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.jforum.cache.CacheEngine;
import net.jforum.cache.Cacheable;
import net.jforum.dao.BanlistDAO;
import net.jforum.dao.DataAccessDriver;
import net.jforum.entities.Banlist;

public class BanlistRepository implements Cacheable {

	private final static String FQN     = "banlist";
	private final static String BANLIST = "banlistCollection";

	private static CacheEngine cache = null;

	public void setCacheEngine(CacheEngine engine) {
		cache = engine;
	}

	@SuppressWarnings("rawtypes")
	public static boolean shouldBan(Banlist b) {
		System.out.println("--> [BanlistRepository.loadBanlist] ......");
		boolean status = false;
		for (Iterator iter = banlist().values().iterator(); iter.hasNext(); ) {
			Banlist current = (Banlist)iter.next();
			if (current.matches(b)) {
				status = true;
				break;
			}
		}
		return status;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void add(Banlist b) {
		Map m = banlist();
		m.put(new Integer(b.getId()), b);
		cache.add(FQN, BANLIST, m);
	}

	@SuppressWarnings("rawtypes")
	public static void remove(int banlistId) {
		Map m = banlist();
		Integer key = new Integer(banlistId);
		if (m.containsKey(key)) {
			m.remove(key);
		}
		cache.add(FQN, BANLIST, m);
	}

	@SuppressWarnings("rawtypes")
	private static Map banlist() {
		Map m = (Map)cache.get(FQN, BANLIST);
		if (m == null) {
			m = new HashMap();
		}
		return m;
	}

	@SuppressWarnings("rawtypes")
	public static void loadBanlist() {
		System.out.println("--> [BanlistRepository.loadBanlist] ......");
		BanlistDAO dao = DataAccessDriver.getInstance().newBanlistDAO();
		List banlist = dao.selectAll();
		for (Iterator iter = banlist.iterator(); iter.hasNext(); ) {
			BanlistRepository.add((Banlist) iter.next());
		}
		System.out.println("INFOR: Add each ban to the BanlistRepository ...");
	}
}
