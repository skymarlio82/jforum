
package net.jforum.api.integration.mail.pop;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Message;

import org.apache.log4j.Logger;

public class POPParser {

	private static Logger logger = Logger.getLogger(POPParser.class);

	@SuppressWarnings("rawtypes")
	private List messages = new ArrayList();

	@SuppressWarnings("unchecked")
	public void parseMessages(POPConnector connector) {
		Message[] connectorMessages = connector.listMessages();
		for (int i = 0; i < connectorMessages.length; i++) {
			POPMessage message = new POPMessage(connectorMessages[i]);
			messages.add(message);
			logger.debug("Retrieved message " + message);
		}
	}

	@SuppressWarnings("rawtypes")
	public List getMessages() {
		return messages;
	}
}
