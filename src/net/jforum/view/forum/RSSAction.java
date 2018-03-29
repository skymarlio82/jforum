
package net.jforum.view.forum;

import java.util.List;

import net.jforum.Command;
import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.PostDAO;
import net.jforum.dao.TopicDAO;
import net.jforum.entities.Forum;
import net.jforum.entities.Topic;
import net.jforum.repository.ForumRepository;
import net.jforum.util.I18n;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import net.jforum.util.rss.RSSAware;
import net.jforum.util.rss.RecentTopicsRSS;
import net.jforum.util.rss.TopicPostsRSS;
import net.jforum.util.rss.TopicRSS;
import net.jforum.view.forum.common.TopicsCommon;
import freemarker.template.SimpleHash;
import freemarker.template.Template;

public class RSSAction extends Command {

	@SuppressWarnings("rawtypes")
	public void forumTopics() {
		int forumId = request.getIntParameter("forum_id");
		if (!TopicsCommon.isTopicAccessible(forumId)) {
			JForumExecutionContext.requestBasicAuthentication();
			return;
		}
		List posts = DataAccessDriver.getInstance().newPostDAO().selectLatestByForumForRSS(forumId, SystemGlobals.getIntValue(ConfigKeys.TOPICS_PER_PAGE));
		Forum forum = ForumRepository.getForum(forumId);
		String[] p = { forum.getName() };
		RSSAware rss = new TopicRSS(I18n.getMessage("RSS.ForumTopics.title", p), I18n.getMessage("RSS.ForumTopics.description", p), forumId, posts);
		context.put("rssContents", rss.createRSS());
	}

	@SuppressWarnings("rawtypes")
	public void topicPosts() {
		int topicId = request.getIntParameter("topic_id");
		PostDAO pm = DataAccessDriver.getInstance().newPostDAO();
		TopicDAO tm = DataAccessDriver.getInstance().newTopicDAO();
		Topic topic = tm.selectById(topicId);
		if (!TopicsCommon.isTopicAccessible(topic.getForumId()) || topic.getId() == 0) {
			JForumExecutionContext.requestBasicAuthentication();
			return;
		}
		tm.incrementTotalViews(topic.getId());
		List posts = pm.selectAllByTopic(topicId);
		String[] p = { topic.getTitle() };
		String title = I18n.getMessage("RSS.TopicPosts.title", p);
		String description = I18n.getMessage("RSS.TopicPosts.description", p);
		RSSAware rss = new TopicPostsRSS(title, description, topic.getForumId(), posts);
		context.put("rssContents", rss.createRSS());
	}

	@SuppressWarnings("rawtypes")
	public void recentTopics() {
		String title = I18n.getMessage("RSS.RecentTopics.title", new Object[] { SystemGlobals.getValue(ConfigKeys.FORUM_NAME) });
		String description = I18n.getMessage("RSS.RecentTopics.description");
		List posts = DataAccessDriver.getInstance().newPostDAO().selectHotForRSS(SystemGlobals.getIntValue(ConfigKeys.POSTS_PER_PAGE));
		RSSAware rss = new RecentTopicsRSS(title, description, posts);
		context.put("rssContents", rss.createRSS());
	}

	public void list() {

	}

	public Template process(RequestContext request, ResponseContext response, SimpleHash context) {
		if (!SessionFacade.isLogged() && UserAction.hasBasicAuthentication(request)) {
			new UserAction().validateLogin(request);
			JForumExecutionContext.setRedirect(null);
		}
		JForumExecutionContext.setContentType("text/xml");
		super.setTemplateName(TemplateKeys.RSS);
		return super.process(request, response, context);
	}
}
