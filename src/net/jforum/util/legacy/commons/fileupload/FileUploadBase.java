
package net.jforum.util.legacy.commons.fileupload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.jforum.util.legacy.commons.fileupload.servlet.ServletRequestContext;

public abstract class FileUploadBase {

	public final static String CONTENT_TYPE        = "Content-type";
	public final static String CONTENT_DISPOSITION = "Content-disposition";
	public final static String FORM_DATA           = "form-data";
	public final static String ATTACHMENT          = "attachment";
	public final static String MULTIPART           = "multipart/";
	public final static String MULTIPART_FORM_DATA = "multipart/form-data";
	public final static String MULTIPART_MIXED     = "multipart/mixed";
	public final static int    MAX_HEADER_SIZE     = 1024;

	private long sizeMax = -1L;

	private String headerEncoding = null;

	public final static boolean isMultipartContent(RequestContext ctx) {
		String contentType = ctx.getContentType();
		if (contentType == null) {
			return false;
		}
		if (contentType.toLowerCase().startsWith(MULTIPART)) {
			return true;
		}
		return false;
	}

	public final static boolean isMultipartContent(HttpServletRequest req) {
		if (!"post".equals(req.getMethod().toLowerCase())) {
			return false;
		}
		String contentType = req.getContentType();
		if (contentType == null) {
			return false;
		}
		if (contentType.toLowerCase().startsWith(MULTIPART)) {
			return true;
		}
		return false;
	}

	public abstract FileItemFactory getFileItemFactory();

	public abstract void setFileItemFactory(FileItemFactory factory);

	public long getSizeMax() {
		return sizeMax;
	}

	public void setSizeMax(long sizeMax) {
		this.sizeMax = sizeMax;
	}

	public String getHeaderEncoding() {
		return headerEncoding;
	}

	public void setHeaderEncoding(String encoding) {
		headerEncoding = encoding;
	}

