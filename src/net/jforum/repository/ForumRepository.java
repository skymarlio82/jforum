
package net.jforum.repository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import net.jforum.SessionFacade;
import net.jforum.cache.CacheEngine;
import net.jforum.cache.Cacheable;
import net.jforum.dao.CategoryDAO;
import net.jforum.dao.ConfigDAO;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.ForumDAO;
import net.jforum.dao.UserDAO;
import net.jforum.entities.Category;
import net.jforum.entities.Config;
import net.jforum.entities.Forum;
import net.jforum.entities.LastPostInfo;
import net.jforum.entities.MostUsersEverOnline;
import net.jforum.entities.Post;
import net.jforum.entities.Topic;
import net.jforum.entities.User;
import net.jforum.exceptions.CategoryNotFoundException;
import net.jforum.exceptions.DatabaseException;
import net.jforum.security.PermissionControl;
import net.jforum.security.SecurityConstants;
import net.jforum.util.CategoryOrderComparator;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

public class ForumRepository implements Cacheable {

	private static Logger logger = Logger.getLogger(ForumRepository.class);

	private static CacheEngine cache        = null;
	private static ForumRepository instance = null;

	private final static String FQN               = "forumRepository";
	private final static String CATEGORIES_SET    = "categoriesSet";
	private final static String RELATION          = "relationForums";
	private final static String FQN_MODERATORS    = FQN + "/moderators";
	private final static String TOTAL_MESSAGES    = "totalMessages";
	private final static String MOST_USERS_ONLINE = "mostUsersEverOnline";
	private final static String LOADED            = "loaded";
	private final static String LAST_USER         = "lastUser";
	private final static String TOTAL_USERS       = "totalUsers";

	public void setCacheEngine(CacheEngine engine) {
		cache = engine;
	}

	public synchronized static void start(ForumDAO fm, CategoryDAO cm, ConfigDAO configModel) {
		System.out.println("--> [ForumRepository.start] ......");
		instance = new ForumRepository();
		System.out.println("INFOR: the instance of 'ForumRepository' creation is done ...");
		if (cache.get(FQN, LOADED) == null) {
			System.out.println("INFOR: forumRepository is not loaded for system 1st time launched ...");
			instance.loadCategories(cm);
			instance.loadForums(fm);
			instance.loadMostUsersEverOnline(configModel);
			instance.loadUsersInfo();
			Integer i = (Integer)cache.get(FQN, TOTAL_MESSAGES);
			if (i == null) {
				cache.add(FQN, TOTAL_MESSAGES, new Integer(0));
			}
			cache.add(FQN, LOADED, "1");
		}
	}

	public static Category getCategory(int categoryId) {
		return getCategory(SessionFacade.getUserSession().getUserId(), categoryId);
	}

	public static Category getCategory(int userId, int categoryId) {
		if (!isCategoryAccessible(userId, categoryId)) {
			return null;
		}
		return (Category)cache.get(FQN, Integer.toString(categoryId));
	}

	public static Category getCategory(PermissionControl pc, int categoryId) {
		System.out.println("--> [ForumRepository.getCategory] ......");
		if (!isCategoryAccessible(pc, categoryId)) {
			System.out.println("DEBUG: the category (" + categoryId + ") is not included in the given object of 'pc'");
			return null;
		}
		return (Category)cache.get(FQN, Integer.toString(categoryId)); 
	}

	public static Category retrieveCategory(int categoryId) {
		return (Category)cache.get(FQN, Integer.toString(categoryId));
	}

	public static boolean isCategoryAccessible(int userId, int categoryId) {
		return isCategoryAccessible(SecurityRepository.get(userId), categoryId);
	}

	public static boolean isCategoryAccessible(int categoryId) {
		return isCategoryAccessible(SessionFacade.getUserSession().getUserId(), categoryId);
	}

