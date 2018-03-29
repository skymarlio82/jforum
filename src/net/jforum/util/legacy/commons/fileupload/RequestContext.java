
package net.jforum.util.legacy.commons.fileupload;

import java.io.IOException;
import java.io.InputStream;

public interface RequestContext {

	String getContentType();
	int getContentLength();
	InputStream getInputStream() throws IOException;
}
