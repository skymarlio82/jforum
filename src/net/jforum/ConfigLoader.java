
package net.jforum;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import net.jforum.api.integration.mail.pop.POPJobStarter;
import net.jforum.cache.CacheEngine;
import net.jforum.cache.Cacheable;
import net.jforum.dao.DataAccessDriver;
import net.jforum.exceptions.CacheEngineStartupException;
import net.jforum.exceptions.ForumException;
import net.jforum.search.SearchFacade;
import net.jforum.sso.LoginAuthenticator;
import net.jforum.summary.SummaryScheduler;
import net.jforum.util.FileMonitor;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.QueriesFileListener;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.SystemGlobalsListener;

import org.apache.log4j.Logger;
import org.quartz.SchedulerException;

public class ConfigLoader {

	private final static Logger logger = Logger.getLogger(ConfigLoader.class);

	private static CacheEngine cache = null;

	public static void startSystemglobals(String appPath) {
		System.out.println("--> [ConfigLoader.startSystemglobals] ......");
		SystemGlobals.initGlobals(appPath, appPath + "/WEB-INF/config/SystemGlobals.properties");
//		SystemGlobals.loadAdditionalDefaults(SystemGlobals.getValue(ConfigKeys.DATABASE_DRIVER_CONFIG));
		// =================================================================================================================
		// modified by jitao, 201312061336, it has been duplicate with the one in the method of 'SystemGlobals.initGlobals'
		// =================================================================================================================
//		System.out.println("DEBUG: installationConfig = " + SystemGlobals.getValue(ConfigKeys.INSTALLATION_CONFIG) + " from 'getValue'");
//		if (new File(SystemGlobals.getValue(ConfigKeys.INSTALLATION_CONFIG)).exists()) {
//			SystemGlobals.loadAdditionalDefaults(SystemGlobals.getValue(ConfigKeys.INSTALLATION_CONFIG));
//		}
	}

