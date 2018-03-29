
package net.jforum.util.legacy.commons.fileupload.servlet;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import net.jforum.util.legacy.commons.fileupload.FileItemFactory;
import net.jforum.util.legacy.commons.fileupload.FileUpload;
import net.jforum.util.legacy.commons.fileupload.FileUploadException;

public class ServletFileUpload extends FileUpload {

	public ServletFileUpload() {
		super();
	}

	public ServletFileUpload(FileItemFactory fileItemFactory) {
		super(fileItemFactory);
	}

	@SuppressWarnings("rawtypes")
	public List parseRequest(HttpServletRequest request) throws FileUploadException {
		System.out.println("--> [ServletFileUpload.parseRequest] ......");
		return parseRequest(new ServletRequestContext(request));
	}
}
