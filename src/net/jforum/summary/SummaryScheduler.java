
package net.jforum.summary;

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

public class SummaryScheduler {

	private static Logger logger = Logger.getLogger(SummaryScheduler.class);

	private static Scheduler scheduler = null;
	private static boolean isStarted   = false;

	public static void startJob() throws SchedulerException {
		System.out.println("--> [SummaryScheduler.startJob] ......");
		boolean isEnabled = SystemGlobals.getBoolValue(ConfigKeys.SUMMARY_IS_ENABLED);
		System.out.println("DEBUG: isStarted = " + isStarted + ", ConfigKeys.SUMMARY_IS_ENABLED = " + isEnabled);
		if (!isStarted && isEnabled) {
			String filename = SystemGlobals.getValue(ConfigKeys.QUARTZ_CONFIG);
			System.out.println("DEBUG: ConfigKeys.QUARTZ_CONFIG = " + filename);
			String cronExpression = SystemGlobals.getValue("org.quartz.context.summary.cron.expression");
			System.out.println("DEBUG: org.quartz.context.summary.cron.expression = " + cronExpression);
			scheduler = new StdSchedulerFactory(filename).getScheduler();
			Trigger trigger = null;
			try {
				trigger = new CronTrigger(SummaryJob.class.getName(), "summaryJob", cronExpression);
				logger.info("Starting quartz summary expression " + cronExpression);
				scheduler.scheduleJob(new JobDetail(SummaryJob.class.getName(), "summaryJob", SummaryJob.class), trigger);
				scheduler.start();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		isStarted = true;
	}
}
