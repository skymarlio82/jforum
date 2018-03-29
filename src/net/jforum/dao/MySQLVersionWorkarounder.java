
package net.jforum.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Properties;

import net.jforum.ConfigLoader;
import net.jforum.dao.mysql.MySQL323DataAccessDriver;
import net.jforum.dao.mysql.MysqlDataAccessDriver;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

public class MySQLVersionWorkarounder {

	private static Logger logger = Logger.getLogger(MySQLVersionWorkarounder.class);

	private final static String MYSQL_323_DATA_ACCESS_DRIVER = MySQL323DataAccessDriver.class.getName();
	private final static String MYSQL_DATA_ACCESS_DRIVER     = MysqlDataAccessDriver.class.getName();

    public void handleWorkarounds(Connection c) {
    	System.out.println("--> [MySQLVersionWorkarounder.handleWorkarounds] ......");
		if (c == null) {
			logger.warn("Cannot work with a null connection");
			return;
    	}
    	if (!"mysql".equals(SystemGlobals.getValue(ConfigKeys.DATABASE_DRIVER_NAME))) {
    		return;
    	}
    	try {
    		DatabaseMetaData meta = c.getMetaData();
    		logger.debug("MySQL Version: " + meta.getDatabaseProductVersion());
    		int major = meta.getDatabaseMajorVersion();
    		int minor = meta.getDatabaseMinorVersion();
    		System.out.println("DEBUG: MySQL major version = " + major + ", minor version = " + minor);
    		if (major == 3 && minor == 23) {
    			handleMySql323();
    		} else if (major == 4 && minor == 0) {
    			handleMySql40x();
    		} else if (major > 4 || (major == 4 && minor > 0)) {
    			handleMySql41xPlus();
    		}
    	} catch (Exception e) {
    		logger.error(e.toString(), e);
    	}
	}

	private void handleMySql323() throws Exception {
		ensureDaoClassIsCorrect(MYSQL_323_DATA_ACCESS_DRIVER);
		Properties p = loadSqlQueries();
		if (p != null) {
			String[] necessaryKeys = {
				"PermissionControl.deleteRoleValuesByRoleId", 
				"PermissionControl.getRoleIdsByGroup", 
				"PermissionControl.getRoles", 
				"PermissionControl.getRoleValues"
			};
			boolean shouldUpdate = false;
			if (p.size() == 0) {
				shouldUpdate = true;
			} else {
				for (int i = 0; i < necessaryKeys.length; i++) {
					String key = necessaryKeys[i];
					if (p.getProperty(key) == null) {
						shouldUpdate = true;
						break;
					}
				}
			}
			if (shouldUpdate) {
				String path = this.buildPath("mysql_323.sql");
				FileInputStream fis = new FileInputStream(path);
				try {
					p.load(fis);
					this.saveSqlQueries(p);
				} finally {
					fis.close();
				}
			}
		}
	}

	private void handleMySql40x() throws Exception {
		ensureDaoClassIsCorrect(MYSQL_DATA_ACCESS_DRIVER);
		Properties p = this.loadSqlQueries();
		if (p != null) {
			if (p.size() == 0 || p.getProperty("PermissionControl.deleteAllRoleValues") == null) {
				String path = buildPath("mysql_40.sql");
				FileInputStream fis = new FileInputStream(path);
				try {
					p.load(fis);
					saveSqlQueries(p);
				} finally {
					fis.close();
				}
			}
		}
	}

	private void handleMySql41xPlus() throws Exception {
		System.out.println("--> [MySQLVersionWorkarounder.handleMySql41xPlus] ......");
		ensureDaoClassIsCorrect(MYSQL_DATA_ACCESS_DRIVER);
		// ----------------------------------------------------------------------
		// if without any error or exception, the properties 'p' should be empty
		// ----------------------------------------------------------------------
		Properties p = loadSqlQueries();
		if (p != null && p.size() > 0) {
			saveSqlQueries(new Properties());
		}
		fixEncoding();
	}

