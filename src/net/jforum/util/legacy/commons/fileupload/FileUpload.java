
package net.jforum.util.legacy.commons.fileupload;

import javax.servlet.http.HttpServletRequest;

@SuppressWarnings("unused")
public class FileUpload extends FileUploadBase {

	private FileItemFactory fileItemFactory = null;

	public FileUpload() {
		super();
	}

	public FileUpload(FileItemFactory fileItemFactory) {
		super();
		this.fileItemFactory = fileItemFactory;
	}

	public FileItemFactory getFileItemFactory() {
		return fileItemFactory;
	}

	public void setFileItemFactory(FileItemFactory factory) {
		this.fileItemFactory = factory;
	}
}
