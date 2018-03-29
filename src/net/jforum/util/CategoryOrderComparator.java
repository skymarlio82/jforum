
package net.jforum.util;

import java.io.Serializable;
import java.util.Comparator;

import net.jforum.entities.Category;

@SuppressWarnings({ "serial", "rawtypes" })
public class CategoryOrderComparator implements Comparator, Serializable {

	public int compare(Object o1, Object o2) {
		Category c1 = (Category) o1;
		Category c2 = (Category) o2;
		if (c1.getOrder() > c2.getOrder()) {
			return 1;
		} else if (c1.getOrder() < c2.getOrder()) {
			return -1;
		} else {
			return c1.getName().compareTo(c2.getName());
		}
	}
}