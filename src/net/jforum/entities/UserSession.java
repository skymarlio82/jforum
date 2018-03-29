
package net.jforum.entities;

import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Date;

import net.jforum.ControllerUtils;
import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.repository.SecurityRepository;
import net.jforum.security.PermissionControl;
import net.jforum.security.SecurityConstants;
import net.jforum.util.Captcha;
import net.jforum.util.I18n;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import com.octo.captcha.image.ImageCaptcha;

@SuppressWarnings("serial")
public class UserSession implements Serializable {

	private long sessionTime = 0;
	private int userId = 0;
	private int privateMessages = 0;
	private Date startTime = null;
	private Date lastVisit = null;
	private String sessionId = null;
	private String username = null;
	private String lang = null;
	private String ip = null;

	private boolean autoLogin = false;

	private ImageCaptcha imageCaptcha = null;

	public UserSession() {

	}

	public UserSession(UserSession us) {
		if (us.getStartTime() != null) {
			startTime = new Date(us.getStartTime().getTime());
		}
		if (us.getLastVisit() != null) {
			lastVisit = new Date(us.getLastVisit().getTime());
		}
		sessionTime = us.getSessionTime();
		userId = us.getUserId();
		sessionId = us.getSessionId();
		username = us.getUsername();
		autoLogin = us.getAutoLogin();
		lang = us.getLang();
		privateMessages = us.getPrivateMessages();
		imageCaptcha = us.imageCaptcha;
		ip = us.getIp();
	}

	public Date sessionLastUpdate() {
		return new Date(startTime.getTime() + sessionTime);
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getIp() {
		return ip;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public int getPrivateMessages() {
		return privateMessages;
	}

	public void setPrivateMessages(int privateMessages) {
		this.privateMessages = privateMessages;
	}

	public void setLastVisit(Date lastVisit) {
		this.lastVisit = lastVisit;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public void setSessionTime(long sessionTime) {
		this.sessionTime = sessionTime;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public void updateSessionTime() {
		System.out.println("--> [UserSession.updateSessionTime] ......");
		sessionTime = System.currentTimeMillis() - startTime.getTime();
		System.out.println("DEBUG: new sessionTime = " + sessionTime);
	}

	public void setAutoLogin(boolean autoLogin) {
		this.autoLogin = autoLogin;
	}

	public Date getStartTime() {
		return startTime;
	}

	public String getLang() {
		return lang;
	}

	public Date getLastVisit() {
		// return new GregorianCalendar(2007, 6, 28, 15, 15, 19).getTime();
		return lastVisit;
	}

	public long getSessionTime() {
		return sessionTime;
	}

	public int getUserId() {
		return userId;
	}

	public String getUsername() {
		if (username == null && userId == SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID)) {
			username = I18n.getMessage("Guest");
		}
		return username;
	}

	public boolean getAutoLogin() {
		return autoLogin;
	}

	public String getSessionId() {
		return sessionId;
	}

	public boolean isAdmin() {
		return SecurityRepository.canAccess(userId, SecurityConstants.PERM_ADMINISTRATION);
	}

	public boolean isModerator() {
		return SecurityRepository.canAccess(userId, SecurityConstants.PERM_MODERATION);
	}

	public boolean isModerator(int forumId) {
		PermissionControl pc = SecurityRepository.get(userId);
		return pc.canAccess(SecurityConstants.PERM_MODERATION) && pc.canAccess(SecurityConstants.PERM_MODERATION_FORUMS, Integer.toString(forumId));
	}

	public void makeAnonymous() {
		System.out.println("--> [UserSession.makeAnonymous] ......");
		registerBasicInfo();
		ControllerUtils.addCookie(SystemGlobals.getValue(ConfigKeys.COOKIE_AUTO_LOGIN), null);
		System.out.println("INFOR: set the 'COOKIE_AUTO_LOGIN' in the cookie as NULL ...");
		ControllerUtils.addCookie(SystemGlobals.getValue(ConfigKeys.COOKIE_NAME_DATA), SystemGlobals.getValue(ConfigKeys.ANONYMOUS_USER_ID));
		System.out.println("INFOR: set the 'COOKIE_NAME_DATA' in the cookie as 'ANONYMOUS_USER_ID' ...");
		SessionFacade.makeUnlogged();
	}

	public void registerBasicInfo() {
		System.out.println("--> [UserSession.registerBasicInfo] ......");
		setStartTime(new Date(System.currentTimeMillis()));
		setLastVisit(new Date(System.currentTimeMillis()));
		setUserId(SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID));
		System.out.println("DEBUG: update the values of startTime (" + startTime + "), lastVisitTime (" + lastVisit + ") and userId (" + userId + ")");
	}

	public void dataToUser(User user) {
		System.out.println("--> [UserSession.dataToUser] ......");
		setUserId(user.getId());
		setUsername(user.getUsername());
		setPrivateMessages(user.getPrivateMessagesCount());
		setStartTime(new Date(System.currentTimeMillis()));
		setLang(user.getLang());
	}

	public BufferedImage getCaptchaImage() {
		if (imageCaptcha == null) {
			return null;
		}
		return (BufferedImage)imageCaptcha.getChallenge();
	}

	public boolean validateCaptchaResponse(String userResponse) {
		if ((SystemGlobals.getBoolValue(ConfigKeys.CAPTCHA_REGISTRATION) || SystemGlobals.getBoolValue(ConfigKeys.CAPTCHA_POSTS)) && imageCaptcha != null) {
			if (SystemGlobals.getBoolValue(ConfigKeys.CAPTCHA_IGNORE_CASE)) {
				userResponse = userResponse.toLowerCase();
			}
			boolean result = imageCaptcha.validateResponse(userResponse).booleanValue();
			destroyCaptcha();
			return result;
		}
		return true;
	}

	public void createNewCaptcha() {
		destroyCaptcha();
		imageCaptcha = Captcha.getInstance().getNextImageCaptcha();
	}

	public void destroyCaptcha() {
		imageCaptcha = null;
	}

	public boolean isBot() {
		// return Boolean.TRUE.equals(JForumExecutionContext.getRequest().getAttribute(ConfigKeys.IS_BOT));
		return JForumExecutionContext.getForumContext().isBot();
	}

	public boolean equals(Object o) {
		if (!(o instanceof UserSession)) {
			return false;
		}
		return sessionId.equals(((UserSession)o).getSessionId());
	}

	public int hashCode() {
		return sessionId.hashCode();
	}
}
