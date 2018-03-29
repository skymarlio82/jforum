
package net.jforum.util.legacy.commons.fileupload.disk;

import java.io.File;

import net.jforum.util.legacy.commons.fileupload.FileItem;
import net.jforum.util.legacy.commons.fileupload.FileItemFactory;

public class DiskFileItemFactory implements FileItemFactory {

	public final static int DEFAULT_SIZE_THRESHOLD = 1024*10;

	private int sizeThreshold = DEFAULT_SIZE_THRESHOLD;
	
	private File repository = null;

	public DiskFileItemFactory() {
		
	}

	public DiskFileItemFactory(int sizeThreshold, File repository) {
		this.sizeThreshold = sizeThreshold;
		this.repository = repository;
	}

	public File getRepository() {
		return repository;
	}

	public void setRepository(File repository) {
		this.repository = repository;
	}

	public int getSizeThreshold() {
		return sizeThreshold;
	}

	public void setSizeThreshold(int sizeThreshold) {
		this.sizeThreshold = sizeThreshold;
	}

	public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
		return new DiskFileItem(fieldName, contentType, isFormField, fileName, sizeThreshold, repository);
	}
}
