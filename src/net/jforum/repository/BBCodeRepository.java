
package net.jforum.repository;

import net.jforum.cache.CacheEngine;
import net.jforum.cache.Cacheable;
import net.jforum.util.bbcode.BBCode;
import net.jforum.util.bbcode.BBCodeHandler;

public class BBCodeRepository implements Cacheable {

	private static CacheEngine cache = null;

	private final static String FQN          = "bbcode";
	private final static String BBCOLLECTION = "bbCollection";

	public void setCacheEngine(CacheEngine cacheEngine) {
		cache = cacheEngine;
	}

	public static void setBBCollection(BBCodeHandler bbCollection) {
		System.out.println("--> BBCodeRepository.setBBCollection (BBCOLLECTION => BBCodeHandler)");
		cache.add(FQN, BBCOLLECTION, bbCollection);
	}

	public static BBCodeHandler getBBCollection() {
		return (BBCodeHandler)cache.get(FQN, BBCOLLECTION);
	}

	public static BBCode findByName(String tagName) {
		return getBBCollection().findByName(tagName);
	}
}