	@SuppressWarnings("rawtypes")
	public List parseRequest(HttpServletRequest req) throws FileUploadException {
		return parseRequest(new ServletRequestContext(req));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List parseRequest(RequestContext ctx) throws FileUploadException {
		System.out.println("--> [FileUploadBase.parseRequest] ......");
		if (ctx == null) {
			throw new NullPointerException("ctx parameter");
		}
		ArrayList items = new ArrayList();
		String contentType = ctx.getContentType();
		System.out.println("DEBUG: contentType = " + contentType);
		if (contentType == null || !contentType.toLowerCase().startsWith(MULTIPART)) {
			throw new InvalidContentTypeException("the request doesn't contain a " + MULTIPART_FORM_DATA + " or " + MULTIPART_MIXED + " stream, content type header is " + contentType);
		}
		int requestSize = ctx.getContentLength();
		System.out.println("DEBUG: contentType = " + requestSize);
		if (requestSize == -1) {
			throw new UnknownSizeException("the request was rejected because its size is unknown");
		}
		if (sizeMax >= 0 && requestSize > sizeMax) {
			throw new SizeLimitExceededException("the request was rejected because its size exceeds allowed range");
		}
		try {
			byte[] boundary = getBoundary(contentType);
			if (boundary == null) {
				throw new FileUploadException("the request was rejected because no multipart boundary was found");
			}
			System.out.println("INFOR: start to fetch the InputStream from HttpServletRequest ...");
			InputStream input = ctx.getInputStream();
			System.out.println("INFOR: start to instante the oject of 'MultipartStream' ...");
			MultipartStream multi = new MultipartStream(input, boundary);
			multi.setHeaderEncoding(headerEncoding);
			boolean nextPart = multi.skipPreamble();
			while (nextPart) {
				Map headers = parseHeaders(multi.readHeaders());
				String fieldName = getFieldName(headers);
				if (fieldName != null) {
					String subContentType = getHeader(headers, CONTENT_TYPE);
					if (subContentType != null && subContentType.toLowerCase().startsWith(MULTIPART_MIXED)) {
						// Multiple files.
						byte[] subBoundary = getBoundary(subContentType);
						multi.setBoundary(subBoundary);
						boolean nextSubPart = multi.skipPreamble();
						while (nextSubPart) {
							headers = parseHeaders(multi.readHeaders());
							if (getFileName(headers) != null) {
								FileItem item = createItem(headers, false);
								OutputStream os = item.getOutputStream();
								try {
									multi.readBodyData(os);
								} finally {
									os.close();
								}
								items.add(item);
							} else {
								// Ignore anything but files inside multipart/mixed.
								multi.discardBodyData();
							}
							nextSubPart = multi.readBoundary();
						}
						multi.setBoundary(boundary);
					} else {
						FileItem item = createItem(headers, getFileName(headers) == null);
						OutputStream os = item.getOutputStream();
						try {
							multi.readBodyData(os);
						} finally {
							os.close();
						}
						items.add(item);
					}
				} else {
					// Skip this part.
					multi.discardBodyData();
				}
				nextPart = multi.readBoundary();
			}
		} catch (IOException e) {
			throw new FileUploadException("Processing of " + MULTIPART_FORM_DATA + " request failed. " + e.getMessage());
		}
		return items;
	}

	@SuppressWarnings("rawtypes")
	protected byte[] getBoundary(String contentType) {
		System.out.println("DEBUG: contentType = " + contentType);
		ParameterParser parser = new ParameterParser();
		parser.setLowerCaseNames(true);
		// Parameter parser can handle null input
		Map params = parser.parse(contentType, ';');
		String boundaryStr = (String)params.get("boundary");
		System.out.println("DEBUG: boundaryStr = " + boundaryStr);
		if (boundaryStr == null) {
			return null;
		}
		byte[] boundary = null;
		try {
			boundary = boundaryStr.getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			boundary = boundaryStr.getBytes();
		}
		return boundary;
	}

	@SuppressWarnings("rawtypes")
	protected String getFileName(Map headers) {
		String fileName = null;
		String cd = getHeader(headers, CONTENT_DISPOSITION);
		if (cd.startsWith(FORM_DATA) || cd.startsWith(ATTACHMENT)) {
			ParameterParser parser = new ParameterParser();
			parser.setLowerCaseNames(true);
			// Parameter parser can handle null input
			Map params = parser.parse(cd, ';');
			if (params.containsKey("filename")) {
				fileName = (String) params.get("filename");
				if (fileName != null) {
					fileName = fileName.trim();
				} else {
					// Even if there is no value, the parameter is present, so we return an empty file name rather than no file name.
					fileName = "";
				}
			}
		}
		return fileName;
	}

	@SuppressWarnings("rawtypes")
	protected String getFieldName(Map headers) {
		String fieldName = null;
		String cd = getHeader(headers, CONTENT_DISPOSITION);
		if (cd != null && cd.startsWith(FORM_DATA)) {
			ParameterParser parser = new ParameterParser();
			parser.setLowerCaseNames(true);
			// Parameter parser can handle null input
			Map params = parser.parse(cd, ';');
			fieldName = (String) params.get("name");
			if (fieldName != null) {
				fieldName = fieldName.trim();
			}
		}
		return fieldName;
	}

	@SuppressWarnings("rawtypes")
	protected FileItem createItem(Map headers, boolean isFormField) {
		return getFileItemFactory().createItem(getFieldName(headers), getHeader(headers, CONTENT_TYPE), isFormField, getFileName(headers));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Map parseHeaders(String headerPart) {
		Map headers = new HashMap();
		char[] buffer = new char[MAX_HEADER_SIZE];
		boolean done = false;
		int j = 0;
		int i = 0;
		String header = null;
		String headerName = null;
		String headerValue = null;
		try {
			while (!done) {
				i = 0;
				// Copy a single line of characters into the buffer, omitting trailing CRLF.
				while (i < 2 || buffer[i - 2] != '\r' || buffer[i - 1] != '\n') {
					buffer[i++] = headerPart.charAt(j++);
				}
				header = new String(buffer, 0, i - 2);
				if (header.equals("")) {
					done = true;
				} else {
					if (header.indexOf(':') == -1) {
						// This header line is malformed, skip it.
						continue;
					}
					headerName = header.substring(0, header.indexOf(':')).trim().toLowerCase();
					headerValue = header.substring(header.indexOf(':') + 1).trim();
					if (getHeader(headers, headerName) != null) {
						// More that one heder of that name exists, append to the list.
						headers.put(headerName, getHeader(headers, headerName) + ',' + headerValue);
					} else {
						headers.put(headerName, headerValue);
					}
				}
			}
		} catch (IndexOutOfBoundsException e) {
			// Headers were malformed. continue with all that was parsed.
		}
		return headers;
	}

	@SuppressWarnings("rawtypes")
	protected final String getHeader(Map headers, String name) {
		return (String)headers.get(name.toLowerCase());
	}

	@SuppressWarnings("serial")
	public static class InvalidContentTypeException extends FileUploadException {
		public InvalidContentTypeException() {
			super();
		}
		public InvalidContentTypeException(String message) {
			super(message);
		}
	}

	@SuppressWarnings("serial")
	public static class UnknownSizeException extends FileUploadException {
		public UnknownSizeException() {
			super();
		}
		public UnknownSizeException(String message) {
			super(message);
		}
	}

	@SuppressWarnings("serial")
	public static class SizeLimitExceededException extends FileUploadException {
		public SizeLimitExceededException() {
			super();
		}
		public SizeLimitExceededException(String message) {
			super(message);
		}
	}
}
