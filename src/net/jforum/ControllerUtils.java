
package net.jforum;

import java.util.Date;

import javax.servlet.http.Cookie;

import net.jforum.context.ForumContext;
import net.jforum.context.RequestContext;
import net.jforum.context.SessionContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.UserDAO;
import net.jforum.dao.UserSessionDAO;
import net.jforum.entities.User;
import net.jforum.entities.UserSession;
import net.jforum.exceptions.DatabaseException;
import net.jforum.exceptions.ForumException;
import net.jforum.repository.SecurityRepository;
import net.jforum.security.SecurityConstants;
import net.jforum.sso.SSO;
import net.jforum.sso.SSOUtils;
import net.jforum.util.I18n;
import net.jforum.util.MD5;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import freemarker.template.SimpleHash;

public class ControllerUtils {

	public void prepareTemplateContext(SimpleHash context, ForumContext jforumContext) {
		System.out.println("--> [ControllerUtils.prepareTemplateContext] ......");
		RequestContext request = JForumExecutionContext.getRequest();
		context.put("karmaEnabled", SecurityRepository.canAccess(SecurityConstants.PERM_KARMA_ENABLED));
		context.put("dateTimeFormat", SystemGlobals.getValue(ConfigKeys.DATE_TIME_FORMAT));
		context.put("autoLoginEnabled", SystemGlobals.getBoolValue(ConfigKeys.AUTO_LOGIN_ENABLED));
		context.put("sso", ConfigKeys.TYPE_SSO.equals(SystemGlobals.getValue(ConfigKeys.AUTHENTICATION_TYPE)));
		context.put("contextPath", request.getContextPath());
		context.put("serverName", request.getServerName());
		context.put("templateName", SystemGlobals.getValue(ConfigKeys.TEMPLATE_DIR));
		context.put("extension", SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
		context.put("serverPort", Integer.toString(request.getServerPort()));
		context.put("I18n", I18n.getInstance());
		context.put("version", SystemGlobals.getValue(ConfigKeys.VERSION));
		context.put("forumTitle", SystemGlobals.getValue(ConfigKeys.FORUM_PAGE_TITLE));
		context.put("pageTitle", SystemGlobals.getValue(ConfigKeys.FORUM_PAGE_TITLE));
		context.put("metaKeywords", SystemGlobals.getValue(ConfigKeys.FORUM_PAGE_METATAG_KEYWORDS));
		context.put("metaDescription", SystemGlobals.getValue(ConfigKeys.FORUM_PAGE_METATAG_DESCRIPTION));
		context.put("forumLink", SystemGlobals.getValue(ConfigKeys.FORUM_LINK));
		context.put("homepageLink", SystemGlobals.getValue(ConfigKeys.HOMEPAGE_LINK));
		context.put("encoding", SystemGlobals.getValue(ConfigKeys.ENCODING));
		context.put("bookmarksEnabled", SecurityRepository.canAccess(SecurityConstants.PERM_BOOKMARKS_ENABLED));
		context.put("canAccessModerationLog", SecurityRepository.canAccess(SecurityConstants.PERM_MODERATION_LOG));
		context.put("JForumContext", jforumContext);
		context.put("timestamp", new Long(System.currentTimeMillis()));
	}

	protected boolean checkAutoLogin(UserSession userSession) {
		System.out.println("--> [ControllerUtils.checkAutoLogin] ......");
		String cookieName = SystemGlobals.getValue(ConfigKeys.COOKIE_NAME_DATA);
		System.out.println("DEBUG: cookieName = " + cookieName);
		Cookie cookie = getCookieTemplate(cookieName);
		System.out.println("DEBUG: cookie object => " + ((cookie == null) ? "NULL" : cookie.getValue()));
		String cookieUserHash = SystemGlobals.getValue(ConfigKeys.COOKIE_USER_HASH);
		System.out.println("DEBUG: cookieUserHash = " + cookieUserHash);
		Cookie hashCookie = getCookieTemplate(cookieUserHash);
		System.out.println("DEBUG: hashCookie object => " + ((hashCookie == null) ? "NULL" : hashCookie.getValue()));
		String cookieAutoLogin = SystemGlobals.getValue(ConfigKeys.COOKIE_AUTO_LOGIN);
		System.out.println("DEBUG: cookieAutoLogin = " + cookieAutoLogin);
		Cookie autoLoginCookie = getCookieTemplate(cookieAutoLogin);
		System.out.println("DEBUG: autoLoginCookie object => " + ((autoLoginCookie == null) ? "NULL" : autoLoginCookie.getValue()));
		if (hashCookie != null && cookie != null && !cookie.getValue().equals(SystemGlobals.getValue(ConfigKeys.ANONYMOUS_USER_ID)) && autoLoginCookie != null && "1".equals(autoLoginCookie.getValue())) {
			String uid = cookie.getValue();
			System.out.println("DEBUG: uid = " + uid);
			String uidHash = hashCookie.getValue();
			System.out.println("DEBUG: uidHash = " + uidHash);
			// Load the user-specific security hash from the database
			try {
				UserDAO userDao = DataAccessDriver.getInstance().newUserDAO();
				String userHash = userDao.getUserAuthHash(Integer.parseInt(uid));
				System.out.println("DEBUG: userHash = " + userHash);
				if (userHash == null || userHash.trim().length() == 0) {
					return false;
				}
				String securityHash = MD5.crypt(userHash);
				System.out.println("DEBUG: securityHash = " + securityHash);
				if (securityHash.equals(uidHash)) {
					int userId = Integer.parseInt(uid);
					userSession.setUserId(userId);
					User user = userDao.selectById(userId);
					if (user == null || user.getId() != userId || user.isDeleted()) {
						userSession.makeAnonymous();
						return false;
					}
					configureUserSession(userSession, user);
					return true;
				}
			} catch (Exception e) {
				throw new DatabaseException(e);
			}
			userSession.makeAnonymous();
		}
		return false;
	}

	protected void configureUserSession(UserSession userSession, User user) {
		System.out.println("--> [ControllerUtils.configureUserSession] ......");
		userSession.dataToUser(user);
		// As an user may come back to the forum before its last visit's session expires, we should check for existent user information and then, if found, store it to the database before getting his information back.
		String sessionId = SessionFacade.isUserInSession(user.getId());
		UserSession tmpUs = null;
		if (sessionId != null) {
			SessionFacade.storeSessionData(sessionId, JForumExecutionContext.getConnection());
			tmpUs = SessionFacade.getUserSession(sessionId);
			SessionFacade.remove(sessionId);
		} else {
			UserSessionDAO sm = DataAccessDriver.getInstance().newUserSessionDAO();
			tmpUs = sm.selectById(userSession, JForumExecutionContext.getConnection());
		}
		if (tmpUs == null) {
			userSession.setLastVisit(new Date(System.currentTimeMillis()));
		} else {
			// Update last visit and session start time
			userSession.setLastVisit(new Date(tmpUs.getStartTime().getTime() + tmpUs.getSessionTime()));
		}
		System.out.println("DEBUG: update the userSession lastVisitDatetime as " + userSession.getLastVisit());
		// If the execution point gets here, then the user has chosen "autoLogin"
		userSession.setAutoLogin(true);
		SessionFacade.makeLogged();
		I18n.load(user.getLang());
		System.out.println("DEBUG: re-load the I18n for user's language: " + user.getLang());
	}

	protected void checkSSO(UserSession userSession) {
		System.out.println("--> [ControllerUtils.checkSSO] ......");
		try {
			SSO sso = (SSO)Class.forName(SystemGlobals.getValue(ConfigKeys.SSO_IMPLEMENTATION)).newInstance();
			String username = sso.authenticateUser(JForumExecutionContext.getRequest());
			if (username == null || username.trim().equals("")) {
				userSession.makeAnonymous();
			} else {
				SSOUtils utils = new SSOUtils();
				if (!utils.userExists(username)) {
					SessionContext session = JForumExecutionContext.getRequest().getSessionContext();
					String email = (String) session.getAttribute(SystemGlobals.getValue(ConfigKeys.SSO_EMAIL_ATTRIBUTE));
					String password = (String) session.getAttribute(SystemGlobals.getValue(ConfigKeys.SSO_PASSWORD_ATTRIBUTE));
					if (email == null) {
						email = SystemGlobals.getValue(ConfigKeys.SSO_DEFAULT_EMAIL);
					}
					if (password == null) {
						password = SystemGlobals.getValue(ConfigKeys.SSO_DEFAULT_PASSWORD);
					}
					utils.register(password, email);
				}
				this.configureUserSession(userSession, utils.getUser());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ForumException("Error while executing SSO actions: " + e);
		}
	}

	public void refreshSession() {
		System.out.println("--> [ControllerUtils.refreshSession] ......");
		UserSession userSession = SessionFacade.getUserSession();
		RequestContext request = JForumExecutionContext.getRequest();
		if (userSession == null) {
			System.out.println("INFOR: userSession is NULL ...");
			userSession = new UserSession();
			userSession.registerBasicInfo();
			userSession.setSessionId(request.getSessionContext().getId());
			System.out.println("DEBUG: set the value of 'sessionId' in the 'userSession' as (" + userSession.getSessionId() + ")");
			userSession.setIp(request.getRemoteAddr());
			System.out.println("DEBUG: set the value of 'ip' in the 'userSession' as (" + userSession.getIp() + ")");
			SessionFacade.makeUnlogged();
			System.out.println("INFOR: remove the flag of 'LOGGED' in SessionContext ...");
			System.out.println("DEBUG: JForumExecutionContext.forumContext.bot = " + JForumExecutionContext.getForumContext().isBot());
			if (!JForumExecutionContext.getForumContext().isBot()) {
				// Non-SSO authentications can use auto login
				System.out.println("DEBUG: SystemGlobals.AUTHENTICATION_TYPE = " + SystemGlobals.getValue(ConfigKeys.AUTHENTICATION_TYPE));
				if (!ConfigKeys.TYPE_SSO.equals(SystemGlobals.getValue(ConfigKeys.AUTHENTICATION_TYPE))) {
					System.out.println("DEBUG: SystemGlobals.AUTO_LOGIN_ENABLED = " + SystemGlobals.getBoolValue(ConfigKeys.AUTO_LOGIN_ENABLED));
					if (SystemGlobals.getBoolValue(ConfigKeys.AUTO_LOGIN_ENABLED)) {
						checkAutoLogin(userSession);
					} else {
						userSession.makeAnonymous();
					}
				} else {
					checkSSO(userSession);
				}
			}
			SessionFacade.add(userSession);
		} else if (ConfigKeys.TYPE_SSO.equals(SystemGlobals.getValue(ConfigKeys.AUTHENTICATION_TYPE))) {
			System.out.println("INFOR: the website is protected by SSO now ...");
			SSO sso = null;
			try {
				sso = (SSO)Class.forName(SystemGlobals.getValue(ConfigKeys.SSO_IMPLEMENTATION)).newInstance();
			} catch (Exception e) {
				throw new ForumException(e);
			}
			// If SSO, then check if the session is valid
			if (!sso.isSessionValid(userSession, request)) {
				SessionFacade.remove(userSession.getSessionId());
				refreshSession();
			}
		} else {
			System.out.println("INFOR: updating the session time ...");
			SessionFacade.getUserSession().updateSessionTime();
		}
	}

	public static Cookie getCookie(String name) {
		Cookie[] cookies = JForumExecutionContext.getRequest().getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie c = cookies[i];
				if (c.getName().equals(name)) {
					return c;
				}
			}
		}
		return null;
	}

	protected Cookie getCookieTemplate(String name) {
		return ControllerUtils.getCookie(name);
	}

	public static void addCookie(String name, String value) {
		int maxAge = 3600*24*365;
		if (value == null) {
			maxAge = 0;
			value = "";
		}
		Cookie cookie = new Cookie(name, value);
		cookie.setMaxAge(maxAge);
		cookie.setPath("/");
		JForumExecutionContext.getResponse().addCookie(cookie);
	}

	protected void addCookieTemplate(String name, String value) {
		ControllerUtils.addCookie(name, value);
	}
}
