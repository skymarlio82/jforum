
package net.jforum;

import java.sql.Connection;

import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

public abstract class DBConnection {

	private final static Logger logger = Logger.getLogger(DBConnection.class);

	private static DBConnection instance = null;
	protected boolean isDatabaseUp       = false;

	public static boolean createInstance() {
		System.out.println("--> [DBConnection.createInstance] ......");
		try {
			System.out.println("DEBUG: ConfigKeys.DATABASE_CONNECTION_IMPLEMENTATION = " + SystemGlobals.getValue(ConfigKeys.DATABASE_CONNECTION_IMPLEMENTATION));
			instance = (DBConnection)Class.forName(SystemGlobals.getValue(ConfigKeys.DATABASE_CONNECTION_IMPLEMENTATION)).newInstance();
			System.out.println("INFOR: the instance of DBConnection is done ...");
		} catch (Exception e) {
			logger.warn("Error creating the database connection implementation instance. " + e);
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static DBConnection getImplementation() {
		return instance;
	}

	public boolean isDatabaseUp() {
		return isDatabaseUp;
	}

	public abstract void init() throws Exception;

	public abstract Connection getConnection();

	public abstract void releaseConnection(Connection conn);

	public abstract void realReleaseAllConnections() throws Exception;
}
