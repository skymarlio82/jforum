
package net.jforum.search;

import java.util.ArrayList;

import net.jforum.entities.Post;
import net.jforum.exceptions.SearchInstantiationException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

public class SearchFacade {

	private static Logger logger = Logger.getLogger(SearchFacade.class);

	private static SearchManager searchManager = null;

	public static void init() {
		System.out.println("--> [SearchFacade.init] ......");
		if (!isSearchEnabled()) {
			logger.info("Search indexing is disabled. Will try to create a SearchManager instance for runtime configuration changes");
		}
		String clazz = SystemGlobals.getValue(ConfigKeys.SEARCH_INDEXER_IMPLEMENTATION);
		System.out.println("DEBUG: ConfigKeys.SEARCH_INDEXER_IMPLEMENTATION = " + clazz);
		if (clazz == null || "".equals(clazz)) {
			logger.info(ConfigKeys.SEARCH_INDEXER_IMPLEMENTATION + " is not defined. Skipping.");
		} else {
			try {
				searchManager = (SearchManager)Class.forName(clazz).newInstance();
				System.out.println("INFOR: instantiating SearchManager is done ...");
			} catch (Exception e) {
				logger.warn(e.toString(), e);
				throw new SearchInstantiationException("Error while tring to start the search manager: " + e);
			}
			searchManager.init();
			System.out.println("INFOR: initializing SearchManager is done ...");
		}
	}

	public static void create(Post post) {
		System.out.println("--> [SearchFacade.create] ......");
		if (isSearchEnabled()) {
			searchManager.create(post);
		}
	}

	public static void update(Post post) {
		if (isSearchEnabled()) {
			searchManager.update(post);
		}
	}

	@SuppressWarnings("rawtypes")
	public static SearchResult search(SearchArgs args) {
		System.out.println("--> [SearchFacade.search] ......");
		return isSearchEnabled() ? searchManager.search(args) : new SearchResult(new ArrayList(), 0);
	}

	private static boolean isSearchEnabled() {
		return SystemGlobals.getBoolValue(ConfigKeys.SEARCH_INDEXING_ENABLED);
	}

	public static void delete(Post p) {
		if (isSearchEnabled()) {
			searchManager.delete(p);
		}
	}

	public static SearchManager manager() {
		return searchManager;
	}
}
