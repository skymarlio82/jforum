
package net.jforum.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

public class FileMonitor {

	private static Logger logger = Logger.getLogger(FileMonitor.class);

	private final static FileMonitor instance = new FileMonitor();

	private Timer timer      = null;
	@SuppressWarnings("rawtypes")
	private Map timerEntries = null;

	@SuppressWarnings("rawtypes")
	private FileMonitor() {
		timerEntries = new HashMap();
		timer = new Timer(true);
	}

	public static FileMonitor getInstance() {
		return instance;
	}

	@SuppressWarnings("unchecked")
	public void addFileChangeListener(FileChangeListener listener, String filename, long period) {
		System.out.println("--> [FileMonitor.addFileChangeListener] ......");
		removeFileChangeListener(filename);
		logger.info("Watching " + filename);
		FileMonitorTask task = new FileMonitorTask(listener, filename);
		timerEntries.put(filename, task);
		timer.schedule(task, period, period);
	}

	public void removeFileChangeListener(String filename) {
		System.out.println("--> [FileMonitor.removeFileChangeListener] ......");
		FileMonitorTask task = (FileMonitorTask)timerEntries.remove(filename);
		if (task != null) {
			task.cancel();
		}
	}

	private static class FileMonitorTask extends TimerTask {

		private FileChangeListener listener = null;
		private String filename             = null;
		private File monitoredFile          = null;
		private long lastModified           = 0;

		public FileMonitorTask(FileChangeListener listener, String filename) {
			this.listener = listener;
			this.filename = filename;
			monitoredFile = new File(filename);
			if (!monitoredFile.exists()) {
				return;
			}
			lastModified = monitoredFile.lastModified();
		}

		public void run() {
			long latestChange = monitoredFile.lastModified();
			if (this.lastModified != latestChange) {
				this.lastModified = latestChange;
				listener.fileChanged(filename);
			}
		}
	}
}
