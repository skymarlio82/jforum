
package net.jforum.api.integration.mail.pop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;

import net.jforum.exceptions.MailException;

public class POPMessage {

	private static final String IN_REPLY_TO = "In-Reply-To";
	private static final String REFERENCES  = "References";

	private String subject         = null;
	private Object message         = null;
	private String messageContents = null;
	private String sender          = null;
	private String replyTo         = null;
	private String references      = null;
	private String inReplyTo       = null;
	private String contentType     = null;
	private String listEmail       = null;
	private Date sendDate          = null;
	@SuppressWarnings("rawtypes")
	private Map headers            = null;

	public POPMessage(Message message) {
		extract(message);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void extract(Message message) {
		try {
			subject = message.getSubject();
			this.message = message.getContent();
			contentType = message.getContentType();
			sender = ((InternetAddress)message.getFrom()[0]).getAddress();
			listEmail = ((InternetAddress)message.getAllRecipients()[0]).getAddress();
			sendDate = message.getSentDate();
			if (message.getReplyTo().length > 0) {
				replyTo = ((InternetAddress)message.getReplyTo()[0]).getAddress();
			} else {
				replyTo = sender;
			}
			headers = new HashMap();
			for (Enumeration e = message.getAllHeaders(); e.hasMoreElements(); ) {
				Header header = (Header) e.nextElement();
				headers.put(header.getName(), header.getValue());
			}
			if (headers.containsKey(IN_REPLY_TO)) {
				inReplyTo = headers.get(IN_REPLY_TO).toString();
			}
			if (headers.containsKey(REFERENCES)) {
				references = headers.get(REFERENCES).toString();
			}
			extractMessageContents(message);
		} catch (Exception e) {

		}
	}

	private void extractMessageContents(Message m) throws MessagingException {
		Part messagePart = m;
		if (message instanceof Multipart) {
			messagePart = ((Multipart)message).getBodyPart(0);
		}
		if (contentType.startsWith("text/html") || contentType.startsWith("text/plain")) {
			InputStream is = null;
			BufferedReader reader = null;
			try {
				is = messagePart.getInputStream();
				is.reset();
				reader = new BufferedReader(new InputStreamReader(is));
				StringBuffer sb = new StringBuffer(512);
				int c = 0;
				char[] ch = new char[2048];
				while ((c = reader.read(ch)) != -1) {
					sb.append(ch, 0, c);
				}
				messageContents = sb.toString();
			} catch (IOException e) {
				throw new MailException(e);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (Exception e) {
						
					}
				}
				if (is != null) {
					try {
						is.close();
					} catch (Exception e) {
						
					}
				}
			}
		}
	}

	public String getListEmail() {
		return this.listEmail;
	}

	public String getContentType() {
		return this.contentType;
	}

	@SuppressWarnings("rawtypes")
	public Map getHeaders() {
		return this.headers;
	}

	public String getInReplyTo() {
		return this.inReplyTo;
	}

	public String getMessage() {
		return this.messageContents;
	}

	public String getReferences() {
		return this.references;
	}

	public String getReplyTo() {
		return this.replyTo;
	}

	public Date getSendDate() {
		return this.sendDate;
	}

	public String getSender() {
		return this.sender;
	}

	public String getSubject() {
		return this.subject;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@SuppressWarnings("rawtypes")
	public void setHeaders(Map headers) {
		this.headers = headers;
	}

	public void setInReplyTo(String inReplyTo) {
		this.inReplyTo = inReplyTo;
	}

	public void setMessage(Object message) {
		this.message = message;
	}

	public void setReferences(String references) {
		this.references = references;
	}

	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	public void setSendDate(Date sendDate) {
		this.sendDate = sendDate;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String toString() {
		return new StringBuffer().append('[').append(", subject=").append(subject).append(", sender=").append(sender).append(", replyTo=").append(replyTo).append(", references=").append(references).append(", inReplyTo=").append(inReplyTo).append(", contentType=").append(contentType).append(", date=").append(sendDate).append(", content=").append(messageContents).append(", headers=").append(headers).append(']').toString();
	}
}
