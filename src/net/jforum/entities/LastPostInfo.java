
package net.jforum.entities;

import java.io.Serializable;

@SuppressWarnings("serial")
public class LastPostInfo implements Serializable {

	private long postTimeMillis = 0L;
	private int topicId = 0;
	private int postId = 0;
	private int userId = 0;
	private int topicReplies = 0;
	private String username = null;
	private String postDate = null;
	private boolean hasInfo = false;

	public void setHasInfo(boolean value) {
		this.hasInfo = value;
	}

	public boolean hasInfo() {
		return hasInfo;
	}

	public String getPostDate() {
		return postDate;
	}

	public void setPostDate(String postDate) {
		this.postDate = postDate;
	}

	public int getPostId() {
		return postId;
	}

	public void setPostId(int postId) {
		this.postId = postId;
	}

	public long getPostTimeMillis() {
		return postTimeMillis;
	}

	public void setPostTimeMillis(long postTimeMillis) {
		this.postTimeMillis = postTimeMillis;
	}

	public int getTopicId() {
		return topicId;
	}

	public void setTopicId(int topicId) {
		this.topicId = topicId;
	}

	public int getTopicReplies() {
		return topicReplies;
	}

	public void setTopicReplies(int topicReplies) {
		this.topicReplies = topicReplies;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
