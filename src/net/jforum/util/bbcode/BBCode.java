
package net.jforum.util.bbcode;

import java.io.Serializable;

@SuppressWarnings("serial")
public class BBCode implements Serializable {

	private String tagName = "";
	private String regex   = null;
	private String replace = null;

	private boolean removQuotes   = false;
	private boolean alwaysProcess = false;

	public BBCode() {

	}

	public BBCode(String tagName, String regex, String replace) {
		this.tagName = tagName;
		this.regex = regex;
		this.replace = replace;
	}

	public String getRegex() {
		return this.regex;
	}

	public String getReplace() {
		return this.replace;
	}

	public String getTagName() {
		return this.tagName;
	}

	public boolean removeQuotes() {
		return this.removQuotes;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public void setReplace(String replace) {
		this.replace = replace;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public void enableAlwaysProcess() {
		this.alwaysProcess = true;
	}

	public boolean alwaysProcess() {
		return this.alwaysProcess;
	}

	public void enableRemoveQuotes() {
		this.removQuotes = true;
	}
}
