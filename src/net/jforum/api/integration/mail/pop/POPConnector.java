
package net.jforum.api.integration.mail.pop;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Flags.Flag;

import net.jforum.entities.MailIntegration;
import net.jforum.exceptions.MailException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

public class POPConnector {

	private MailIntegration mailIntegration = null;
	private Store store                     = null;
	private Folder folder                   = null;
	private Message[] messages              = null;

	public void setMailIntegration(MailIntegration mailIntegration) {
		this.mailIntegration = mailIntegration;
	}

	public Message[] listMessages() {
		try {
			messages = folder.getMessages();
			return messages;
		} catch (Exception e) {
			throw new MailException(e);
		}
	}

	public void openConnection() {
		try {
			Session session = Session.getDefaultInstance(new Properties());
			store = session.getStore(mailIntegration.isSSL() ? "pop3s" : "pop3");
			store.connect(mailIntegration.getPopHost(), mailIntegration.getPopPort(), mailIntegration.getPopUsername(), mailIntegration.getPopPassword());
			folder = store.getFolder("INBOX");
			if (folder == null) {
				throw new Exception("No Inbox");
			}
			folder.open(Folder.READ_WRITE);
		} catch (Exception e) {
			throw new MailException(e);
		}
	}

	public void closeConnection() {
		boolean deleteMessages = !SystemGlobals.getBoolValue(ConfigKeys.MAIL_POP3_DEBUG_KEEP_MESSAGES);
		closeConnection(deleteMessages);
	}

	public void closeConnection(boolean deleteAll) {
		if (deleteAll) {
			markAllMessagesAsDeleted();
		}
		if (folder != null) {
			try {
				folder.close(false);
			} catch (Exception e) {
				
			}
		}
		if (store != null) {
			try {
				store.close();
			} catch (Exception e) {
				
			}
		}
	}

	private void markAllMessagesAsDeleted() {
		try {
			if (messages != null) {
				for (int i = 0; i < messages.length; i++) {
					messages[i].setFlag(Flag.DELETED, true);
				}
			}
		} catch (Exception e) {
			throw new MailException(e);
		}
	}
}
