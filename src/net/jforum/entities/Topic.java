
package net.jforum.entities;

import java.io.Serializable;
import java.util.Date;

@SuppressWarnings("serial")
public class Topic implements Serializable {

	public final static int TYPE_NORMAL   = 0;
	public final static int TYPE_STICKY   = 1;
	public final static int TYPE_ANNOUNCE = 2;

	public final static int STATUS_UNLOCKED = 0;
	public final static int STATUS_LOCKED   = 1;

	private int id = 0;
	private int forumId = 0;
	private int totalViews = 0;
	private int totalReplies = 0;
	private int status = 0;
	private int type = 0;
	private int firstPostId = 0;
	private int lastPostId = 0;
	private int voteId = 0;
	private int movedId = 0;

	private boolean read = true;
	private boolean moderated = false;
	private boolean isHot = false;
	private boolean hasAttach = false;
	private boolean paginate = false;

	private String firstPostTime = null;
	private String lastPostTime = null;
	private String title = null;

	private Date time = null;
	private Date lastPostDate = null;

	private Double totalPages = 0d;

	private User postedBy = null;
	private User lastPostBy = null;

	public Topic() {
		
	}

	public Topic(int topicId) {
		this.id = topicId;
	}

	public int getFirstPostId() {
		return firstPostId;
	}

	public int getId() {
		return id;
	}

	public int getForumId() {
		return forumId;
	}

	public int getLastPostId() {
		return lastPostId;
	}

	public int getStatus() {
		return status;
	}

	public Date getTime() {
		return time;
	}

	public void setFirstPostTime(String d) {
		this.firstPostTime = d;
	}

	public void setLastPostTime(String d) {
		this.lastPostTime = d;
	}

	public String getTitle() {
		return (title == null) ? "" : title;
	}

	public int getTotalReplies() {
		return totalReplies;
	}

	public int getTotalViews() {
		return totalViews;
	}

	public User getPostedBy() {
		return postedBy;
	}

	public User getLastPostBy() {
		return lastPostBy;
	}

	public int getType() {
		return type;
	}

	public boolean isVote() {
		return voteId != 0;
	}

	public int getVoteId() {
		return voteId;
	}

	public void setFirstPostId(int firstPostId) {
		this.firstPostId = firstPostId;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setForumId(int idForum) {
		this.forumId = idForum;
	}

	public void setLastPostId(int lastPostId) {
		this.lastPostId = lastPostId;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setTotalReplies(int totalReplies) {
		this.totalReplies = totalReplies;
	}

	public void setTotalViews(int totalViews) {
		this.totalViews = totalViews;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setVoteId(int voteId) {
		this.voteId = voteId;
	}

	public boolean isModerated() {
		return moderated;
	}

	public void setModerated(boolean b) {
		this.moderated = b;
	}

	public void setPostedBy(User u) {
		this.postedBy = u;
	}

	public void setLastPostBy(User u) {
		this.lastPostBy = u;
	}

	public String getFirstPostTime() {
		return firstPostTime;
	}

	public String getLastPostTime() {
		return lastPostTime;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public boolean getRead() {
		return read;
	}

	public void setLastPostDate(Date t) {
		this.lastPostDate = t;
	}

	public Date getLastPostDate() {
		return lastPostDate;
	}

	public void setPaginate(boolean paginate) {
		this.paginate = paginate;
	}

	public boolean getPaginate() {
		return paginate;
	}

	public void setTotalPages(Double total) {
		this.totalPages = total;
	}

	public Double getTotalPages() {
		return totalPages;
	}

	public void setHot(boolean hot) {
		this.isHot = hot;
	}

	public boolean isHot() {
		return isHot;
	}

	public void setHasAttach(boolean b) {
		this.hasAttach = b;
	}

	public boolean hasAttach() {
		return hasAttach;
	}

	public boolean equals(Object o) {
		if (!(o instanceof Topic)) {
			return false;
		}
		return ((Topic)o).getId() == id;
	}

	public int hashCode() {
		return id;
	}

	public String toString() {
		return "[" + id + ", " + title + "]";
	}

	public int getMovedId() {
		return movedId;
	}

	public void setMovedId(int movedId) {
		this.movedId = movedId;
	}
}
