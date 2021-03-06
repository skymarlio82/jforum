
package net.jforum.api.integration.mail.pop;

import java.text.ParseException;

import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

public class POPJobStarter {

	private static Logger logger = Logger.getLogger(POPJobStarter.class);

	private static boolean isStarted = false;
	private static Scheduler scheduler = null;

	public static void startJob() throws SchedulerException {
		boolean isEnabled = SystemGlobals.getBoolValue(ConfigKeys.MAIL_POP3_INTEGRATION_ENABLED);
		if (!isStarted && isEnabled) {
			String filename = SystemGlobals.getValue(ConfigKeys.QUARTZ_CONFIG);
			String cronExpression = SystemGlobals.getValue("org.quartz.context.mailintegration.cron.expression");
			scheduler = new StdSchedulerFactory(filename).getScheduler();
			Trigger trigger = null;
			try {
				trigger = new CronTrigger(POPListener.class.getName(), "pop3Integration", cronExpression);
				logger.info("Starting POP3 integration expression " + cronExpression);
				scheduler.scheduleJob(new JobDetail(POPListener.class.getName(), "pop3Integration", POPListener.class), trigger);
				scheduler.start();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		isStarted = true;
	}
}
