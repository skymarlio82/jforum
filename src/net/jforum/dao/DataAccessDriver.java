
package net.jforum.dao;

public abstract class DataAccessDriver {

	private static DataAccessDriver driver = null;

	protected DataAccessDriver() {

	}

	public static void init(DataAccessDriver implementation) {
		driver = implementation;
	}

	public static DataAccessDriver getInstance() {
		return driver;
	}

	public abstract ForumDAO newForumDAO();

	public abstract GroupDAO newGroupDAO();

	public abstract PostDAO newPostDAO();

	public abstract PollDAO newPollDAO();

	public abstract RankingDAO newRankingDAO();

	public abstract TopicDAO newTopicDAO();

	public abstract UserDAO newUserDAO();

	public abstract CategoryDAO newCategoryDAO();

	public abstract TreeGroupDAO newTreeGroupDAO();

	public abstract SmilieDAO newSmilieDAO();

	public abstract GroupSecurityDAO newGroupSecurityDAO();

	public abstract PrivateMessageDAO newPrivateMessageDAO();

	public abstract UserSessionDAO newUserSessionDAO();

	public abstract ConfigDAO newConfigDAO();

	public abstract KarmaDAO newKarmaDAO();

	public abstract BookmarkDAO newBookmarkDAO();

	public abstract AttachmentDAO newAttachmentDAO();

	public abstract ModerationDAO newModerationDAO();

	public abstract BannerDAO newBannerDAO();

	public abstract SummaryDAO newSummaryDAO();

	public abstract MailIntegrationDAO newMailIntegrationDAO();

	public abstract ApiDAO newApiDAO();

	public abstract BanlistDAO newBanlistDAO();

	public abstract ModerationLogDAO newModerationLogDAO();

	public abstract LuceneDAO newLuceneDAO();
}
