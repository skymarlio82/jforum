
package net.jforum;

import java.util.HashMap;
import java.util.Map;

public class UrlPatternCollection {

	@SuppressWarnings("rawtypes")
	private static Map patternsMap = new HashMap();

	public static UrlPattern findPattern(String name) {
		return (UrlPattern)UrlPatternCollection.patternsMap.get(name);
	}

	@SuppressWarnings("unchecked")
	public static void addPattern(String name, String value) {
		UrlPatternCollection.patternsMap.put(name, new UrlPattern(name, value));
	}
}
