
package net.jforum;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.log4j.Logger;

public class ForumSessionListener implements HttpSessionListener {

	private final static Logger logger = Logger.getLogger(ForumSessionListener.class);

	public void sessionCreated(HttpSessionEvent event) {
		
	}

	public void sessionDestroyed(HttpSessionEvent event) {
		HttpSession session = event.getSession();
		if (session == null) {
			return;
		}
		String sessionId = session.getId();
		try {
			SessionFacade.storeSessionData(sessionId);
		} catch (Exception e) {
			logger.warn(e);
		}
		SessionFacade.remove(sessionId);
	}
}
