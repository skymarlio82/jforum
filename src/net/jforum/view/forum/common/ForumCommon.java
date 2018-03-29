
package net.jforum.view.forum.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.jforum.SessionFacade;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.ForumDAO;
import net.jforum.entities.Category;
import net.jforum.entities.Forum;
import net.jforum.entities.LastPostInfo;
import net.jforum.entities.Post;
import net.jforum.entities.Topic;
import net.jforum.entities.UserSession;
import net.jforum.repository.ForumRepository;
import net.jforum.util.concurrent.Executor;
import net.jforum.util.mail.EmailSenderTask;
import net.jforum.util.mail.ForumNewTopicSpammer;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

public class ForumCommon {

	private static Logger logger = Logger.getLogger(ForumCommon.class);

	@SuppressWarnings("rawtypes")
	public static void checkUnreadPosts(Forum forum, Map tracking, long lastVisit) {
		LastPostInfo lpi = forum.getLastPostInfo();
		if (lpi == null) {
			return;
		}
		Integer topicId = new Integer(lpi.getTopicId());
		if (tracking != null && tracking.containsKey(topicId)) {
			long readTime = ((Long) tracking.get(topicId)).longValue();
			forum.setUnread(readTime > 0 && lpi.getPostTimeMillis() > readTime);
		} else {
			forum.setUnread(lpi.getPostTimeMillis() > lastVisit);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List getAllCategoriesAndForums(UserSession us, int anonymousUserId, Map tracking, boolean checkUnreadPosts) {
		System.out.println("--> [ForumCommon.getAllCategoriesAndForums] ......");
		long lastVisit = 0;
		int userId = anonymousUserId;
		if (us != null) {
			lastVisit = us.getLastVisit().getTime();
			userId = us.getUserId();
		}
        // Do not check for unread posts if the user is not logged in
		checkUnreadPosts = checkUnreadPosts && (userId != anonymousUserId);
		System.out.println("DEBUG: the userId = " + userId + ", anonymousUserId = " + anonymousUserId + ", checkUnreadPosts = " + checkUnreadPosts + ", lastVisit = " + lastVisit);
		List categories = ForumRepository.getAllCategories(userId);
		if (!checkUnreadPosts) {
			return categories;
		}
		List returnCategories = new ArrayList();
		for (Iterator iter = categories.iterator(); iter.hasNext(); ) {
			Category c = new Category((Category)iter.next());
			for (Iterator tmpIterator = c.getForums().iterator(); tmpIterator.hasNext(); ) {
				Forum f = (Forum)tmpIterator.next();
				ForumCommon.checkUnreadPosts(f, tracking, lastVisit);
			}
			returnCategories.add(c);
		}
		return returnCategories;
	}

	@SuppressWarnings("rawtypes")
	public static List getAllCategoriesAndForums(boolean checkUnreadPosts) {
		return getAllCategoriesAndForums(SessionFacade.getUserSession(), SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID), SessionFacade.getTopicsReadTime(), checkUnreadPosts);
	}

	@SuppressWarnings("rawtypes")
	public static List getAllCategoriesAndForums() {
		UserSession us = SessionFacade.getUserSession();
		boolean checkUnread = (us != null && us.getUserId() != SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID));
		return getAllCategoriesAndForums(checkUnread);
	}

	@SuppressWarnings("rawtypes")
	public static void notifyUsers(Forum f, Topic t, Post post) {
		if (SystemGlobals.getBoolValue(ConfigKeys.MAIL_NOTIFY_ANSWERS)) {
			try {
				ForumDAO dao = DataAccessDriver.getInstance().newForumDAO();
				List usersToNotify = dao.notifyUsers(f);
				// we only have to send an email if there are users subscribed to the topic
				if (usersToNotify != null && usersToNotify.size() > 0) {
					Executor.execute(new EmailSenderTask(new ForumNewTopicSpammer(f, t, post, usersToNotify)));
				}
			} catch (Exception e) {
				logger.warn("Error while sending notification emails: " + e);
			}
		}
	}
}
