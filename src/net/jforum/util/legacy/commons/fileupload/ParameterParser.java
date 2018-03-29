
package net.jforum.util.legacy.commons.fileupload;

import java.util.HashMap;
import java.util.Map;

public class ParameterParser {

	private char[] chars = null;
	private int pos = 0;
	private int len = 0;
	private int i1  = 0;
	private int i2  = 0;

	private boolean lowerCaseNames = false;

	public ParameterParser() {
		super();
	}

	private boolean hasChar() {
		return pos < len;
	}

	private String getToken(boolean quoted) {
		// Trim leading white spaces
		while ((i1 < i2) && Character.isWhitespace(chars[i1])) {
			i1++;
		}
		// Trim trailing white spaces
		while ((i2 > i1) && Character.isWhitespace(chars[i2 - 1])) {
			i2--;
		}
		// Strip away quotation marks if necessary
		if (quoted) {
			if (((i2 - i1) >= 2) && (chars[i1] == '"') && (chars[i2 - 1] == '"')) {
				i1++;
				i2--;
			}
		}
		String result = null;
		if (i2 > i1) {
			result = new String(chars, i1, i2 - i1);
		}
		return result;
	}

	private boolean isOneOf(char ch, final char[] charray) {
		boolean result = false;
		for (int i = 0; i < charray.length; i++) {
			if (ch == charray[i]) {
				result = true;
				break;
			}
		}
		return result;
	}

	private String parseToken(final char[] terminators) {
		char ch;
		i1 = pos;
		i2 = pos;
		while (hasChar()) {
			ch = chars[pos];
			if (isOneOf(ch, terminators)) {
				break;
			}
			i2++;
			pos++;
		}
		return getToken(false);
	}

	private String parseQuotedToken(final char[] terminators) {
		char ch;
		i1 = pos;
		i2 = pos;
		boolean quoted = false;
		boolean charEscaped = false;
		while (hasChar()) {
			ch = chars[pos];
			if (!quoted && isOneOf(ch, terminators)) {
				break;
			}
			if (!charEscaped && ch == '"') {
				quoted = !quoted;
			}
			charEscaped = (!charEscaped && ch == '\\');
			i2++;
			pos++;
		}
		return getToken(true);
	}

	public boolean isLowerCaseNames() {
		return lowerCaseNames;
	}

	public void setLowerCaseNames(boolean b) {
		lowerCaseNames = b;
	}

	@SuppressWarnings("rawtypes")
	public Map parse(final String str, char separator) {
		if (str == null) {
			return new HashMap();
		}
		return parse(str.toCharArray(), separator);
	}

	@SuppressWarnings("rawtypes")
	public Map parse(final char[] chars, char separator) {
		if (chars == null) {
			return new HashMap();
		}
		return parse(chars, 0, chars.length, separator);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map parse(final char[] chars, int offset, int length, char separator) {
		if (chars == null) {
			return new HashMap();
		}
		HashMap params = new HashMap();
		this.chars = chars;
		this.pos = offset;
		this.len = length;
		String paramName = null;
		String paramValue = null;
		while (hasChar()) {
			paramName = parseToken(new char[] { '=', separator });
			paramValue = null;
			if (hasChar() && (chars[pos] == '=')) {
				pos++; // skip '='
				paramValue = parseQuotedToken(new char[] { separator });
			}
			if (hasChar() && (chars[pos] == separator)) {
				pos++; // skip separator
			}
			if ((paramName != null) && (paramName.length() > 0)) {
				if (lowerCaseNames) {
					paramName = paramName.toLowerCase();
				}
				System.out.println("DEBUG: paramName = " + paramName + ", paramValue = " + paramValue + " in the contentType");
				params.put(paramName, paramValue);
			}
		}
		return params;
	}
}
