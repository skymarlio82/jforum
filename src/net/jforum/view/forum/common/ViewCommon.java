
package net.jforum.view.forum.common;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import net.jforum.JForumExecutionContext;
import net.jforum.context.RequestContext;
import net.jforum.entities.User;
import net.jforum.exceptions.ForumException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import freemarker.template.SimpleHash;

public final class ViewCommon {

	public static void contextToPagination(int start, int totalRecords, int recordsPerPage) {
		SimpleHash context = JForumExecutionContext.getTemplateContext();
		context.put("totalPages", new Double(Math.ceil((double)totalRecords/(double)recordsPerPage)));
		context.put("recordsPerPage", new Integer(recordsPerPage));
		context.put("totalRecords", new Integer(totalRecords));
		context.put("thisPage", new Double(Math.ceil((double)(start + 1)/(double)recordsPerPage)));
		context.put("start", new Integer(start));
	}

	public static String contextToLogin() {
		RequestContext request = JForumExecutionContext.getRequest();
		String uri = request.getRequestURI();
		String query = request.getQueryString();
		String returnPath = (query == null) ? uri : (uri + "?" + query);
		return contextToLogin(returnPath);
	}

	public static String contextToLogin(String returnPath) {
		JForumExecutionContext.getTemplateContext().put("returnPath", returnPath);
		if (ConfigKeys.TYPE_SSO.equals(SystemGlobals.getValue(ConfigKeys.AUTHENTICATION_TYPE))) {
			String redirect = SystemGlobals.getValue(ConfigKeys.SSO_REDIRECT);
			if (!StringUtils.isEmpty(redirect)) {
				URI redirectUri = URI.create(redirect);
				if (!redirectUri.isAbsolute()) {
					throw new ForumException("SSO redirect URL should start with a scheme");
				}
				try {
					returnPath = URLEncoder.encode(ViewCommon.getForumLink() + returnPath, "UTF-8");
				} catch (UnsupportedEncodingException e) {
				
				}
				if (redirect.indexOf('?') == -1) {
					redirect += "?";
				} else {
					redirect += "&";
				}
				redirect += "returnUrl=" + returnPath;
				JForumExecutionContext.setRedirect(redirect);
			}
		}
		return TemplateKeys.USER_LOGIN;
	}

	public static int getStartPage() {
		String s = JForumExecutionContext.getRequest().getParameter("start");
		int start = 0;
		if (StringUtils.isEmpty(s)) {
			start = 0;
		} else {
			start = Integer.parseInt(s);
			if (start < 0) {
				start = 0;
			}
		}
		return start;
	}

	public static String getForumLink() {
		String forumLink = SystemGlobals.getValue(ConfigKeys.FORUM_LINK);
		if (forumLink.charAt(forumLink.length() - 1) != '/') {
			forumLink += "/";
		}
		return forumLink;
	}

	public static String toUtf8String(String s) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ((c >= 0) && (c <= 255)) {
				sb.append(c);
			} else {
				byte[] b = null;
				try {
					b = Character.toString(c).getBytes("utf-8");
				} catch (Exception ex) {
					System.out.println(ex);
					b = new byte[0];
				}
				for (int j = 0; j < b.length; j++) {
					int k = b[j];
					if (k < 0) {
						k += 256;
					}
					sb.append('%').append(Integer.toHexString(k).toUpperCase());
				}
			}
		}
		return sb.toString();
	}

	public static String formatDate(Date date) {
		SimpleDateFormat df = new SimpleDateFormat(SystemGlobals.getValue(ConfigKeys.DATE_TIME_FORMAT));
		return df.format(date);
	}

	public static String espaceHtml(String contents) {
		StringBuffer sb = new StringBuffer(contents);
		replaceAll(sb, "<", "&lt");
		replaceAll(sb, ">", "&gt;");
		return sb.toString();
	}

	public static String replaceAll(StringBuffer sb, String what, String with) {
		int pos = sb.indexOf(what);
		while (pos > -1) {
			sb.replace(pos, pos + what.length(), with);
			pos = sb.indexOf(what);
		}
		return sb.toString();
	}

	public static String replaceAll(String contents, String what, String with) {
		return replaceAll(new StringBuffer(contents), what, with);
	}

	public static void prepareUserSignature(User u) {
		if (u.getSignature() != null) {
			StringBuffer sb = new StringBuffer(u.getSignature());
			replaceAll(sb, "\n", "<br />");
			u.setSignature(sb.toString());
			u.setSignature(PostCommon.prepareTextForDisplayExceptCodeTag(u.getSignature(), true, true));
		}
	}
}
