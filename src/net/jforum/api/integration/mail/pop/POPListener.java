
package net.jforum.api.integration.mail.pop;

import java.util.Iterator;
import java.util.List;

import net.jforum.dao.DataAccessDriver;
import net.jforum.entities.MailIntegration;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class POPListener implements Job {

	private final static Logger logger = Logger.getLogger(POPListener.class);
	
	private static boolean working = false;
	protected POPConnector connector = new POPConnector();

	@SuppressWarnings("rawtypes")
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		if (!working) {
			try {
				working = true;
				List integrationList = DataAccessDriver.getInstance().newMailIntegrationDAO().findAll();
				POPParser parser = new POPParser();
				for (Iterator iter = integrationList.iterator(); iter.hasNext(); ) {
					MailIntegration integration = (MailIntegration)iter.next();
					connector.setMailIntegration(integration);
					try {
						logger.debug("Going to check " + integration);
						connector.openConnection();
						parser.parseMessages(connector);
						POPPostAction postAction = new POPPostAction();
						postAction.insertMessages(parser);
					} finally {
						connector.closeConnection();
					}
				}
			} finally {
				working = false;
			}
		} else {
			logger.debug("Already working. Leaving for now.");
		}
	}

	POPConnector getConnector() {
		return connector;
	}
}
