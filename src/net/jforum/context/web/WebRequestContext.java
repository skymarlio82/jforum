
package net.jforum.context.web;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import net.jforum.UrlPattern;
import net.jforum.UrlPatternCollection;
import net.jforum.context.RequestContext;
import net.jforum.context.SessionContext;
import net.jforum.exceptions.MultipartHandlingException;
import net.jforum.util.legacy.commons.fileupload.FileItem;
import net.jforum.util.legacy.commons.fileupload.FileUploadException;
import net.jforum.util.legacy.commons.fileupload.disk.DiskFileItemFactory;
import net.jforum.util.legacy.commons.fileupload.servlet.ServletFileUpload;
import net.jforum.util.legacy.commons.fileupload.servlet.ServletRequestContext;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.commons.lang.StringUtils;

public class WebRequestContext extends HttpServletRequestWrapper implements RequestContext {

	@SuppressWarnings("rawtypes")
	private Map query = null;

	@SuppressWarnings("rawtypes")
	public WebRequestContext(HttpServletRequest superRequest) throws IOException {
		super(superRequest);
		query = new HashMap();
		boolean isMultipart = false;
		String requestType = superRequest.getMethod().toUpperCase();
		System.out.println("DEBUG: requestType = " + requestType);
		String contextPath = superRequest.getContextPath();
		System.out.println("DEBUG: contextPath = " + contextPath);
		String requestUri = extractRequestUri(superRequest.getRequestURI(), contextPath);
		System.out.println("DEBUG: requestUri = " + requestUri);
		String encoding = SystemGlobals.getValue(ConfigKeys.ENCODING);
		System.out.println("DEBUG: encoding = " + encoding);
		String servletExtension = SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION);
		System.out.println("DEBUG: servletExtension = " + servletExtension);
		boolean isPost = "POST".equals(requestType);
		System.out.println("DEBUG: isPost = " + isPost);
		boolean isGet = !isPost;
		System.out.println("DEBUG: isGet = " + isGet);
		boolean isQueryStringEmpty = (superRequest.getQueryString() == null || superRequest.getQueryString().length() == 0);
		System.out.println("DEBUG: isQueryStringEmpty = " + isQueryStringEmpty);
		if (isGet && isQueryStringEmpty && requestUri.endsWith(servletExtension)) {
			superRequest.setCharacterEncoding(encoding);
			parseFriendlyURL(requestUri, servletExtension);
		} else if (isPost) {
			isMultipart = ServletFileUpload.isMultipartContent(new ServletRequestContext(superRequest));
			if (isMultipart) {
			    handleMultipart(superRequest, encoding);
			}
		}
		System.out.println("DEBUG: isMultipart = " + isMultipart);
		if (!isMultipart) {
			boolean isAjax = "XMLHttpRequest".equals(superRequest.getHeader("X-Requested-With"));
			System.out.println("DEBUG: isAjax = " + isAjax);
			if (!isAjax) {
				superRequest.setCharacterEncoding(encoding);
			} else {
				// Ajax requests are *usually* sent using application/x-www-form-urlencoded; charset=UTF-8. In JForum, we assume this as always true.
				superRequest.setCharacterEncoding("UTF-8");
			}
			String containerEncoding = SystemGlobals.getValue(ConfigKeys.DEFAULT_CONTAINER_ENCODING);
			if (isPost) {
				containerEncoding = encoding;
			}
			for (Enumeration e = superRequest.getParameterNames(); e.hasMoreElements(); ) {
				String name = (String)e.nextElement();
				System.out.println("DEBUG: URL Param after '?' : name = " + name);
				String[] values = superRequest.getParameterValues(name);
				if (values != null && values.length > 1) {
					for (int i = 0; i < values.length; i++) {
						addParameter(name, new String(values[i].getBytes(containerEncoding), encoding));
						System.out.println("DEBUG: URL Param Values after '?' : values[" + i + "] = " + new String(values[i].getBytes(containerEncoding), encoding));
					}
				} else {
					addParameter(name, new String(superRequest.getParameter(name).getBytes(containerEncoding), encoding));
					System.out.println("DEBUG: URL Param Value after '?' : value = " + new String(superRequest.getParameter(name).getBytes(containerEncoding), encoding));
				}
			}
			System.out.println("DEBUG: getModule() = " + getModule());
			System.out.println("DEBUG: getAction() = " + getAction());
			if (getModule() == null && getAction() == null) {
				int index = requestUri.indexOf('?');
				if (index > -1) {
					requestUri = requestUri.substring(0, index);
				}
				parseFriendlyURL(requestUri, servletExtension);
			}
		}
	}

	private void parseFriendlyURL(String requestUri, String servletExtension) {
		System.out.println("--> [WebRequestContext.parseFriendlyURL] ......");
		requestUri = requestUri.substring(0, requestUri.length() - servletExtension.length());
		System.out.println("DEBUG: 'requestUri' in parseFriendlyURL = " + requestUri);
		String[] urlModel = requestUri.split("/");
		int moduleIndex = 1;
		int actionIndex = 2;
		int baseLen = 3;
		System.out.println("DEBUG: urlModel[0] = " + urlModel[0] + ", urlModel[moduleIndex] = " + urlModel[moduleIndex] + ", urlModel[actionIndex] = " + urlModel[actionIndex]);
		UrlPattern url = null;
		if (urlModel.length >= baseLen) {
			StringBuffer sb = new StringBuffer(64).append(urlModel[moduleIndex]).append('.').append(urlModel[actionIndex]).append('.').append(urlModel.length - baseLen);
			System.out.println("DEBUG: [<moduleName>.<actionName>.<numberOfParameters>] = [" + sb.toString() + "]");
			url = UrlPatternCollection.findPattern(sb.toString());
		}
		if (url != null) {
			if (url.getSize() >= urlModel.length - baseLen) {
				for (int i = 0; i < url.getSize(); i++) {
					addParameter(url.getVars()[i], urlModel[i + baseLen]);
					System.out.println("DEBUG: Url Optional Param => " + url.getVars()[i] + " := " + urlModel[i + baseLen]);
				}
			}
			addOrReplaceParameter("module", urlModel[moduleIndex]);
			addParameter("action", urlModel[actionIndex]);
		} else {
			addOrReplaceParameter("module", null);
			addParameter("action", null);
		}
	}

    public SessionContext getSessionContext(boolean create) {
        return new WebSessionContext(getSession(true));
    }

    public SessionContext getSessionContext() {
        return new WebSessionContext(getSession());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private void handleMultipart(HttpServletRequest superRequest, String encoding) throws UnsupportedEncodingException {
		System.out.println("--> [WebRequestContext.handleMultipart] ......");
		String tmpPath = new StringBuffer(256)
		    .append(SystemGlobals.getApplicationPath())
		    .append('/')
		    .append(SystemGlobals.getValue(ConfigKeys.TMP_DIR))
		    .toString();
		File tmpDir = new File(tmpPath);
		boolean success = false;
		try {
			if (!tmpDir.exists()) {
				tmpDir.mkdirs();
				success = true;
			}
		} catch (Exception e) {
			// We won't log it because the directory creation failed for some reason - a SecurityException or something else. We don't care about it, as the code below tries to use java.io.tmpdir
		}
		if (!success) {
			tmpPath = System.getProperty("java.io.tmpdir");
			tmpDir = new File(tmpPath);
		}
		System.out.println("INFOR: start to instante the object of 'DiskFileItemFactory' ...");
		System.out.println("INFOR: start to instante the object of 'ServletFileUpload' ...");
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory(100*1024, tmpDir));
		upload.setHeaderEncoding(encoding);
		try {
			List items = upload.parseRequest(superRequest);
			for (Iterator iter = items.iterator(); iter.hasNext(); ) {
				FileItem item = (FileItem)iter.next();
				if (item.isFormField()) {
					addParameter(item.getFieldName(), item.getString(encoding));
				} else {
					if (item.getSize() > 0) {
						// We really don't want to call addParameter(), as there should not be possible to have multiple values for a InputStream data
						query.put(item.getFieldName(), item);
					}
				}
			}
		} catch (FileUploadException e) {
			throw new MultipartHandlingException("Error while processing multipart content: " + e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String[] getParameterValues(String name) {
		Object value = this.getObjectParameter(name);
		if (value instanceof String) {
			return new String[] { (String)value };
		}
		List l = (List)value;
		return (l == null) ? super.getParameterValues(name) : (String[])l.toArray(new String[0]);
	}

	private String extractRequestUri(String requestUri, String contextPath) {
		// First, remove the context path from the requestUri, so we can work only with the important stuff
		if (contextPath != null && contextPath.length() > 0) {
			requestUri = requestUri.substring(contextPath.length(), requestUri.length());
		}
		// Remove the "jsessionid" (or similar) from the URI Probably this is not the right way to go, since we're discarding the value...
		int index = requestUri.indexOf(';');
		if (index > -1) {
			int lastIndex = requestUri.indexOf('?', index);
			if (lastIndex == -1) {
				lastIndex = requestUri.indexOf('&', index);
			}
			if (lastIndex == -1) {
				requestUri = requestUri.substring(0, index);
			} else {
				String part1 = requestUri.substring(0, index);
				requestUri = part1 + requestUri.substring(lastIndex);
			}
		}
		return requestUri;
	}

	public String getParameter(String parameter) {
		return (String)query.get(parameter);
	}

	public int getIntParameter(String parameter) {
		return Integer.parseInt(getParameter(parameter));
	}

	public Object getObjectParameter(String parameter) {
		return query.get(parameter);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addParameter(String name, Object value) {
		if (!query.containsKey(name)) {
			query.put(name, value);
		} else {
			Object currentValue = getObjectParameter(name);
			List l = null;
			if (!(currentValue instanceof List)) {
				l = new ArrayList();
				l.add(currentValue);
			} else {
				l = (List)currentValue;
			}
			l.add(value);
			query.put(name, l);
		}
	}

	@SuppressWarnings("unchecked")
	public void addOrReplaceParameter(String name, Object value) {
		query.put(name, value);
	}

	public String getAction() {
		return getParameter("action");
	}

	@SuppressWarnings("unchecked")
	public void changeAction(String newAction) {
		if (query.containsKey("action")) {
			query.remove("action");
			query.put("action", newAction);
		} else {
			addParameter("action", newAction);
		}
	}

	public String getModule() {
		return getParameter("module");
	}

	public Object getObjectRequestParameter(String parameter) {
		return query.get(parameter);
	}

	public String getContextPath() {
		String contextPath = super.getContextPath();
		String proxiedContextPath = SystemGlobals.getValue(ConfigKeys.PROXIED_CONTEXT_PATH);
		if (!StringUtils.isEmpty(proxiedContextPath)) {
			contextPath = proxiedContextPath;
		}
		return contextPath;
	}

	public String getRemoteAddr() {
		// We look if the request is forwarded If it is not call the older function.
		String ip = super.getHeader("x-forwarded-for");
		if (ip == null) {
			ip = super.getRemoteAddr();
		} else {
			// Process the IP to keep the last IP (real ip of the computer on the net)
			StringTokenizer tokenizer = new StringTokenizer(ip, ",");
			// Ignore all tokens, except the last one
			for (int i = 0; i < tokenizer.countTokens() - 1; i++) {
				tokenizer.nextElement();
			}
			ip = tokenizer.nextToken().trim();
			if (ip.equals("")) {
				ip = null;
			}
		}
		// If the ip is still null, we put 0.0.0.0 to avoid null values
		if (ip == null) {
			ip = "0.0.0.0";
		}
		return ip;
	}
}
