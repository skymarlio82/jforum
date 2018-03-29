
package net.jforum.util.legacy.commons.fileupload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

public class MultipartStream {

	public final static byte CR   = 0x0D;
	public final static byte LF   = 0x0A;
	public final static byte DASH = 0x2D;

	public final static int HEADER_PART_SIZE_MAX    = 1024*10;
	protected final static int DEFAULT_BUFSIZE      = 1024*4;
	protected final static byte[] HEADER_SEPARATOR  = { CR, LF, CR, LF };
	protected final static byte[] FIELD_SEPARATOR   = { CR, LF };
	protected final static byte[] STREAM_TERMINATOR = { DASH, DASH };

	private InputStream input     = null;
	private byte[] boundary       = null;
	private byte[] buffer         = null;
	private String headerEncoding = null;
	private int boundaryLength    = 0;
	private int keepRegion        = 0;
	private int bufSize           = 0;
	private int head              = 0;
	private int tail              = 0;

	public MultipartStream() {
		
	}

	public MultipartStream(InputStream input, byte[] boundary, int bufSize) {
		this.input = input;
		this.bufSize = bufSize;
		this.buffer = new byte[bufSize];
		// We prepend CR/LF to the boundary to chop trailing CR/LF from body-data tokens.
		this.boundary = new byte[boundary.length + 4];
		this.boundaryLength = boundary.length + 4;
		this.keepRegion = boundary.length + 3;
		this.boundary[0] = CR;
		this.boundary[1] = LF;
		this.boundary[2] = DASH;
		this.boundary[3] = DASH;
		System.arraycopy(boundary, 0, this.boundary, 4, boundary.length);
		head = 0;
		tail = 0;
	}

	public MultipartStream(InputStream input, byte[] boundary) {
		this(input, boundary, DEFAULT_BUFSIZE);
	}

	public String getHeaderEncoding() {
		return headerEncoding;
	}

	public void setHeaderEncoding(String encoding) {
		headerEncoding = encoding;
	}

	public byte readByte() throws IOException {
		// Buffer depleted ?
		if (head == tail) {
			head = 0;
			// Refill.
			tail = input.read(buffer, head, bufSize);
			if (tail == -1) {
				// No more data available.
				throw new IOException("No more data is available");
			}
		}
		return buffer[head++];
	}

	public boolean readBoundary() throws MalformedStreamException {
		byte[] marker = new byte[2];
		boolean nextChunk = false;
		head += boundaryLength;
		try {
			marker[0] = readByte();
			if (marker[0] == LF) {
				// Work around IE5 MAC bug with input type=image. Because the boundary delimiter, not including the trailing CRLF, must not appear within any file (RFC 2046, section 5.1.1), we know the missing CR is due to a buggy browser rather than a file containing something similar to a boundary.
				return true;
			}
			marker[1] = readByte();
			if (arrayequals(marker, STREAM_TERMINATOR, 2)) {
				nextChunk = false;
			} else if (arrayequals(marker, FIELD_SEPARATOR, 2)) {
				nextChunk = true;
			} else {
				throw new MalformedStreamException("Unexpected characters follow a boundary");
			}
		} catch (IOException e) {
			throw new MalformedStreamException("Stream ended unexpectedly");
		}
		return nextChunk;
	}

	public void setBoundary(byte[] boundary) throws IllegalBoundaryException {
		if (boundary.length != boundaryLength - 4) {
			throw new IllegalBoundaryException("The length of a boundary token can not be changed");
		}
		System.arraycopy(boundary, 0, this.boundary, 4, boundary.length);
	}

	public String readHeaders() throws MalformedStreamException {
		int i = 0;
		byte[] b = new byte[1];
		// to support multi-byte characters
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int sizeMax = HEADER_PART_SIZE_MAX;
		int size = 0;
		while (i < 4) {
			try {
				b[0] = readByte();
			} catch (IOException e) {
				throw new MalformedStreamException("Stream ended unexpectedly");
			}
			size++;
			if (b[0] == HEADER_SEPARATOR[i]) {
				i++;
			} else {
				i = 0;
			}
			if (size <= sizeMax) {
				baos.write(b[0]);
			}
		}
		String headers = null;
		if (headerEncoding != null) {
			try {
				headers = baos.toString(headerEncoding);
			} catch (UnsupportedEncodingException e) {
				// Fall back to platform default if specified encoding is not supported.
				headers = baos.toString();
			}
		} else {
			headers = baos.toString();
		}
		return headers;
	}

