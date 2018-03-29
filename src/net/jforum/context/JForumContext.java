
package net.jforum.context;

import net.jforum.util.preferences.ConfigKeys;

public class JForumContext implements ForumContext {
	
	private String contextPath = null;
	private String servletExtension = null;
	private RequestContext request = null;
	private ResponseContext response = null;
	private boolean isEncodingDisabled = false;
	private boolean isBot = false;

	public JForumContext(String contextPath, String servletExtension, RequestContext request, ResponseContext response) {
		this.contextPath = contextPath;
		this.servletExtension = servletExtension;
		this.request = request;
		this.response = response;
		Boolean isBotObject = (Boolean)request.getAttribute(ConfigKeys.IS_BOT);
		this.isBot = (isBotObject != null && isBotObject.booleanValue());
		this.isEncodingDisabled = isBot;
	}

	public JForumContext(String contextPath, String servletExtension, RequestContext request, ResponseContext response, boolean isEncodingDisabled) {
		this.contextPath = contextPath;
		this.servletExtension = servletExtension;
		this.request = request;
		this.response = response;
		this.isEncodingDisabled = isEncodingDisabled;
		Boolean isBotObject = (Boolean)request.getAttribute(ConfigKeys.IS_BOT);
		this.isBot = (isBotObject != null && isBotObject.booleanValue());
	}

	public boolean isBot() {
		return isBot;
	}

	public String encodeURL(String url) {
		return encodeURL(url, servletExtension);
	}

	public String encodeURL(String url, String extension) {
		String ucomplete = contextPath + url + extension;
		if (isEncodingDisabled()) {
			return ucomplete;
		}
		return response.encodeURL(ucomplete);
	}

	public boolean isEncodingDisabled() {
		return isEncodingDisabled;
	}

	public RequestContext getRequest() {
		return request;
	}

	public ResponseContext getResponse() {
		return response;
	}
}
