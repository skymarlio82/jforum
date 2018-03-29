
package net.jforum.repository;

import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.cache.CacheEngine;
import net.jforum.cache.Cacheable;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.GroupSecurityDAO;
import net.jforum.dao.UserDAO;
import net.jforum.entities.User;
import net.jforum.entities.UserSession;
import net.jforum.exceptions.SecurityLoadException;
import net.jforum.security.PermissionControl;

import org.apache.log4j.Logger;

public class SecurityRepository implements Cacheable {

	private final static Logger logger = Logger.getLogger(SecurityRepository.class);

	private final static String FQN = "security";

	private static CacheEngine cache = null;

	public void setCacheEngine(CacheEngine engine) {
		cache = engine;
	}

	public static PermissionControl load(int userId, boolean force) {
		System.out.println("--> [SecurityRepository.load] ......");
		System.out.println("DEBUG: userId = " + userId + ", force = " + force + ", cache.FQN.userId = " + cache.get(FQN, Integer.toString(userId)));
		if (force || cache.get(FQN, Integer.toString(userId)) == null) {
			UserDAO um = DataAccessDriver.getInstance().newUserDAO();
			return SecurityRepository.load(um.selectById(userId), force);
		}
		return SecurityRepository.get(userId);
	}

	public static PermissionControl load(int userId) {
		System.out.println("--> [SecurityRepository.load] ......");
		System.out.println("DEBUG: userId = " + userId);
		return SecurityRepository.load(userId, false);
	}

	public static PermissionControl load(User user) {
		return SecurityRepository.load(user, false);
	}

	public static PermissionControl load(User user, boolean force) {
		System.out.println("--> [SecurityRepository.load] ......");
		String userId = Integer.toString(user.getId());
		System.out.println("DEBUG: userId = " + userId + ", force = " + force + ", cache.FQN.userId = " + cache.get(FQN, userId));
		if (force || cache.get(FQN, userId) == null) {
			PermissionControl pc = new PermissionControl();
			// load roles
			GroupSecurityDAO dao = DataAccessDriver.getInstance().newGroupSecurityDAO();
			pc.setRoles(dao.loadRolesByUserGroups(user));
			cache.add(FQN, userId, pc);
			return pc;
		}
		return SecurityRepository.get(user.getId());
	}

	public static boolean canAccess(String roleName) {
		System.out.println("--> [SecurityRepository.canAccess] ......");
		System.out.println("DEBUG: roleName = " + roleName);
		return canAccess(roleName, null);
	}

	public static boolean canAccess(int userId, String roleName) {
		return canAccess(userId, roleName, null);
	}

	public static boolean canAccess(String roleName, String value) {
		System.out.println("--> [SecurityRepository.canAccess] ......");
		System.out.println("DEBUG: roleName = " + roleName + ", value = " + value);
		UserSession us = SessionFacade.getUserSession();
		if (us == null) {
			logger.warn("Found null userSession. Going anonymous. Session id #" + JForumExecutionContext.getRequest().getSessionContext().getId());
			us = new UserSession();
			us.makeAnonymous();
		}
		return canAccess(us.getUserId(), roleName, value);
	}

	public static boolean canAccess(int userId, String roleName, String value) {
		System.out.println("--> [SecurityRepository.canAccess] ......");
		System.out.println("DEBUG: userId = " + userId + ", roleName = " + roleName + ", value = " + value);
		PermissionControl pc = SecurityRepository.get(userId);
		if (pc == null) {
			throw new SecurityLoadException("Failed to load security roles for userId " + userId + " (null PermissionControl returned). " + "roleName=" + roleName + ", roleValue=" + value);
		}
		return (value != null) ? pc.canAccess(roleName, value) : pc.canAccess(roleName);
	}

	public static PermissionControl get(int userId) {
		System.out.println("--> [SecurityRepository.get] ......");
		PermissionControl pc = (PermissionControl)cache.get(FQN, Integer.toString(userId));
		if (pc == null) {
			System.out.println("INFOR: the object of 'pc' is empty ...");
			System.out.println("DEBUG: need to reload the user (" + userId + ")'s Permissions ...");
			try {
				pc = load(userId);
			} catch (Exception e) {
				throw new SecurityLoadException(e);
			}
		}
		return pc;
	}

	public static synchronized void add(int userId, PermissionControl pc) {
		cache.add(FQN, Integer.toString(userId), pc);
	}

	public static synchronized void remove(int userId) {
		cache.remove(FQN, Integer.toString(userId));
	}

	public static synchronized void clean() {
		cache.remove(FQN);
	}
}