	public int readBodyData(OutputStream output) throws MalformedStreamException, IOException {
		boolean done = false;
		int pad = 0;
		int pos = 0;
		int bytesRead = 0;
		int total = 0;
		while (!done) {
			// Is boundary token present somewere in the buffer?
			pos = findSeparator();
			if (pos != -1) {
				// Write the rest of the data before the boundary.
				output.write(buffer, head, pos - head);
				total += pos - head;
				head = pos;
				done = true;
			} else {
				// Determine how much data should be kept in the buffer.
				if (tail - head > keepRegion) {
					pad = keepRegion;
				} else {
					pad = tail - head;
				}
				// Write out the data belonging to the body-data.
				output.write(buffer, head, tail - head - pad);
				// Move the data to the beginning of the buffer.
				total += tail - head - pad;
				System.arraycopy(buffer, tail - pad, buffer, 0, pad);
				// Refill buffer with new data.
				head = 0;
				bytesRead = input.read(buffer, pad, bufSize - pad);
				// [pprrrrrrr]
				if (bytesRead != -1) {
					tail = pad + bytesRead;
				} else {
					// The last pad amount is left in the buffer. Boundary can't be in there so write out the data you have and signal an error condition.
					output.write(buffer, 0, pad);
					output.flush();
					total += pad;
					throw new MalformedStreamException("Stream ended unexpectedly");
				}
			}
		}
		output.flush();
		return total;
	}

	public int discardBodyData() throws MalformedStreamException, IOException {
		boolean done = false;
		int pad = 0;
		int pos = 0;
		int bytesRead = 0;
		int total = 0;
		while (!done) {
			// Is boundary token present somewere in the buffer?
			pos = findSeparator();
			if (pos != -1) {
				// Write the rest of the data before the boundary.
				total += pos - head;
				head = pos;
				done = true;
			} else {
				// Determine how much data should be kept in the buffer.
				if (tail - head > keepRegion) {
					pad = keepRegion;
				} else {
					pad = tail - head;
				}
				total += tail - head - pad;
				// Move the data to the beginning of the buffer.
				System.arraycopy(buffer, tail - pad, buffer, 0, pad);
				// Refill buffer with new data.
				head = 0;
				bytesRead = input.read(buffer, pad, bufSize - pad);
				// [pprrrrrrr]
				if (bytesRead != -1) {
					tail = pad + bytesRead;
				} else {
					// The last pad amount is left in the buffer. Boundary can't be in there so signal an error condition.
					total += pad;
					throw new MalformedStreamException("Stream ended unexpectedly");
				}
			}
		}
		return total;
	}

	public boolean skipPreamble() throws IOException {
		// First delimiter may be not preceeded with a CRLF, so skip the first 2 bytes in the 'boundary'
		System.arraycopy(boundary, 2, boundary, 0, boundary.length - 2);
		boundaryLength = boundary.length - 2;
		try {
			// Discard all data up to the delimiter.
			discardBodyData();
			// Read boundary - if success, the stream contains an encapsulation.
			return readBoundary();
		} catch (MalformedStreamException e) {
			return false;
		} finally {
			// Restore delimiter.
			System.arraycopy(boundary, 0, boundary, 2, boundary.length - 2);
			boundaryLength = boundary.length;
			boundary[0] = CR;
			boundary[1] = LF;
		}
	}

	public static boolean arrayequals(byte[] a, byte[] b, int count) {
		for (int i = 0; i < count; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}
		return true;
	}

	protected int findByte(byte value, int pos) {
		for (int i = pos; i < tail; i++) {
			if (buffer[i] == value) {
				return i;
			}
		}
		return -1;
	}

	protected int findSeparator() {
		int first = 0;
		int match = 0;
		int maxpos = tail - boundaryLength;
		for (first = head; (first <= maxpos) && (match != boundaryLength); first++) {
			first = findByte(boundary[0], first);
			if (first == -1 || (first > maxpos)) {
				return -1;
			}
			for (match = 1; match < boundaryLength; match++) {
				if (buffer[first + match] != boundary[match]) {
					break;
				}
			}
		}
		if (match == boundaryLength) {
			return first - 1;
		}
		return -1;
	}

	public String toString() {
		StringBuffer sbTemp = new StringBuffer();
		sbTemp.append("boundary='").append(String.valueOf(boundary)).append("'\nbufSize=").append(bufSize);
		return sbTemp.toString();
	}

	@SuppressWarnings("serial")
	public static class MalformedStreamException extends IOException implements Serializable {
		public MalformedStreamException() {
			super();
		}
		public MalformedStreamException(String message) {
			super(message);
		}
	}

	@SuppressWarnings("serial")
	public static class IllegalBoundaryException extends IOException implements Serializable {
		public IllegalBoundaryException() {
			super();
		}
		public IllegalBoundaryException(String message) {
			super(message);
		}
	}
}
