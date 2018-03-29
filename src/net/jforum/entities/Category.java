
package net.jforum.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.jforum.SessionFacade;
import net.jforum.exceptions.ForumOrderChangedException;
import net.jforum.repository.SecurityRepository;
import net.jforum.security.PermissionControl;
import net.jforum.security.SecurityConstants;
import net.jforum.util.ForumOrderComparator;

@SuppressWarnings("serial")
public class Category implements Serializable {

	private int id = 0;
	private int order = 0;
	private boolean moderated = false;
	private String name = null;
	@SuppressWarnings("rawtypes")
	private Map forumsIdMap = new HashMap();
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Set forums = new TreeSet(new ForumOrderComparator());

	public Category() {
		
	}

	public Category(int id) {
		this.id = id;
	}

	public Category(String name, int id) {
		this.name = name;
		this.id = id;
	}

	@SuppressWarnings("rawtypes")
	public Category(Category c) {
		name = c.getName();
		id = c.getId();
		order = c.getOrder();
		moderated = c.isModerated();
		for (Iterator iter = c.getForums().iterator(); iter.hasNext(); ) {
			addForum(new Forum((Forum) iter.next()));
		}
	}

	public void setModerated(boolean status) {
		this.moderated = status;
	}

	public boolean isModerated() {
		return moderated;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getOrder() {
		return order;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@SuppressWarnings("unchecked")
	public void addForum(Forum forum) {
		forumsIdMap.put(new Integer(forum.getId()), forum);
		forums.add(forum);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void reloadForum(Forum forum) {
		Forum currentForum = getForum(forum.getId());
		if (forum.getOrder() != currentForum.getOrder()) {
			throw new ForumOrderChangedException("Forum #" + forum.getId() + " cannot be reloaded, since its display order was changed. You must call Category#changeForumOrder(Forum) first");
		}
		Set tmpSet = new TreeSet(new ForumOrderComparator());
		tmpSet.addAll(forums);
		tmpSet.remove(currentForum);
		tmpSet.add(forum);
		forumsIdMap.put(new Integer(forum.getId()), forum);
		forums = tmpSet;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void changeForumOrder(Forum forum) {
		Forum current = getForum(forum.getId());
		Forum currentAtOrder = findByOrder(forum.getOrder());
		Set tmpSet = new TreeSet(new ForumOrderComparator());
		tmpSet.addAll(forums);
		// Remove the forum in the current order where the changed forum will need to be
		if (currentAtOrder != null) {
			tmpSet.remove(currentAtOrder);
		}
		tmpSet.add(forum);
		forumsIdMap.put(new Integer(forum.getId()), forum);
		// Remove the forum in the position occupied by the changed forum before its modification, so then we can add the another forum into its position
		if (currentAtOrder != null) {
			tmpSet.remove(current);
			currentAtOrder.setOrder(current.getOrder());
			tmpSet.add(currentAtOrder);
			forumsIdMap.put(new Integer(currentAtOrder.getId()), currentAtOrder);
		}
		forums = tmpSet;
	}

	@SuppressWarnings("rawtypes")
	private Forum findByOrder(int order) {
		for (Iterator iter = forums.iterator(); iter.hasNext();) {
			Forum f = (Forum) iter.next();
			if (f.getOrder() == order) {
				return f;
			}
		}
		return null;
	}

	public void removeForum(int forumId) {
		forums.remove(getForum(forumId));
		forumsIdMap.remove(new Integer(forumId));
	}

	public Forum getForum(int userId, int forumId) {
		PermissionControl pc = SecurityRepository.get(userId);
		if (pc.canAccess(SecurityConstants.PERM_FORUM, Integer.toString(forumId))) {
			return (Forum)forumsIdMap.get(new Integer(forumId));
		}
		return null;
	}

	public Forum getForum(int forumId) {
		return getForum(SessionFacade.getUserSession().getUserId(), forumId);
	}

	@SuppressWarnings("rawtypes")
	public Collection getForums() {
		if (forums.size() == 0) {
			return forums;
		}
		return getForums(SessionFacade.getUserSession().getUserId());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Collection getForums(int userId) {
		PermissionControl pc = SecurityRepository.get(userId);
		List forums = new ArrayList();
		for (Iterator iter = this.forums.iterator(); iter.hasNext();) {
			Forum f = (Forum) iter.next();
			if (pc.canAccess(SecurityConstants.PERM_FORUM, Integer.toString(f.getId()))) {
				forums.add(f);
			}
		}
		return forums;
	}

	public int hashCode() {
		return id;
	}

	public boolean equals(Object o) {
		return ((o instanceof Category) && (((Category)o).getId() == id));
	}

	public String toString() {
		return "[name = " + this.name + ", id = " + this.id + ", order = " + this.order + ", moderated = " + this.moderated + "]";
	}
}
