
package net.jforum.repository;

import java.util.Iterator;
import java.util.List;

import net.jforum.cache.CacheEngine;
import net.jforum.cache.Cacheable;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.RankingDAO;
import net.jforum.entities.Ranking;
import net.jforum.exceptions.RankingLoadException;

public class RankingRepository implements Cacheable {

	private final static String FQN     = "ranking";
	private final static String ENTRIES = "entries";

	private static CacheEngine cache = null;

	public void setCacheEngine(CacheEngine engine) {
		cache = engine;
	}

	public static void loadRanks() {
		System.out.println("--> [RankingRepository.loadRanks] ......");
		try {
			RankingDAO rm = DataAccessDriver.getInstance().newRankingDAO();
			cache.add(FQN, ENTRIES, rm.selectAll());
		} catch (Exception e) {
			throw new RankingLoadException("Error while loading the rankings: " + e);
		}
	}

	@SuppressWarnings("rawtypes")
	public static int size() {
		return ((List)cache.get(FQN, ENTRIES)).size();
	}

	public static String getRankTitle(int rankId, int total) {
		String title = null;
		if (rankId > 0) {
			title = getRankTitleById(rankId);
		}
		if (title == null) {
			title = getRankTitleByPosts(total);
		}
		return title;
	}

	@SuppressWarnings("rawtypes")
	private static String getRankTitleByPosts(int total) {
		Ranking lastRank = new Ranking();
		List entries = (List)cache.get(FQN, ENTRIES);
		for (Iterator iter = entries.iterator(); iter.hasNext(); ) {
			Ranking r = (Ranking) iter.next();
			if (total == r.getMin() && !r.isSpecial()) {
				return r.getTitle();
			} else if (total > lastRank.getMin() && total < r.getMin()) {
				return lastRank.getTitle();
			}
			lastRank = r;
		}
		return lastRank.getTitle();
	}

	@SuppressWarnings("rawtypes")
	private static String getRankTitleById(int rankId) {
		Ranking r = new Ranking();
		r.setId(rankId);
		List l = (List)cache.get(FQN, ENTRIES);
		int index = l.indexOf(r);
		return (index > -1) ? ((Ranking)l.get(index)).getTitle() : null;
	}
}
