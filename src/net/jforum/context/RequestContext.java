
package net.jforum.context;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.http.Cookie;

public interface RequestContext {

	String getRequestURI();
	String getQueryString();
	String getHeader(String name);
	Cookie[] getCookies();
	String getRemoteAddr();
	int getServerPort();
	String getScheme();
	String getServerName();
	void removeAttribute(String name);
	void setAttribute(String name, Object o);
	Object getAttribute(String name);
	void setCharacterEncoding(String env) throws UnsupportedEncodingException;
	SessionContext getSessionContext(boolean create);
	SessionContext getSessionContext();
	String getContextPath();
	String getRemoteUser();
	int getIntParameter(String parameter);
	String[] getParameterValues(String name);
	String getParameter(String name);
	@SuppressWarnings("rawtypes")
	Enumeration getParameterNames();
	String getAction();
	String getModule();
	void addParameter(String name, Object value);
	void addOrReplaceParameter(String name, Object value);
	Object getObjectParameter(String parameter);
	Locale getLocale();
}
