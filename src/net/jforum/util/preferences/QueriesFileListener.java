
package net.jforum.util.preferences;

import net.jforum.util.FileChangeListener;

import org.apache.log4j.Logger;

public class QueriesFileListener implements FileChangeListener {

	private final static Logger logger = Logger.getLogger(QueriesFileListener.class);

	public void fileChanged(String filename) {
		System.out.println("--> [QueriesFileListener.fileChanged] ......");
		logger.info("Reloading " + filename);
		SystemGlobals.loadQueries(filename);
		String driverQueries = SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_DRIVER);
		// Force reload of driver specific queries
		if (!filename.equals(driverQueries)) {
			SystemGlobals.loadQueries(driverQueries);
		}
	}
}
