
package net.jforum.dao.generic;

import net.jforum.dao.ApiDAO;
import net.jforum.dao.AttachmentDAO;
import net.jforum.dao.BanlistDAO;
import net.jforum.dao.BannerDAO;
import net.jforum.dao.BookmarkDAO;
import net.jforum.dao.CategoryDAO;
import net.jforum.dao.ConfigDAO;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.ForumDAO;
import net.jforum.dao.GroupDAO;
import net.jforum.dao.GroupSecurityDAO;
import net.jforum.dao.KarmaDAO;
import net.jforum.dao.LuceneDAO;
import net.jforum.dao.MailIntegrationDAO;
import net.jforum.dao.ModerationDAO;
import net.jforum.dao.ModerationLogDAO;
import net.jforum.dao.PollDAO;
import net.jforum.dao.PostDAO;
import net.jforum.dao.PrivateMessageDAO;
import net.jforum.dao.RankingDAO;
import net.jforum.dao.SmilieDAO;
import net.jforum.dao.SummaryDAO;
import net.jforum.dao.TopicDAO;
import net.jforum.dao.TreeGroupDAO;
import net.jforum.dao.UserDAO;
import net.jforum.dao.UserSessionDAO;
import net.jforum.dao.generic.security.GenericGroupSecurityDAO;

public class GenericDataAccessDriver extends DataAccessDriver {

	private static GroupDAO groupDao                     = new GenericGroupDAO();
	private static PostDAO postDao                       = new GenericPostDAO();
	private static PollDAO pollDao                       = new GenericPollDAO();
	private static RankingDAO rankingDao                 = new GenericRankingDAO();
	private static TopicDAO topicDao                     = new GenericTopicDAO();
	private static UserDAO userDao                       = new GenericUserDAO();
	private static TreeGroupDAO treeGroupDao             = new GenericTreeGroupDAO();
	private static SmilieDAO smilieDao                   = new GenericSmilieDAO();
	private static GroupSecurityDAO groupSecurityDao     = new GenericGroupSecurityDAO();
	private static PrivateMessageDAO privateMessageDao   = new GenericPrivateMessageDAO();
	private static UserSessionDAO userSessionDao         = new GenericUserSessionDAO();
	private static KarmaDAO karmaDao                     = new GenericKarmaDAO();
	private static BookmarkDAO bookmarkDao               = new GenericBookmarkDAO();
	private static AttachmentDAO attachmentDao           = new GenericAttachmentDAO();
	private static ModerationDAO moderationDao           = new GenericModerationDAO();
	private static ForumDAO forumDao                     = new GenericForumDAO();
	private static CategoryDAO categoryDao               = new GenericCategoryDAO();
	private static ConfigDAO configDao                   = new GenericConfigDAO();
	private static BannerDAO bannerDao                   = new GenericBannerDAO();
	private static SummaryDAO summaryDao                 = new GenericSummaryDAO();
	private static MailIntegrationDAO mailIntegrationDao = new GenericMailIntegrationDAO();
	private static ApiDAO apiDAO                         = new GenericApiDAO();
	private static BanlistDAO banlistDao                 = new GenericBanlistDAO();
	private static ModerationLogDAO moderationLogDao     = new GenericModerationLogDAO();
	private static LuceneDAO luceneDao                   = new GenericLuceneDAO();

	public ForumDAO newForumDAO() {
		return forumDao;
	}

	public GroupDAO newGroupDAO() {
		return groupDao;
	}

	public PostDAO newPostDAO() {
		return postDao;
	}

	public PollDAO newPollDAO() {
		return pollDao;
	}

	public RankingDAO newRankingDAO() {
		return rankingDao;
	}

	public TopicDAO newTopicDAO() {
		return topicDao;
	}

	public UserDAO newUserDAO() {
		return userDao;
	}

	public CategoryDAO newCategoryDAO() {
		return categoryDao;
	}

	public TreeGroupDAO newTreeGroupDAO() {
		return treeGroupDao;
	}

	public SmilieDAO newSmilieDAO() {
		return smilieDao;
	}

	public GroupSecurityDAO newGroupSecurityDAO() {
		return groupSecurityDao;
	}

	public PrivateMessageDAO newPrivateMessageDAO() {
		return privateMessageDao;
	}

	public UserSessionDAO newUserSessionDAO() {
		return userSessionDao;
	}

	public ConfigDAO newConfigDAO() {
		return configDao;
	}

	public KarmaDAO newKarmaDAO() {
		return karmaDao;
	}

	public BookmarkDAO newBookmarkDAO() {
		return bookmarkDao;
	}

	public AttachmentDAO newAttachmentDAO() {
		return attachmentDao;
	}

	public ModerationDAO newModerationDAO() {
		return moderationDao;
	}

	public BannerDAO newBannerDAO() {
		return bannerDao;
	}

	public SummaryDAO newSummaryDAO() {
		return summaryDao;
	}

	public MailIntegrationDAO newMailIntegrationDAO() {
		return mailIntegrationDao;
	}

	public ApiDAO newApiDAO() {
		return apiDAO;
	}

	public BanlistDAO newBanlistDAO() {
		return banlistDao;
	}

	public ModerationLogDAO newModerationLogDAO() {
		return moderationLogDao;
	}

	public LuceneDAO newLuceneDAO() {
		return luceneDao;
	}
}
