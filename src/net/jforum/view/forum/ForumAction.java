
package net.jforum.view.forum;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.jforum.Command;
import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.ForumDAO;
import net.jforum.dao.ModerationDAO;
import net.jforum.entities.Forum;
import net.jforum.entities.MostUsersEverOnline;
import net.jforum.entities.UserSession;
import net.jforum.repository.ForumRepository;
import net.jforum.repository.SecurityRepository;
import net.jforum.security.SecurityConstants;
import net.jforum.util.I18n;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import net.jforum.view.admin.ModerationAction;
import net.jforum.view.forum.common.ForumCommon;
import net.jforum.view.forum.common.PostCommon;
import net.jforum.view.forum.common.TopicsCommon;
import net.jforum.view.forum.common.ViewCommon;

public class ForumAction extends Command {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void list() {
		System.out.println("--> [ForumAction.list] ......");
		super.setTemplateName(TemplateKeys.FORUMS_LIST);
		context.put("allCategories", ForumCommon.getAllCategoriesAndForums(true));
		context.put("topicsPerPage", new Integer(SystemGlobals.getIntValue(ConfigKeys.TOPICS_PER_PAGE)));
		context.put("rssEnabled", SystemGlobals.getBoolValue(ConfigKeys.RSS_ENABLED));
		context.put("totalMessages", new Integer(ForumRepository.getTotalMessages()));
		context.put("totalRegisteredUsers", ForumRepository.totalUsers());
		context.put("lastUser", ForumRepository.lastRegisteredUser());
		SimpleDateFormat df = new SimpleDateFormat(SystemGlobals.getValue(ConfigKeys.DATE_TIME_FORMAT));
		GregorianCalendar gc = new GregorianCalendar();
		context.put("now", df.format(gc.getTime()));
		context.put("lastVisit", df.format(SessionFacade.getUserSession().getLastVisit()));
		context.put("forumRepository", new ForumRepository());
		// Online Users
		context.put("totalOnlineUsers", new Integer(SessionFacade.size()));
		int aid = SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID);
		List onlineUsersList = SessionFacade.getLoggedSessions();
		// Check for an optional language parameter
		UserSession currentUser = SessionFacade.getUserSession();
		if (currentUser.getUserId() == aid) {
			String lang = request.getParameter("lang");
			if (lang != null && I18n.languageExists(lang)) {
				currentUser.setLang(lang);
			}
		}
		// If there are only guest users, then just register a single one. In any other situation, we do not show the "guest" username
		if (onlineUsersList.size() == 0) {
			UserSession us = new UserSession();
			us.setUserId(aid);
			us.setUsername(I18n.getMessage("Guest"));
			onlineUsersList.add(us);
		}
		int registeredSize = SessionFacade.registeredSize();
		int anonymousSize = SessionFacade.anonymousSize();
		int totalOnlineUsers = registeredSize + anonymousSize;
		context.put("userSessions", onlineUsersList);
		context.put("totalOnlineUsers", new Integer(totalOnlineUsers));
		context.put("totalRegisteredOnlineUsers", new Integer(registeredSize));
		context.put("totalAnonymousUsers", new Integer(anonymousSize));
		// Most users ever online
		MostUsersEverOnline mostUsersEverOnline = ForumRepository.getMostUsersEverOnline();
		if (totalOnlineUsers > mostUsersEverOnline.getTotal()) {
			mostUsersEverOnline.setTotal(totalOnlineUsers);
			mostUsersEverOnline.setTimeInMillis(System.currentTimeMillis());
			ForumRepository.updateMostUsersEverOnline(mostUsersEverOnline);
		}
		context.put("mostUsersEverOnline", mostUsersEverOnline);
	}

	public void moderation() {
		context.put("openModeration", true);
		show();
	}

	@SuppressWarnings("rawtypes")
	public void show() {
		System.out.println("--> [ForumAction.show] ......");
		int forumId = request.getIntParameter("forum_id");
		ForumDAO fm = DataAccessDriver.getInstance().newForumDAO();
		// The user can access this forum?
		Forum forum = ForumRepository.getForum(forumId);
		if (forum == null || !ForumRepository.isCategoryAccessible(forum.getCategoryId())) {
			new ModerationHelper().denied(I18n.getMessage("ForumListing.denied"));
			return;
		}
		int start = ViewCommon.getStartPage();
		List tmpTopics = TopicsCommon.topicsByForum(forumId, start);
		super.setTemplateName(TemplateKeys.FORUMS_SHOW);
		// Moderation
		UserSession userSession = SessionFacade.getUserSession();
		boolean isLogged = SessionFacade.isLogged();
		boolean isModerator = userSession.isModerator(forumId);
		boolean canApproveMessages = (isLogged && isModerator && SecurityRepository.canAccess(SecurityConstants.PERM_MODERATION_APPROVE_MESSAGES));
		Map topicsToApprove = new HashMap();
		if (canApproveMessages) {
			ModerationDAO mdao = DataAccessDriver.getInstance().newModerationDAO();
			topicsToApprove = mdao.topicsByForum(forumId);
			context.put("postFormatter", new PostCommon());
		}
		context.put("topicsToApprove", topicsToApprove);
		context.put("attachmentsEnabled", SecurityRepository.canAccess(SecurityConstants.PERM_ATTACHMENTS_ENABLED, Integer.toString(forumId)) || SecurityRepository.canAccess(SecurityConstants.PERM_ATTACHMENTS_DOWNLOAD));
		context.put("topics", TopicsCommon.prepareTopics(tmpTopics));
		context.put("allCategories", ForumCommon.getAllCategoriesAndForums(false));
		context.put("forum", forum);
		context.put("rssEnabled", SystemGlobals.getBoolValue(ConfigKeys.RSS_ENABLED));
		context.put("pageTitle", forum.getName());
		context.put("canApproveMessages", canApproveMessages);
		context.put("replyOnly", !SecurityRepository.canAccess(SecurityConstants.PERM_REPLY_ONLY, Integer.toString(forum.getId())));
		context.put("readonly", !SecurityRepository.canAccess(SecurityConstants.PERM_READ_ONLY_FORUMS, Integer.toString(forumId)));
		context.put("watching", fm.isUserSubscribed(forumId, userSession.getUserId()));
		// Pagination
		int topicsPerPage = SystemGlobals.getIntValue(ConfigKeys.TOPICS_PER_PAGE);
		int postsPerPage = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);
		int totalTopics = forum.getTotalTopics();
		ViewCommon.contextToPagination(start, totalTopics, topicsPerPage);
		context.put("postsPerPage", new Integer(postsPerPage));
		TopicsCommon.topicListingBase();
		context.put("moderator", isLogged && isModerator);
	}

	// Make an URL to some action
	private String makeRedirect(String action) {
		String path = request.getContextPath() + "/forums/" + action + "/";
		String thisPage = request.getParameter("start");
		if (thisPage != null && !thisPage.equals("0")) {
			path += thisPage + "/";
		}
		String forumId = request.getParameter("forum_id");
		if (forumId == null) {
			forumId = request.getParameter("persistData");
		}
		path += forumId + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION);
		return path;
	}

	// Mark all topics as read
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void readAll() {
		String forumId = request.getParameter("forum_id");
		if (forumId != null) {
			Map tracking = SessionFacade.getTopicsReadTimeByForum();
			if (tracking == null) {
				tracking = new HashMap();
			}
			tracking.put(new Integer(forumId), new Long(System.currentTimeMillis()));
			SessionFacade.setAttribute(ConfigKeys.TOPICS_READ_TIME_BY_FORUM, tracking);
		}
		if (forumId != null) {
			JForumExecutionContext.setRedirect(makeRedirect("show"));
		} else {
			JForumExecutionContext.setRedirect(request.getContextPath() + "/forums/list" + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
		}
	}

	// Messages since last visit
	public void newMessages() {
		request.addParameter("from_date", SessionFacade.getUserSession().getLastVisit());
		request.addParameter("to_date", new Date());
		SearchAction searchAction = new SearchAction(request, response, context);
		searchAction.newMessages();
		setTemplateName(TemplateKeys.SEARCH_NEW_MESSAGES);
	}

	public void approveMessages() {
		if (SessionFacade.getUserSession().isModerator(request.getIntParameter("forum_id"))) {
			new ModerationAction(context, request).doSave();
		}
		JForumExecutionContext.setRedirect(request.getContextPath() + "/forums/show/" + request.getParameter("forum_id") + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION));
	}

	public void watchForum() {
		int forumId = request.getIntParameter("forum_id");
		int userId = SessionFacade.getUserSession().getUserId();
		watchForum(DataAccessDriver.getInstance().newForumDAO(), forumId, userId);
		JForumExecutionContext.setRedirect(redirectLinkToShowAction(forumId));
	}

	public void banned() {
		setTemplateName(TemplateKeys.FORUMS_BANNED);
		context.put("message", I18n.getMessage("ForumBanned.banned"));
	}

	private String redirectLinkToShowAction(int forumId) {
		int start = ViewCommon.getStartPage();
		return request.getContextPath() + "/forums/show/" + (start > 0 ? start + "/" : "") + forumId + SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION);
	}

	private void watchForum(ForumDAO dao, int forumId, int userId) {
		if (SessionFacade.isLogged() && !dao.isUserSubscribed(forumId, userId)) {
			dao.subscribeUser(forumId, userId);
		}
	}

	public void unwatchForum() {
		if (SessionFacade.isLogged()) {
			int forumId = request.getIntParameter("forum_id");
			int userId = SessionFacade.getUserSession().getUserId();
			DataAccessDriver.getInstance().newForumDAO().removeSubscription(forumId, userId);
			String returnPath = this.redirectLinkToShowAction(forumId);
			setTemplateName(TemplateKeys.POSTS_UNWATCH);
			context.put("message", I18n.getMessage("ForumBase.forumUnwatched", new String[] { returnPath }));
		} else {
			setTemplateName(ViewCommon.contextToLogin());
		}
	}
}
