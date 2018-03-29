
package net.jforum.context;

public interface ForumContext {

	String encodeURL(String url);
	String encodeURL(String url, String extension);
	boolean isEncodingDisabled();
	RequestContext getRequest();
	ResponseContext getResponse();
	boolean isBot();
}