	public static boolean isCategoryAccessible(PermissionControl pc, int categoryId) {
		System.out.println("--> [ForumRepository.isCategoryAccessible] ......");
		return pc.canAccess(SecurityConstants.PERM_CATEGORY, Integer.toString(categoryId));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List getAllCategories(int userId) {
		System.out.println("--> [ForumRepository.getAllCategories] ......");
		PermissionControl pc = SecurityRepository.get(userId);
		List l = new ArrayList();
		Set categoriesSet = (Set)cache.get(FQN, CATEGORIES_SET);
		if (categoriesSet == null) {
			synchronized (ForumRepository.instance) {
				if (categoriesSet == null) {
					logger.warn("Categories set returned null from the cache. Trying to reload");
					try {
						ForumRepository.instance.loadCategories(DataAccessDriver.getInstance().newCategoryDAO());
						ForumRepository.instance.loadForums(DataAccessDriver.getInstance().newForumDAO());
					} catch (Exception e) {
						throw new CategoryNotFoundException("Failed to get the category", e);
					}
					categoriesSet = (Set)cache.get(FQN, CATEGORIES_SET);
					if (categoriesSet == null) {
						throw new CategoryNotFoundException("Could not find all categories. There must be a problem with the cache");
					}
				}
			}
		}
		for (Iterator iter = categoriesSet.iterator(); iter.hasNext(); ) {
			Category c = getCategory(pc, ((Category)iter.next()).getId());
			if (c != null) {
				System.out.println("DEBUG: the fetched category (" + c.toString() + ") for the user '" + userId + "'");
				l.add(c);
			}
		}
		return l;
	}

	@SuppressWarnings("rawtypes")
	public static List getAllCategories() {
		return getAllCategories(SessionFacade.getUserSession().getUserId());
	}

	@SuppressWarnings("rawtypes")
	private static Category findCategoryByOrder(int order) {
		for (Iterator iter = ((Set)cache.get(FQN, CATEGORIES_SET)).iterator(); iter.hasNext(); ) {
			Category c = (Category)iter.next();
			if (c.getOrder() == order) {
				return c;
			}
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public synchronized static void reloadCategory(Category c) {
		Category current = (Category)cache.get(FQN, Integer.toString(c.getId()));
		Category currentAtOrder = findCategoryByOrder(c.getOrder());
		Set tmpSet = new TreeSet(new CategoryOrderComparator());
		tmpSet.addAll((Set)cache.get(FQN, CATEGORIES_SET));
		if (currentAtOrder != null) {
			tmpSet.remove(currentAtOrder);
			cache.remove(FQN, Integer.toString(currentAtOrder.getId()));
		}
		tmpSet.add(c);
		cache.add(FQN, Integer.toString(c.getId()), c);
		if (currentAtOrder != null && c.getId() != currentAtOrder.getId()) {
			tmpSet.remove(current);
			currentAtOrder.setOrder(current.getOrder());
			tmpSet.add(currentAtOrder);
			cache.add(FQN, Integer.toString(currentAtOrder.getId()), currentAtOrder);
		}
		cache.add(FQN, CATEGORIES_SET, tmpSet);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized static void refreshCategory(Category c) {
		cache.add(FQN, Integer.toString(c.getId()), c);
		Set s = (Set)cache.get(FQN, CATEGORIES_SET);
		s.remove(c);
		s.add(c);
		cache.add(FQN, CATEGORIES_SET, s);
	}

	public synchronized static void refreshForum(Forum forum) {
		Category c = retrieveCategory(forum.getCategoryId());
		c.addForum(forum);
		refreshCategory(c);
	}

	@SuppressWarnings("rawtypes")
	public synchronized static void removeCategory(Category c) {
		cache.remove(FQN, Integer.toString(c.getId()));
		Set s = (Set)cache.get(FQN, CATEGORIES_SET);
		s.remove(c);
		cache.add(FQN, CATEGORIES_SET, s);
		Map m = (Map)cache.get(FQN, RELATION);
		for (Iterator iter = m.values().iterator(); iter.hasNext(); ) {
			if (Integer.parseInt((String)iter.next()) == c.getId()) {
				iter.remove();
			}
		}
		cache.add(FQN, RELATION, m);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized static void addCategory(Category c) {
		String categoryId = Integer.toString(c.getId());
		cache.add(FQN, categoryId, c);
		Set s = (Set)cache.get(FQN, CATEGORIES_SET);
		if (s == null) {
			s = new TreeSet(new CategoryOrderComparator());
		}
		s.add(c);
		cache.add(FQN, CATEGORIES_SET, s);
		Map relation = (Map)cache.get(FQN, RELATION);
		if (relation == null) {
			relation = new HashMap();
		}
		for (Iterator iter = c.getForums().iterator(); iter.hasNext(); ) {
			Forum f = (Forum)iter.next();
			relation.put(Integer.toString(f.getId()), categoryId);
		}
		cache.add(FQN, RELATION, relation);
	}

	@SuppressWarnings("rawtypes")
	public static Forum getForum(int forumId) {
		System.out.println("--> [ForumRepository.getForum] ......");
		String categoryId = (String)((Map)cache.get(FQN, RELATION)).get(Integer.toString(forumId));
		if (categoryId != null) {
			Category category = (Category)cache.get(FQN, categoryId);
			if (isCategoryAccessible(category.getId())) {
				return category.getForum(forumId);
			}
		}
		return null;
	}

	public static boolean isForumAccessible(int forumId) {
		return isForumAccessible(SessionFacade.getUserSession().getUserId(), forumId);
	}

	@SuppressWarnings("rawtypes")
	public static boolean isForumAccessible(int userId, int forumId) {
		int categoryId = Integer.parseInt((String)((Map)cache.get(FQN, RELATION)).get(Integer.toString(forumId)));
		return isForumAccessible(userId, categoryId, forumId);
	}

	public static boolean isForumAccessible(int userId, int categoryId, int forumId) {
		return ((Category)cache.get(FQN, Integer.toString(categoryId))).getForum(userId, forumId) != null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized static void addForum(Forum forum) {
		String categoryId = Integer.toString(forum.getCategoryId());
		Category c = (Category)cache.get(FQN, categoryId);
		c.addForum(forum);
		cache.add(FQN, categoryId, c);
		Map m = (Map)cache.get(FQN, RELATION);
		m.put(Integer.toString(forum.getId()), categoryId);
		cache.add(FQN, RELATION, m);
		Set s = (Set)cache.get(FQN, CATEGORIES_SET);
		cache.add(FQN, CATEGORIES_SET, s);
	}

	@SuppressWarnings("rawtypes")
	public synchronized static void removeForum(Forum forum) {
		String id = Integer.toString(forum.getId());
		Map m = (Map)cache.get(FQN, RELATION);
		m.remove(id);
		cache.add(FQN, RELATION, m);
		id = Integer.toString(forum.getCategoryId());
		Category c = (Category)cache.get(FQN, id);
		c.removeForum(forum.getId());
		cache.add(FQN, id, c);
		Set s = (Set)cache.get(FQN, CATEGORIES_SET);
		cache.add(FQN, CATEGORIES_SET, s);
	}

	@SuppressWarnings("rawtypes")
	public static synchronized void reloadForum(int forumId) {
		Forum f = DataAccessDriver.getInstance().newForumDAO().selectById(forumId);
		if (((Map)cache.get(FQN, RELATION)).containsKey(Integer.toString(forumId))) {
			String id = Integer.toString(f.getCategoryId());
			Category c = (Category)cache.get(FQN, id);
			f.setLastPostInfo(null);
			f.setLastPostInfo(ForumRepository.getLastPostInfo(f));
			c.reloadForum(f);
			cache.add(FQN, id, c);
			Set s = (Set)cache.get(FQN, CATEGORIES_SET);
			cache.add(FQN, CATEGORIES_SET, s);
		}
		getTotalMessages(true);
	}

	@SuppressWarnings("rawtypes")
	public static synchronized void updateForumStats(Topic t, User u, Post p) {
		String f = Integer.toString(t.getForumId());
		if (((Map)cache.get(FQN, RELATION)).containsKey(f)) {
			Forum forum = getForum(t.getForumId());
			SimpleDateFormat df = new SimpleDateFormat(SystemGlobals.getValue(ConfigKeys.DATE_TIME_FORMAT));
			LastPostInfo lpi = forum.getLastPostInfo();
			if (lpi == null) {
				lpi = new LastPostInfo();
			}
			lpi.setPostId(p.getId());
			lpi.setPostDate(df.format(p.getTime()));
			lpi.setPostTimeMillis(p.getTime().getTime());
			lpi.setTopicId(t.getId());
			lpi.setTopicReplies(t.getTotalReplies());
			lpi.setUserId(u.getId());
			lpi.setUsername(u.getUsername());
			forum.setLastPostInfo(lpi);
			if (t.getTotalReplies() == 0) {
				forum.setTotalTopics(forum.getTotalTopics() + 1);
			}
			forum.setTotalPosts(forum.getTotalPosts() + 1);
			Category c = retrieveCategory(forum.getCategoryId());
			c.reloadForum(forum);
			refreshCategory(c);
		}
	}

	public static LastPostInfo getLastPostInfo(Forum forum) {
		LastPostInfo lpi = forum.getLastPostInfo();
		if (lpi == null || !forum.getLastPostInfo().hasInfo()) {
			lpi = DataAccessDriver.getInstance().newForumDAO().getLastPostInfo(forum.getId());
			forum.setLastPostInfo(lpi);
		}
		return lpi;
	}

	public static LastPostInfo getLastPostInfo(int forumId) {
		return getLastPostInfo(getForum(forumId));
	}

	@SuppressWarnings("rawtypes")
	public static List getModeratorList(int forumId) {
		List l = (List)cache.get(FQN_MODERATORS, Integer.toString(forumId));
		if (l == null) {
			synchronized (FQN_MODERATORS) {
				try {
					l = DataAccessDriver.getInstance().newForumDAO().getModeratorList(forumId);
					cache.add(FQN_MODERATORS, Integer.toString(forumId), l);
				} catch (Exception e) {
					throw new DatabaseException(e);
				}
			}
		}
		return l;
	}

	public static void clearModeratorList() {
		cache.remove(FQN_MODERATORS);
	}

	public static User lastRegisteredUser() {
		return (User)cache.get(FQN, LAST_USER);
	}

	public static void setLastRegisteredUser(User user) {
		cache.add(FQN, LAST_USER, user);
	}

	public static Integer totalUsers() {
		return (Integer)cache.get(FQN, TOTAL_USERS);
	}

	public static void incrementTotalUsers() {
		Integer i = (Integer)cache.get(FQN, TOTAL_USERS);
		if (i == null) {
			i = new Integer(0);
		}
		cache.add(FQN,TOTAL_USERS, new Integer(i.intValue() + 1));
	}

	public static int getTotalMessages() {
		return getTotalMessages(false);
	}

	public static int getTotalMessages(boolean fromDb) {
		Integer i = (Integer)cache.get(FQN, TOTAL_MESSAGES);
		int total = (i != null) ? i.intValue() : 0;
		if (fromDb || total == 0) {
			total = DataAccessDriver.getInstance().newForumDAO().getTotalMessages();
			cache.add(FQN, TOTAL_MESSAGES, new Integer(total));
		}
		return total;
	}
	
	public static synchronized void incrementTotalMessages() {
		int total = ((Integer)cache.get(FQN, TOTAL_MESSAGES)).intValue();
		cache.add(FQN, TOTAL_MESSAGES, new Integer(total + 1));
	}

	public static MostUsersEverOnline getMostUsersEverOnline() {
		System.out.println("--> [ForumRepository.getMostUsersEverOnline] ......");
		MostUsersEverOnline online = (MostUsersEverOnline)cache.get(FQN, MOST_USERS_ONLINE);
		if (online == null) {
			synchronized (MOST_USERS_ONLINE) {
				online = (MostUsersEverOnline)cache.get(FQN, MOST_USERS_ONLINE);
				if (online == null) {
					online = instance.loadMostUsersEverOnline(DataAccessDriver.getInstance().newConfigDAO());
				}
			}
		}
		return online;
	}

	public static void updateMostUsersEverOnline(MostUsersEverOnline m) {
		ConfigDAO cm = DataAccessDriver.getInstance().newConfigDAO();
		Config config = cm.selectByName(ConfigKeys.MOST_USERS_EVER_ONLINE);
		if (config == null) {
			// Total
			config = new Config();
			config.setName(ConfigKeys.MOST_USERS_EVER_ONLINE);
			config.setValue(Integer.toString(m.getTotal()));
			cm.insert(config);
			// Date
			config.setName(ConfigKeys.MOST_USER_EVER_ONLINE_DATE);
			config.setValue(Long.toString(m.getTimeInMillis()));
			cm.insert(config);
		} else {
			// Total
			config.setValue(Integer.toString(m.getTotal()));
			cm.update(config);
			// Date
			config.setName(ConfigKeys.MOST_USER_EVER_ONLINE_DATE);
			config.setValue(Long.toString(m.getTimeInMillis()));
			cm.update(config);
		}
		cache.add(FQN, MOST_USERS_ONLINE, m);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void loadForums(ForumDAO fm) {
		System.out.println("--> [ForumRepository.loadForums] ......");
		List l = fm.selectAll();
		Map m = (Map)cache.get(FQN, RELATION);
		if (m == null) {
			m = new HashMap();
		}
		int lastId = 0;
		Category c = null;
		String catId = null;
		for (Iterator iter = l.iterator(); iter.hasNext(); ) {
			Forum f = (Forum)iter.next();
			if (f.getCategoryId() != lastId) {
				if (c != null) {
					cache.add(FQN, catId, c);
					System.out.println("DEBUG: add category object 'c' : '" + catId + "' to cache.FQN.catId");
				}
				lastId = f.getCategoryId();
				catId = Integer.toString(f.getCategoryId());
				c = (Category)cache.get(FQN, catId);
			}
			if (c == null) {
				throw new CategoryNotFoundException("Category for forum #" + f.getId() + " not found");
			}
			String forumId = Integer.toString(f.getId());
			c.addForum(f);
			System.out.println("DEBUG: in category of '" + c.getName() + "' to add the forum of '" + f.getName() + "'");
			m.put(forumId, catId);
			System.out.println("DEBUG: mapping the forum '" + forumId + "' to category '" + catId + "'");
		}
		if (c != null) {
			cache.add(FQN, catId, c);
			System.out.println("DEBUG: add category object 'c' : '" + catId + "' to cache.FQN.catId at the loop outside");
		}
		cache.add(FQN, RELATION, m);
		System.out.println("DEBUG: add forum<->category mapper to cache.FQN.RELATION ");
	}

	private void loadUsersInfo() {
		System.out.println("--> [ForumRepository.loadUsersInfo] ......");
		UserDAO udao = DataAccessDriver.getInstance().newUserDAO();
		cache.add(FQN, LAST_USER, udao.getLastUserInfo());
		System.out.println("INFOR: add the LAST_USER to the cache ...");
		cache.add(FQN, TOTAL_USERS, new Integer(udao.getTotalUsers()));
		System.out.println("INFOR: add the TOTAL_USERS to the cache ...");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void loadCategories(CategoryDAO cm) {
		System.out.println("--> [ForumRepository.loadCategories] ......");
		List categories = cm.selectAll();
		Set categoriesSet = new TreeSet(new CategoryOrderComparator());
		for (Iterator iter = categories.iterator(); iter.hasNext(); ) {
			Category c = (Category)iter.next();
			cache.add(FQN, Integer.toString(c.getId()), c);
			categoriesSet.add(c);
			System.out.println("DEBUG: Category Name (" + c.getName() + ") is loaded into 'cache.FQN.categoryId'");
		}
		cache.add(FQN, CATEGORIES_SET, categoriesSet);
		System.out.println("INFOR: TreeSet of category is loaded into 'cache.FQN.categorySet'");
	}

	private MostUsersEverOnline loadMostUsersEverOnline(ConfigDAO cm) {
		System.out.println("--> [ForumRepository.loadMostUsersEverOnline] ......");
		Config config = cm.selectByName(ConfigKeys.MOST_USERS_EVER_ONLINE);
		MostUsersEverOnline mostUsersEverOnline = new MostUsersEverOnline();
		if (config != null) {
			mostUsersEverOnline.setTotal(Integer.parseInt(config.getValue()));
			// We're assuming that, if we have one key, the another one will always exist
			config = cm.selectByName(ConfigKeys.MOST_USER_EVER_ONLINE_DATE);
			mostUsersEverOnline.setTimeInMillis(Long.parseLong(config.getValue()));
		}
		cache.add(FQN, MOST_USERS_ONLINE, mostUsersEverOnline);
		return mostUsersEverOnline;
	}

	@SuppressWarnings("rawtypes")
	public static String getListAllowedForums() {
		int n = 0;
		StringBuffer buf = new StringBuffer();
		List allCategories = ForumRepository.getAllCategories();
		for (Iterator iter = allCategories.iterator(); iter.hasNext(); ) {
			Collection forums = ((Category)iter.next()).getForums();
			for (Iterator tmpIterator = forums.iterator(); tmpIterator.hasNext(); ) {
				Forum f = (Forum)tmpIterator.next();
				if (ForumRepository.isForumAccessible(f.getId())) {
					if (n++ > 0) {
						buf.append(',');
					}
					buf.append(f.getId());
				}
			}
		}
		if (n <= 0) {
			return "-1";
		}
		return buf.toString();
	}
}
