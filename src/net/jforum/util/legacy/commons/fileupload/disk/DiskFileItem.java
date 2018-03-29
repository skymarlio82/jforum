
package net.jforum.util.legacy.commons.fileupload.disk;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import net.jforum.util.legacy.commons.fileupload.FileItem;
import net.jforum.util.legacy.commons.fileupload.FileUploadException;
import net.jforum.util.legacy.commons.fileupload.ParameterParser;

import org.apache.commons.io.FileCleaner;
import org.apache.commons.io.output.DeferredFileOutputStream;

@SuppressWarnings("serial")
public class DiskFileItem implements FileItem {

	public final static String DEFAULT_CHARSET = "ISO-8859-1";
	private final static int WRITE_BUFFER_SIZE = 1024*2;

	private static int counter = 0;
	private String fieldName = null;
	private String contentType = null;
	private boolean isFormField = false;
	private String fileName = null;
	private int sizeThreshold = 0;
	private File repository = null;
	private byte[] cachedContent = null;
	private DeferredFileOutputStream dfos = null;

	public DiskFileItem(String fieldName, String contentType, boolean isFormField, String fileName, int sizeThreshold, File repository) {
		this.fieldName = fieldName;
		this.contentType = contentType;
		this.isFormField = isFormField;
		this.fileName = fileName;
		this.sizeThreshold = sizeThreshold;
		this.repository = repository;
	}

	public InputStream getInputStream() throws IOException {
		if (!dfos.isInMemory()) {
			return new FileInputStream(dfos.getFile());
		}
		if (cachedContent == null) {
			cachedContent = dfos.getData();
		}
		return new ByteArrayInputStream(cachedContent);
	}

	public String getContentType() {
		return contentType;
	}

	@SuppressWarnings("rawtypes")
	public String getCharSet() {
		ParameterParser parser = new ParameterParser();
		parser.setLowerCaseNames(true);
		// Parameter parser can handle null input
		Map params = parser.parse(getContentType(), ';');
		return (String)params.get("charset");
	}

	public String getName() {
		return fileName;
	}

	public boolean isInMemory() {
		return dfos.isInMemory();
	}

	public long getSize() {
		if (cachedContent != null) {
			return cachedContent.length;
		} else if (dfos.isInMemory()) {
			return dfos.getData().length;
		} else {
			return dfos.getFile().length();
		}
	}

	public byte[] get() {
		if (dfos.isInMemory()) {
			if (cachedContent == null) {
				cachedContent = dfos.getData();
			}
			return cachedContent;
		}
		byte[] fileData = new byte[(int) getSize()];
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(dfos.getFile());
			fis.read(fileData);
		} catch (IOException e) {
			fileData = null;
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return fileData;
	}

	public String getString(final String charset) throws UnsupportedEncodingException {
		return new String(get(), charset);
	}

	public String getString() {
		byte[] rawdata = get();
		String charset = getCharSet();
		if (charset == null) {
			charset = DEFAULT_CHARSET;
		}
		try {
			return new String(rawdata, charset);
		} catch (UnsupportedEncodingException e) {
			return new String(rawdata);
		}
	}

	public void write(File file) throws Exception {
		if (isInMemory()) {
			FileOutputStream fout = null;
			try {
				fout = new FileOutputStream(file);
				fout.write(get());
			} finally {
				if (fout != null) {
					fout.close();
				}
			}
		} else {
			File outputFile = getStoreLocation();
			if (outputFile != null) {
				// The uploaded file is being stored on disk in a temporary location so move it to the desired file.
				if (!outputFile.renameTo(file)) {
					BufferedInputStream in = null;
					BufferedOutputStream out = null;
					try {
						in = new BufferedInputStream(new FileInputStream(outputFile));
						out = new BufferedOutputStream(new FileOutputStream(file));
						byte[] bytes = new byte[WRITE_BUFFER_SIZE];
						int s = 0;
						while ((s = in.read(bytes)) != -1) {
							out.write(bytes, 0, s);
						}
					} finally {
						if (in != null) {
							try {
								in.close();
							} catch (IOException e) {
								// ignore
							}
						}
						if (out != null) {
							try {
								out.close();
							} catch (IOException e) {
								// ignore
							}
						}
					}
				}
			} else {
				// For whatever reason we cannot write the file to disk.
				throw new FileUploadException("Cannot write uploaded file to disk!");
			}
		}
	}

	public void delete() {
		cachedContent = null;
		File outputFile = getStoreLocation();
		if (outputFile != null && outputFile.exists()) {
			outputFile.delete();
		}
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public boolean isFormField() {
		return isFormField;
	}

	public void setFormField(boolean state) {
		isFormField = state;
	}

	public OutputStream getOutputStream() throws IOException {
		if (dfos == null) {
			File outputFile = getTempFile();
			dfos = new DeferredFileOutputStream(sizeThreshold, outputFile);
		}
		return dfos;
	}

	public File getStoreLocation() {
		return dfos.getFile();
	}

	protected void finalize() {
		File outputFile = dfos.getFile();
		if (outputFile != null && outputFile.exists()) {
			outputFile.delete();
		}
	}

	protected File getTempFile() {
		File tempDir = repository;
		if (tempDir == null) {
			tempDir = new File(System.getProperty("java.io.tmpdir"));
		}
		String fileName = "upload_" + getUniqueId() + ".tmp";
		File f = new File(tempDir, fileName);
		FileCleaner.track(f, this);
		return f;
	}

	private static String getUniqueId() {
		int current;
		synchronized (DiskFileItem.class) {
			current = counter++;
		}
		String id = Integer.toString(current);
		// If you manage to get more than 100 million of ids, you'll start getting ids longer than 8 characters.
		if (current < 100000000) {
			id = ("00000000" + id).substring(id.length());
		}
		return id;
	}

	public String toString() {
		return "name=" + getName() + ", StoreLocation=" + String.valueOf(getStoreLocation()) + ", size=" + getSize() + "bytes, " + "isFormField=" + isFormField() + ", FieldName=" + fieldName;
	}
}
