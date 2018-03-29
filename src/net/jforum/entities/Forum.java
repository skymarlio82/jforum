
package net.jforum.entities;

import java.io.Serializable;
import java.util.List;

import net.jforum.repository.ForumRepository;

@SuppressWarnings("serial")
public class Forum implements Serializable {

	private int id = 0;
	private int idCategories = 0;
	private String name = null;
	private String description = null;
	private int order = 0;
	private int totalTopics = 0;
	private int totalPosts = 0;
	private int lastPostId = 0;
	private boolean moderated = false;
	private boolean unread = false;
	private LastPostInfo lpi = null;

	public Forum() {
		
	}

	public Forum(int forumId) {
		this.id = forumId;
	}

	public Forum(Forum f) {
		this.description = f.getDescription();
		this.id = f.getId();
		this.idCategories = f.getCategoryId();
		this.lastPostId = f.getLastPostId();
		this.moderated = f.isModerated();
		this.name = f.getName();
		this.order = f.getOrder();
		this.totalPosts = f.getTotalPosts();
		this.totalTopics = f.getTotalTopics();
		this.unread = f.getUnread();
		this.lpi = f.getLastPostInfo();
	}

	public void setLastPostInfo(LastPostInfo lpi) {
		this.lpi = lpi;
	}

	public LastPostInfo getLastPostInfo() {
		return lpi;
	}

	@SuppressWarnings("rawtypes")
	public List getModeratorList() {
		return ForumRepository.getModeratorList(id);
	}

	public String getDescription() {
		return description;
	}

	public int getId() {
		return id;
	}

	public int getCategoryId() {
		return idCategories;
	}

	public int getLastPostId() {
		return lastPostId;
	}

	public boolean isModerated() {
		return moderated;
	}

	public String getName() {
		return name;
	}

	public int getOrder() {
		return order;
	}

	public int getTotalTopics() {
		return totalTopics;
	}

	public boolean getUnread() {
		return unread;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setIdCategories(int idCategories) {
		this.idCategories = idCategories;
	}

	public void setLastPostId(int lastPostId) {
		this.lastPostId = lastPostId;
	}

	public void setModerated(boolean moderated) {
		this.moderated = moderated;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public void setUnread(boolean status) {
		this.unread = status;
	}

	public void setTotalTopics(int totalTopics) {
		this.totalTopics = totalTopics;
	}

	public int getTotalPosts() {
		return totalPosts;
	}

	public void setTotalPosts(int totalPosts) {
		this.totalPosts = totalPosts;
	}

	public boolean equals(Object o) {
		return ((o instanceof Forum) && (((Forum)o).getId() == id));
	}

	public int hashCode() {
		return id;
	}

	public String toString() {
		return "[" + name + ", id=" + id + ", order=" + order + "]";
	}
}