	private void fixEncoding() throws Exception {
		System.out.println("--> [MySQLVersionWorkarounder.fixEncoding] ......");
		FileInputStream fis = null;
		OutputStream os = null;
		try {
			Properties p = new Properties();
			File f = new File(SystemGlobals.getValue(ConfigKeys.DATABASE_DRIVER_CONFIG));
			// ---------------------------------------------------------------------------
			// set the value of key - 'mysql.encoding' and key - 'mysql.unicode' 
			// in the file of '~/config/database/mysql/mysql.properties' as empty forever
			// ---------------------------------------------------------------------------
			if (f.canWrite()) {
				fis = new FileInputStream(f);
				p.load(fis);
				p.setProperty(ConfigKeys.DATABASE_MYSQL_ENCODING, "");
				p.setProperty(ConfigKeys.DATABASE_MYSQL_UNICODE, "");
				os = new FileOutputStream(f);
				p.store(os, null);
			}
		} finally {
			if (fis != null) {
				fis.close();
			}
			if (os != null) {
				os.close();
			}
		}
	}

	private void ensureDaoClassIsCorrect(String shouldBe) throws Exception {
		System.out.println("--> [MySQLVersionWorkarounder.ensureDaoClassIsCorrect] ......");
		System.out.println("DEBUG: shouldBe = " + shouldBe);
		System.out.println("DEBUG: ConfigKeys.DAO_DRIVER = " + SystemGlobals.getValue(ConfigKeys.DAO_DRIVER));
		// ------------------------------------------------------------------------
		// if without any error or exception, the below logic will not be executed
		// ------------------------------------------------------------------------
		if (!shouldBe.equals(SystemGlobals.getValue(ConfigKeys.DAO_DRIVER))) {
			logger.info("MySQL DAO class is incorrect. Setting it to " + shouldBe);
			fixDAODriver(shouldBe);
			SystemGlobals.setValue(ConfigKeys.DAO_DRIVER, shouldBe);
			ConfigLoader.loadDaoImplementation();
		}
	}

	private Properties loadSqlQueries() throws Exception {
		System.out.println("--> [MySQLVersionWorkarounder.loadSqlQueries] ......");
		String sqlQueries = SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_DRIVER);
		System.out.println("DEBUG: ConfigKeys.SQL_QUERIES_DRIVER - sqlQueries = " + sqlQueries);
		File f = new File(sqlQueries);
		Properties p = new Properties();
		FileInputStream fis = new FileInputStream(f);
		try {
			p.load(fis);
			if (f.canWrite()) {
				return p;
			}
		} finally {
			try {
				fis.close();
			} catch (Exception e) {
				
			}
		}
		logger.warn("Cannot overwrite " + sqlQueries + " file. Insuficient privileges");
		return null;
	}

	private void saveSqlQueries(Properties p) throws Exception {
		System.out.println("--> [MySQLVersionWorkarounder.saveSqlQueries] ......");
		FileOutputStream fos = new FileOutputStream(SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_DRIVER));
		try {
			p.store(fos, null);
		} finally {
			fos.close();
		}
		SystemGlobals.loadQueries(SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_DRIVER));
	}

	private void fixDAODriver(String daoClassName) throws Exception {
		System.out.println("--> [MySQLVersionWorkarounder.fixDAODriver] ......");
		String driverConfigPath = SystemGlobals.getValue(ConfigKeys.DATABASE_DRIVER_CONFIG);
		File f = new File(driverConfigPath);
		if (f.canWrite()) {
			// Fix the DAO class
			Properties p = new Properties();
			FileInputStream fis = new FileInputStream(driverConfigPath);
			FileOutputStream fos = null;
			try {
				p.load(fis);
				p.setProperty(ConfigKeys.DAO_DRIVER, daoClassName);
				fos = new FileOutputStream(driverConfigPath);
				p.store(fos, null);
			} finally {
				if (fos != null) {
					fos.close();
				}
				fis.close();
			}
		} else {
			logger.warn("Cannot overwrite" + driverConfigPath + ". Insuficient privileges");
		}
	}

	private String buildPath(String concat) {
		return new StringBuffer(256).append(SystemGlobals.getValue(ConfigKeys.CONFIG_DIR)).append('/').append("database/mysql/").append(concat).toString();
	}
}
