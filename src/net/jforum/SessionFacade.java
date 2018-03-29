
package net.jforum;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.jforum.cache.CacheEngine;
import net.jforum.cache.Cacheable;
import net.jforum.dao.DataAccessDriver;
import net.jforum.entities.UserSession;
import net.jforum.repository.SecurityRepository;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

public class SessionFacade implements Cacheable {

	private static final Logger logger = Logger.getLogger(SessionFacade.class);

	private static final String FQN             = "sessions";
	private static final String FQN_LOGGED      = FQN + "/logged";
	private static final String FQN_COUNT       = FQN + "/count";
	private static final String FQN_USER_ID     = FQN + "/userId";
	private static final String ANONYMOUS_COUNT = "anonymousCount";
	private static final String LOGGED_COUNT    = "loggedCount";

	private static CacheEngine cache = null;

	public void setCacheEngine(CacheEngine engine) {
		cache = engine;
	}

	public static void add(UserSession us) {
		add(us, JForumExecutionContext.getRequest().getSessionContext().getId());
	}

	public static void add(UserSession us, String sessionId) {
		System.out.println("--> [SessionFacade.add] ......");
		if (us.getSessionId() == null || us.getSessionId().equals("")) {
			us.setSessionId(sessionId);
		}
		synchronized (FQN) {
			cache.add(FQN, us.getSessionId(), us);
			if (!JForumExecutionContext.getForumContext().isBot()) {
				if (us.getUserId() != SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID)) {
					changeUserCount(LOGGED_COUNT, true);
					cache.add(FQN_LOGGED, us.getSessionId(), us);
					cache.add(FQN_USER_ID, Integer.toString(us.getUserId()), us.getSessionId());
				} else {
					changeUserCount(ANONYMOUS_COUNT, true);
				}
			}
		}
	}

	private static void changeUserCount(String cacheEntryName, boolean increment) {
		System.out.println("--> [SessionFacade.changeUserCount] ......");
		Integer count = (Integer)cache.get(FQN_COUNT, cacheEntryName);
		if (count == null) {
			count = new Integer(0);
		}
		if (increment) {
			count = new Integer(count.intValue() + 1);
		} else if (count.intValue() > 0) {
			count = new Integer(count.intValue() - 1);
		}
		cache.add(FQN_COUNT, cacheEntryName, count);
		System.out.println("DEBUG: set the value of cache - FQN_COUNT - " + cacheEntryName + " for " + count);
	}

	public static void setAttribute(String name, Object value) {
		JForumExecutionContext.getRequest().getSessionContext().setAttribute(name, value);
	}

	public static Object getAttribute(String name) {
		return JForumExecutionContext.getRequest().getSessionContext().getAttribute(name);
	}

	public static void removeAttribute(String name) {
		JForumExecutionContext.getRequest().getSessionContext().removeAttribute(name);
	}

	public static void remove(String sessionId) {
		System.out.println("--> [SessionFacade.remove] ......");
		if (cache == null) {
			logger.warn("Got a null cache instance. #" + sessionId);
			return;
		}
		logger.debug("Removing session " + sessionId);
		synchronized (FQN) {
			UserSession us = getUserSession(sessionId);
			if (us != null) {
				cache.remove(FQN_LOGGED, sessionId);
				System.out.println("DEBUG: remove the sessionId - " + sessionId + " from cache - FQN_LOGGED");
				cache.remove(FQN_USER_ID, Integer.toString(us.getUserId()));
				System.out.println("DEBUG: remove the userId - " + Integer.toString(us.getUserId()) + " from cache - FQN_USER_ID");
				if (us.getUserId() != SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID)) {
					System.out.println("INFOR: decrease FQN_COUNT - LOGGED_COUNT");
					changeUserCount(LOGGED_COUNT, false);
				} else {
					System.out.println("INFOR: decrease FQN_COUNT - ANONYMOUS_COUNT");
					changeUserCount(ANONYMOUS_COUNT, false);
				}
			}
			cache.remove(FQN, sessionId);
			System.out.println("DEBUG: remove the sessionId - " + sessionId + " from cache - FQN");
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List getAllSessions() {
		synchronized (FQN) {
			return new ArrayList(cache.getValues(FQN));
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List getLoggedSessions() {
		synchronized (FQN) {
			return new ArrayList(cache.getValues(FQN_LOGGED));
		}
	}

	public static int registeredSize() {
		Integer count = (Integer)cache.get(FQN_COUNT, LOGGED_COUNT);
		return (count == null) ? 0 : count.intValue();
	}

	public static int anonymousSize() {
		Integer count = (Integer)cache.get(FQN_COUNT, ANONYMOUS_COUNT);
		return (count == null) ? 0 : count.intValue();
	}

	@SuppressWarnings("rawtypes")
	public static void clear() {
		synchronized (FQN) {
			cache.add(FQN, new HashMap());
			cache.add(FQN_COUNT, LOGGED_COUNT, new Integer(0));
			cache.add(FQN_COUNT, ANONYMOUS_COUNT, new Integer(0));
			cache.remove(FQN_LOGGED);
			cache.remove(FQN_USER_ID);
		}
	}

	public static UserSession getUserSession() {
		return getUserSession(JForumExecutionContext.getRequest().getSessionContext().getId());
	}

	public static UserSession getUserSession(String sessionId) {
		if (cache != null) {
			UserSession us = (UserSession)cache.get(FQN, sessionId);
			return us;
		}
		logger.warn("Got a null cache in getUserSession. #" + sessionId);
		return null;
	}

	public static int size() {
		return anonymousSize() + registeredSize();
	}

	@SuppressWarnings("rawtypes")
	public static String isUserInSession(String username) {
		int aid = SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID);
		synchronized (FQN) {
			for (Iterator iter = cache.getValues(FQN).iterator(); iter.hasNext(); ) {
				UserSession us = (UserSession)iter.next();
				String thisUsername = us.getUsername();
				if (thisUsername == null) {
					continue;
				}
				if (us.getUserId() != aid && thisUsername.equals(username)) {
					return us.getSessionId();
				}
			}
		}
		return null;
	}

	public static String isUserInSession(int userId) {
		System.out.println("--> [SessionFacade.isUserInSession] ......");
		return (String)cache.get(FQN_USER_ID, Integer.toString(userId));
	}

	public static boolean isLogged() {
		return "1".equals(SessionFacade.getAttribute(ConfigKeys.LOGGED));
	}

	public static void makeLogged() {
		SessionFacade.setAttribute(ConfigKeys.LOGGED, "1");
	}

	public static void makeUnlogged() {
		System.out.println("--> [SessionFacade.makeUnlogged] ......");
		SessionFacade.removeAttribute(ConfigKeys.LOGGED);
	}

	@SuppressWarnings("rawtypes")
	public static Map getTopicsReadTime() {
		Map tracking = (Map)getAttribute(ConfigKeys.TOPICS_READ_TIME);
		if (tracking == null) {
			tracking = new HashMap();
			setAttribute(ConfigKeys.TOPICS_READ_TIME, tracking);
		}
		return tracking;
	}

	@SuppressWarnings("rawtypes")
	public static Map getTopicsReadTimeByForum() {
		return (Map)getAttribute(ConfigKeys.TOPICS_READ_TIME_BY_FORUM);
	}

	public static void storeSessionData(String sessionId) {
		Connection conn = null;
		try {
			conn = DBConnection.getImplementation().getConnection();
			SessionFacade.storeSessionData(sessionId, conn);
		} finally {
			if (conn != null) {
				try {
					DBConnection.getImplementation().releaseConnection(conn);
				} catch (Exception e) {
					logger.warn("Error while releasing a connection: " + e);
				}
			}
		}
	}

	public static void storeSessionData(String sessionId, Connection conn) {
		System.out.println("--> [SessionFacade.storeSessionData] ......");
		UserSession us = SessionFacade.getUserSession(sessionId);
		if (us != null) {
			try {
				if (us.getUserId() != SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID)) {
					DataAccessDriver.getInstance().newUserSessionDAO().update(us, conn);
				}
				SecurityRepository.remove(us.getUserId());
			} catch (Exception e) {
				logger.warn("Error storing user session data: " + e, e);
			}
		}
	}
}
