
package net.jforum.entities;

import java.io.Serializable;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

@SuppressWarnings("serial")
public class Banlist implements Serializable {

	private int id = 0;
	private int userId = 0;
	private String ip = null;
	private String email = null;

	public int getId() {
		return id;
	}

	public int getUserId() {
		return userId;
	}

	public String getIp() {
		return ip;
	}

	public String getEmail() {
		return email;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean matches(Banlist b) {
		boolean status = false;
		if (matchesUserId(b) || matchesEmail(b)) {
			status = true;
		} else if (!StringUtils.isEmpty(b.getIp()) && !StringUtils.isEmpty(getIp())) {
			if (b.getIp().equalsIgnoreCase(getIp())) {
				status = true;
			} else {
				status = matchIp(b);
			}
		}
		return status;
	}

	private boolean matchesEmail(Banlist b) {
		return (!StringUtils.isEmpty(b.getEmail()) && b.getEmail().equals(getEmail()));
	}

	private boolean matchesUserId(Banlist b) {
		return b.getUserId() > 0 && getUserId() > 0 && b.getUserId() == getUserId();
	}

	private boolean matchIp(Banlist b) {
		boolean status = false;
		StringTokenizer userToken = new StringTokenizer(b.getIp(), ".");
		StringTokenizer thisToken = new StringTokenizer(getIp(), ".");
		if (userToken.countTokens() == thisToken.countTokens()) {
			String[] userValues = tokenizerAsArray(userToken);
			String[] thisValues = tokenizerAsArray(thisToken);
			status = compareIpValues(userValues, thisValues);
		}
		return status;
	}

	private boolean compareIpValues(String[] userValues, String[] thisValues) {
		boolean helperStatus = true;
		boolean onlyStars = true;
		for (int i = 0; i < thisValues.length; i++) {
			if (thisValues[i].charAt(0) != '*') {
				onlyStars = false;
				if (!thisValues[i].equals(userValues[i])) {
					helperStatus = false;
				}
			}
		}
		return helperStatus && !onlyStars;
	}

	private String[] tokenizerAsArray(StringTokenizer token) {
		String[] values = new String[token.countTokens()];
		for (int i = 0; token.hasMoreTokens(); i++) {
			values[i] = token.nextToken();
		}
		return values;
	}
}
