
package net.jforum.util.legacy.commons.fileupload.servlet;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import net.jforum.util.legacy.commons.fileupload.RequestContext;

public class ServletRequestContext implements RequestContext {

	private HttpServletRequest request = null;

	public ServletRequestContext(HttpServletRequest request) {
		this.request = request;
	}

	public String getContentType() {
		return request.getContentType();
	}

	public int getContentLength() {
		return request.getContentLength();
	}

	public InputStream getInputStream() throws IOException {
		return request.getInputStream();
	}

	public String toString() {
		return "ContentLength=" + getContentLength() + ", ContentType=" + getContentType();
	}
}
