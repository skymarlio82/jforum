
package net.jforum.view.forum.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.ForumDAO;
import net.jforum.dao.TopicDAO;
import net.jforum.entities.Forum;
import net.jforum.entities.Post;
import net.jforum.entities.Topic;
import net.jforum.entities.UserSession;
import net.jforum.repository.ForumRepository;
import net.jforum.repository.SecurityRepository;
import net.jforum.repository.TopicRepository;
import net.jforum.security.PermissionControl;
import net.jforum.security.SecurityConstants;
import net.jforum.util.I18n;
import net.jforum.util.concurrent.Executor;
import net.jforum.util.mail.EmailSenderTask;
import net.jforum.util.mail.TopicReplySpammer;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.view.forum.ModerationHelper;
import freemarker.template.SimpleHash;

public class TopicsCommon {

	private static final Object MUTEXT = new Object();

	@SuppressWarnings("rawtypes")
	public static List topicsByForum(int forumId, int start) {
		TopicDAO tm = DataAccessDriver.getInstance().newTopicDAO();
		int topicsPerPage = SystemGlobals.getIntValue(ConfigKeys.TOPICS_PER_PAGE);
		List topics = null;
		// Try to get the first's page of topics from the cache
		if (start == 0 && SystemGlobals.getBoolValue(ConfigKeys.TOPIC_CACHE_ENABLED)) {
			topics = TopicRepository.getTopics(forumId);
			if (topics.size() == 0 || !TopicRepository.isLoaded(forumId)) {
				synchronized (MUTEXT) {
					if (topics.size() == 0 || !TopicRepository.isLoaded(forumId)) {
						topics = tm.selectAllByForumByLimit(forumId, start, topicsPerPage);
						TopicRepository.addAll(forumId, topics);
					}
				}
			}
		} else {
			topics = tm.selectAllByForumByLimit(forumId, start, topicsPerPage);
		}
		return topics;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List prepareTopics(List topics) {
		UserSession userSession = SessionFacade.getUserSession();
		long lastVisit = userSession.getLastVisit().getTime();
		int hotBegin = SystemGlobals.getIntValue(ConfigKeys.HOT_TOPIC_BEGIN);
		int postsPerPage = SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE);
		List newTopics = new ArrayList(topics.size());
		Map topicsReadTime = SessionFacade.getTopicsReadTime();
		Map topicReadTimeByForum = SessionFacade.getTopicsReadTimeByForum();
		boolean checkUnread = (userSession.getUserId() != SystemGlobals.getIntValue(ConfigKeys.ANONYMOUS_USER_ID));
		for (Iterator iter = topics.iterator(); iter.hasNext(); ) {
			Topic t = (Topic)iter.next();
			boolean read = false;
			boolean isReadByForum = false;
			long lastPostTime = t.getLastPostDate().getTime();
			if (topicReadTimeByForum != null) {
				Long currentForumTime = (Long)topicReadTimeByForum.get(new Integer(t.getForumId()));
				isReadByForum = currentForumTime != null && lastPostTime < currentForumTime.longValue();
			}
			boolean isTopicTimeOlder = !isReadByForum && lastPostTime <= lastVisit;
			if (!checkUnread || isReadByForum || isTopicTimeOlder) {
				read = true;
			} else {
				Integer topicId = new Integer(t.getId());
				Long currentTopicTime = (Long)topicsReadTime.get(topicId);
				if (currentTopicTime != null) {
					read = currentTopicTime.longValue() > lastPostTime;
				}
			}
			if (t.getTotalReplies() + 1 > postsPerPage) {
				t.setPaginate(true);
				t.setTotalPages(new Double(Math.floor(t.getTotalReplies() / postsPerPage)));
			} else {
				t.setPaginate(false);
				t.setTotalPages(new Double(0));
			}
			// Check if this is a hot topic
			t.setHot(t.getTotalReplies() >= hotBegin);
			t.setRead(read);
			newTopics.add(t);
		}
		return newTopics;
	}

	public static void topicListingBase() {
		SimpleHash context = JForumExecutionContext.getTemplateContext();
		// Topic Types
		context.put("TOPIC_ANNOUNCE", new Integer(Topic.TYPE_ANNOUNCE));
		context.put("TOPIC_STICKY", new Integer(Topic.TYPE_STICKY));
		context.put("TOPIC_NORMAL", new Integer(Topic.TYPE_NORMAL));
		// Topic Status
		context.put("STATUS_LOCKED", new Integer(Topic.STATUS_LOCKED));
		context.put("STATUS_UNLOCKED", new Integer(Topic.STATUS_UNLOCKED));
		// Moderation
		PermissionControl pc = SecurityRepository.get(SessionFacade.getUserSession().getUserId());
		context.put("moderator", pc.canAccess(SecurityConstants.PERM_MODERATION));
		context.put("can_remove_posts", pc.canAccess(SecurityConstants.PERM_MODERATION_POST_REMOVE));
		context.put("can_move_topics", pc.canAccess(SecurityConstants.PERM_MODERATION_TOPIC_MOVE));
		context.put("can_lockUnlock_topics", pc.canAccess(SecurityConstants.PERM_MODERATION_TOPIC_LOCK_UNLOCK));
		context.put("rssEnabled", SystemGlobals.getBoolValue(ConfigKeys.RSS_ENABLED));
	}

	public static boolean isTopicAccessible(int forumId) {
		Forum f = ForumRepository.getForum(forumId);
		if (f == null || !ForumRepository.isCategoryAccessible(f.getCategoryId())) {
			new ModerationHelper().denied(I18n.getMessage("PostShow.denied"));
			return false;
		}
		return true;
	}

	@SuppressWarnings("rawtypes")
	public static void notifyUsers(Topic t, Post p) {
		if (SystemGlobals.getBoolValue(ConfigKeys.MAIL_NOTIFY_ANSWERS)) {
			TopicDAO dao = DataAccessDriver.getInstance().newTopicDAO();
			List usersToNotify = dao.notifyUsers(t);
			// We only have to send an email if there are users subscribed to the topic
			if (usersToNotify != null && usersToNotify.size() > 0) {
				Executor.execute(new EmailSenderTask(new TopicReplySpammer(t, p, usersToNotify)));
			}
		}
	}

	public static synchronized void updateBoardStatus(Topic topic, int lastPostId, boolean firstPost, TopicDAO topicDao, ForumDAO forumDao) {
		topic.setLastPostId(lastPostId);
		topicDao.update(topic);
		forumDao.setLastPost(topic.getForumId(), lastPostId);
		if (firstPost) {
			forumDao.incrementTotalTopics(topic.getForumId(), 1);
		} else {
			topicDao.incrementTotalReplies(topic.getId());
		}
		topicDao.incrementTotalViews(topic.getId());
		TopicRepository.addTopic(topic);
		TopicRepository.pushTopic(topic);
		ForumRepository.incrementTotalMessages();
	}

	public static synchronized void deleteTopic(int topicId, int forumId, boolean fromModeration) {
		TopicDAO topicDao = DataAccessDriver.getInstance().newTopicDAO();
		Topic topic = new Topic();
		topic.setId(topicId);
		topic.setForumId(forumId);
		topicDao.delete(topic, fromModeration);
		if (!fromModeration) {
			// Updates the Recent Topics if it contains this topic
			TopicRepository.loadMostRecentTopics();
			// Updates the Hottest Topics if it contains this topic
			TopicRepository.loadHottestTopics();
			TopicRepository.clearCache(forumId);
			topicDao.removeSubscriptionByTopic(topicId);
		}
	}
}
