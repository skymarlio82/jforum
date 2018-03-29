
package net.jforum.entities;

public class MailIntegration {

	private int forumId        = 0;
	private int popPort        = 0;
	private boolean ssl        = false;
	private String forumEmail  = null;
	private String popHost     = null;
	private String popUsername = null;
	private String popPassword = null;

	public int getForumId() {
		return this.forumId;
	}

	public String getPopHost() {
		return this.popHost;
	}

	public String getPopPassword() {
		return this.popPassword;
	}

	public int getPopPort() {
		return this.popPort;
	}

	public String getPopUsername() {
		return this.popUsername;
	}

	public void setForumId(int forumId) {
		this.forumId = forumId;
	}

	public void setPopHost(String popHost) {
		this.popHost = popHost;
	}

	public void setPopPassword(String popPassword) {
		this.popPassword = popPassword;
	}

	public void setPopPort(int popPort) {
		this.popPort = popPort;
	}

	public void setPopUsername(String popUsername) {
		this.popUsername = popUsername;
	}

	public String getForumEmail() {
		return this.forumEmail;
	}

	public void setForumEmail(String forumEmail) {
		this.forumEmail = forumEmail;
	}

	public void setSSL(boolean ssl) {
		this.ssl = ssl;
	}

	public boolean isSSL() {
		return this.ssl;
	}

	public String toString() {
		return new StringBuffer().append('[').append("email=").append(this.forumEmail).append(", host=").append(popHost).append(", port=").append(popPort).append(", ssl=").append(ssl).append(']').toString();
	}
}