	public static Properties loadModulesMapping(String baseConfigDir) {
		System.out.println("--> [ConfigLoader.loadModulesMapping] ......");
		FileInputStream fis = null;
		try {
			Properties modulesMapping = new Properties();
			fis = new FileInputStream(baseConfigDir + "/modulesMapping.properties");
			modulesMapping.load(fis);
			return modulesMapping;
		} catch (IOException e) {
			throw new ForumException( e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
					
				}
			}
		}
    }

	public static void createLoginAuthenticator() {
		System.out.println("--> [ConfigLoader.createLoginAuthenticator] ......");
		String className = SystemGlobals.getValue(ConfigKeys.LOGIN_AUTHENTICATOR);
		System.out.println("DEBUG: ConfigKeys.LOGIN_AUTHENTICATOR = " + className);
		try {
			LoginAuthenticator loginAuthenticator = (LoginAuthenticator)Class.forName(className).newInstance();
			SystemGlobals.setObjectValue(ConfigKeys.LOGIN_AUTHENTICATOR_INSTANCE, loginAuthenticator);
			System.out.println("INFOR: the 'loginAuthenticator' already be set in the key 'ConfigKeys.LOGIN_AUTHENTICATOR_INSTANCE' of 'SystemGlobals.globals.objectProperties'");
		} catch (Exception e) {
			throw new ForumException("Error while trying to create a login.authenticator instance (" + className + "): " + e, e);
		}
	}

	@SuppressWarnings("rawtypes")
	public static void loadUrlPatterns() {
		System.out.println("--> [ConfigLoader.loadUrlPatterns] ......");
		FileInputStream fis = null;
		try {
			Properties p = new Properties();
			fis = new FileInputStream(SystemGlobals.getValue(ConfigKeys.CONFIG_DIR) + "/urlPattern.properties");
			p.load(fis);
			for (Iterator iter = p.entrySet().iterator(); iter.hasNext(); ) {
				Map.Entry entry = (Map.Entry)iter.next();
				UrlPatternCollection.addPattern((String)entry.getKey(), (String)entry.getValue());
			}
			System.out.println("INFOR: already load the url Patterns into 'UrlPatternCollection' from 'urlPattern.properties' ...");
		} catch (IOException e) {
			throw new ForumException(e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
					
				}
			}
		}
    }

	public static void listenForChanges() {
		System.out.println("--> [ConfigLoader.listenForChanges] ......");
		int fileChangesDelay = SystemGlobals.getIntValue(ConfigKeys.FILECHANGES_DELAY);
		if (fileChangesDelay > 0) {
			// Queries
			FileMonitor.getInstance().addFileChangeListener(new QueriesFileListener(), SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_GENERIC), fileChangesDelay);
			FileMonitor.getInstance().addFileChangeListener(new QueriesFileListener(), SystemGlobals.getValue(ConfigKeys.SQL_QUERIES_DRIVER), fileChangesDelay);
			// System Properties
			FileMonitor.getInstance().addFileChangeListener(new SystemGlobalsListener(), SystemGlobals.getValue(ConfigKeys.DEFAULT_CONFIG), fileChangesDelay);
			ConfigLoader.listenInstallationConfig();
        }
	}

	public static void listenInstallationConfig() {
		System.out.println("--> [ConfigLoader.listenInstallationConfig] ......");
		int fileChangesDelay = SystemGlobals.getIntValue(ConfigKeys.FILECHANGES_DELAY);
		if (fileChangesDelay > 0) {
			if (new File(SystemGlobals.getValue(ConfigKeys.INSTALLATION_CONFIG)).exists()) {
				FileMonitor.getInstance().addFileChangeListener(new SystemGlobalsListener(), SystemGlobals.getValue(ConfigKeys.INSTALLATION_CONFIG), fileChangesDelay);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static void loadDaoImplementation() {
		System.out.println("--> [ConfigLoader.loadDaoImplementation] ......");
		// Start the dao.driver implementation
		String driver = SystemGlobals.getValue(ConfigKeys.DAO_DRIVER);
		logger.info("Loading JDBC driver " + driver);
		try {
			Class c = Class.forName(driver);
			DataAccessDriver d = (DataAccessDriver)c.newInstance();
			DataAccessDriver.init(d);
			System.out.println("INFOR: JDBC Driver is instantiated ...");
		} catch (Exception e) {
			throw new ForumException(e);
		}
    }

	public static void startCacheEngine() {
		System.out.println("--> [ConfigLoader.startCacheEngine] ......");
		try {
			String cacheImplementation = SystemGlobals.getValue(ConfigKeys.CACHE_IMPLEMENTATION);
			logger.info("Using cache engine: " + cacheImplementation);
			// *-> Design Pattern : Java reflection
			cache = (CacheEngine)Class.forName(cacheImplementation).newInstance();
			cache.init();
			String s = SystemGlobals.getValue(ConfigKeys.CACHEABLE_OBJECTS);
			if (s == null || s.trim().equals("")) {
				logger.warn("Cannot find Cacheable objects to associate the cache engine instance.");
				return;
			}
			String[] cacheableObjects = s.split(",");
			for (int i = 0; i < cacheableObjects.length; i++) {
				logger.info("Creating an instance of " + cacheableObjects[i]);
				Object o = Class.forName(cacheableObjects[i].trim()).newInstance();
				if (o instanceof Cacheable) {
					((Cacheable)o).setCacheEngine(cache);
				} else {
					logger.error(cacheableObjects[i] + " is not an instance of net.jforum.cache.Cacheable");
				}
			}
		} catch (Exception e) {
			throw new CacheEngineStartupException("Error while starting the cache engine", e);
		}
	}

	public static void stopCacheEngine() {
		if (cache != null) {
			cache.stop();
		}
	}

	public static void startSearchIndexer() {
		System.out.println("--> [ConfigLoader.startSearchIndexer] ......");
		SearchFacade.init();
	}

	public static void startSummaryJob() throws SchedulerException {
		System.out.println("--> [ConfigLoader.startSummaryJob] ......");
		SummaryScheduler.startJob();
	}

	public static void startPop3Integration() throws SchedulerException {
		POPJobStarter.startJob();
	}
}
