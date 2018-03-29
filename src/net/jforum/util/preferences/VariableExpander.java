
package net.jforum.util.preferences;

import java.util.HashMap;
import java.util.Map;

public class VariableExpander {

	private VariableStore variables = null;
	private String pre              = null;
	private String post             = null;
	@SuppressWarnings("rawtypes")
	private Map cache               = null;

	@SuppressWarnings("rawtypes")
	public VariableExpander(VariableStore variables, String pre, String post) {
		this.variables = variables;
		this.pre = pre;
		this.post = post;
		cache = new HashMap();
	}

	public void clearCache() {
		System.out.println("--> [VariableExpander.clearCache] ......");
		cache.clear();
	}

	@SuppressWarnings("unchecked")
	public String expandVariables(String source) {
		String result = (String)cache.get(source);
		// directly return result from cache if it already exist, or 'source' is NULL
		if (source == null || result != null) {
			return result;
		}
		int fIndex = source.indexOf(pre);
		// directly return input if no replace tag in text
		if (fIndex == -1) {
			return source;
		}
		StringBuffer sb = new StringBuffer(source);
		while (fIndex > -1) {
			int lIndex = sb.indexOf(post);
			int start = fIndex + pre.length();
			if (fIndex == 0) {
				String varName = sb.substring(start, start + lIndex - pre.length());
				sb.replace(fIndex, fIndex + lIndex + 1, variables.getVariableValue(varName));
			} else {
				String varName = sb.substring(start, lIndex);
				sb.replace(fIndex, lIndex + 1, variables.getVariableValue(varName));
			}
			fIndex = sb.indexOf(pre);
		}
		result = sb.toString();
		cache.put(source, result);
		return result;
	}
}
